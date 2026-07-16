package com.limeday.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.limeday.app.data.DailyReview
import com.limeday.app.data.LimeDayRepository
import com.limeday.app.data.DailySummary
import com.limeday.app.data.TodoItem
import com.limeday.app.llm.LlmClient
import com.limeday.app.llm.LlmConfig
import com.limeday.app.llm.SecureLlmConfigStore
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

data class DayUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val todos: List<TodoItem> = emptyList(),
    val review: DailyReview = DailyReview(LocalDate.now().toString()),
    val summary: DailySummary? = null,
    val llmConfig: LlmConfig = LlmConfig(),
    val isGeneratingSummary: Boolean = false,
    val summaryError: String? = null,
    val isLoading: Boolean = true
) {
    val completedCount: Int get() = todos.count { it.isCompleted }
    val progress: Float get() = if (todos.isEmpty()) 0f else completedCount.toFloat() / todos.size
    val progressPercent: Int get() = (progress * 100).toInt()
}

@OptIn(ExperimentalCoroutinesApi::class)
class DayViewModel(
    private val repository: LimeDayRepository,
    private val configStore: SecureLlmConfigStore,
    private val llmClient: LlmClient
) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val reviewDraft = MutableStateFlow(DailyReview(selectedDate.value.toString()))
    private val reviewSaveQueue = Channel<DailyReview>(Channel.UNLIMITED)
    private val llmConfig = MutableStateFlow(configStore.load())
    private val isGeneratingSummary = MutableStateFlow(false)
    private val summaryError = MutableStateFlow<String?>(null)

    private val todos = selectedDate.flatMapLatest { repository.observeTodos(it.toString()) }
    private val storedReview = selectedDate.flatMapLatest { repository.observeReview(it.toString()) }
    private val storedSummary = selectedDate.flatMapLatest { repository.observeSummary(it.toString()) }

    private val dayContent = combine(selectedDate, todos, storedReview, reviewDraft) { date, items, stored, draft ->
        val activeReview = if (draft.date == date.toString()) draft
        else stored ?: DailyReview(date.toString())
        DayUiState(date, items, activeReview, isLoading = false)
    }

    val uiState = combine(
        dayContent,
        storedSummary,
        llmConfig,
        isGeneratingSummary,
        summaryError
    ) { day, summary, config, generating, error ->
        day.copy(
            summary = summary,
            llmConfig = config,
            isGeneratingSummary = generating,
            summaryError = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayUiState())

    init {
        viewModelScope.launch {
            selectedDate.flatMapLatest { date ->
                repository.observeReview(date.toString())
                    .map { it ?: DailyReview(date.toString()) }
                    .take(1)
            }.collect { loaded -> reviewDraft.value = loaded }
        }
        viewModelScope.launch {
            for (review in reviewSaveQueue) repository.saveReview(review)
        }
    }

    fun previousDay() = selectDate(selectedDate.value.minusDays(1))
    fun nextDay() = selectDate(selectedDate.value.plusDays(1))
    fun today() = selectDate(LocalDate.now())

    private fun selectDate(date: LocalDate) {
        val pending = reviewDraft.value
        if (pending.date == selectedDate.value.toString()) {
            reviewSaveQueue.trySend(pending)
        }
        selectedDate.value = date
        reviewDraft.value = DailyReview(date.toString())
    }

    fun addTodo(title: String) {
        val clean = title.trim().take(80)
        if (clean.isEmpty()) return
        viewModelScope.launch { repository.addTodo(selectedDate.value.toString(), clean) }
    }

    fun updateTodo(todo: TodoItem, title: String, note: String) {
        val cleanTitle = title.trim().take(80)
        if (cleanTitle.isEmpty()) return
        viewModelScope.launch { repository.updateTodo(todo, cleanTitle, note.take(300)) }
    }

    fun toggleTodo(todo: TodoItem) {
        viewModelScope.launch { repository.setTodoCompleted(todo, !todo.isCompleted) }
    }

    fun deleteTodo(todo: TodoItem) {
        viewModelScope.launch { repository.deleteTodo(todo) }
    }

    fun updateReview(transform: (DailyReview) -> DailyReview) {
        val transformed = transform(reviewDraft.value)
        val updated = transformed.copy(
            highlight = transformed.highlight.take(1000),
            challenge = transformed.challenge.take(1000),
            learning = transformed.learning.take(1000),
            tomorrowFocus = transformed.tomorrowFocus.take(1000)
        )
        reviewDraft.value = updated
        reviewSaveQueue.trySend(updated)
    }

    fun saveLlmConfig(config: LlmConfig) {
        val normalized = config.copy(
            baseUrl = config.baseUrl.trim().trimEnd('/'),
            model = config.model.trim(),
            apiKey = config.apiKey.trim()
        )
        if (!normalized.baseUrl.startsWith("https://")) {
            summaryError.value = "接口地址必须使用 HTTPS"
            return
        }
        if (normalized.model.isBlank() || normalized.apiKey.isBlank()) {
            summaryError.value = "请填写模型名称和 API Key"
            return
        }
        configStore.save(normalized)
        llmConfig.value = normalized
        summaryError.value = null
    }

    fun clearSummaryError() {
        summaryError.value = null
    }

    fun generateSummary() {
        val state = uiState.value
        if (!state.llmConfig.isConfigured) {
            summaryError.value = "请先配置大语言模型接口"
            return
        }
        if (state.todos.isEmpty() && state.review.isEmpty()) {
            summaryError.value = "请先记录待办或复盘内容"
            return
        }
        if (isGeneratingSummary.value) return

        val requestDate = state.selectedDate.toString()
        val requestConfig = state.llmConfig
        val prompt = buildSummaryPrompt(state)
        viewModelScope.launch {
            isGeneratingSummary.value = true
            summaryError.value = null
            runCatching { llmClient.summarize(requestConfig, prompt) }
                .onSuccess { content ->
                    repository.saveSummary(
                        DailySummary(
                            date = requestDate,
                            content = content,
                            provider = requestConfig.provider.displayName,
                            model = requestConfig.model
                        )
                    )
                }
                .onFailure { error ->
                    summaryError.value = error.message ?: "总结生成失败，请稍后重试"
                }
            isGeneratingSummary.value = false
        }
    }

    private fun DailyReview.isEmpty(): Boolean =
        highlight.isBlank() && challenge.isBlank() && learning.isBlank() &&
            tomorrowFocus.isBlank() && mood == 0

    private fun buildSummaryPrompt(state: DayUiState): String = buildString {
        appendLine("日期：${state.selectedDate}")
        appendLine("待办完成：${state.completedCount}/${state.todos.size}")
        appendLine("待办记录：")
        if (state.todos.isEmpty()) appendLine("（无）")
        state.todos.forEach { appendLine("- [${if (it.isCompleted) "已完成" else "未完成"}] ${it.title}") }
        appendLine("复盘记录：")
        appendLine("今日亮点：${state.review.highlight.ifBlank { "（未填写）" }}")
        appendLine("困难：${state.review.challenge.ifBlank { "（未填写）" }}")
        appendLine("收获：${state.review.learning.ifBlank { "（未填写）" }}")
        appendLine("明日重点：${state.review.tomorrowFocus.ifBlank { "（未填写）" }}")
        appendLine("心情评分：${if (state.review.mood == 0) "未选择" else "${state.review.mood}/5"}")
    }
}

class DayViewModelFactory(
    private val repository: LimeDayRepository,
    private val configStore: SecureLlmConfigStore,
    private val llmClient: LlmClient
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        DayViewModel(repository, configStore, llmClient) as T
}
