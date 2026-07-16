package com.limeday.app.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.limeday.app.data.AppMetadata
import com.limeday.app.data.DailyReview
import com.limeday.app.data.DailySummary
import com.limeday.app.data.LimeDayRepository
import com.limeday.app.data.TodoItem
import com.limeday.app.llm.LlmClient
import com.limeday.app.llm.LlmConfig
import com.limeday.app.llm.SecureLlmConfigStore
import com.limeday.app.sync.SecureWebDavConfigStore
import com.limeday.app.sync.WebDavClient
import com.limeday.app.sync.WebDavConfig
import com.limeday.app.sync.WebDavSyncCoordinator
import com.limeday.app.sync.WebDavSyncWorker
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DayUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val todos: List<TodoItem> = emptyList(),
    val review: DailyReview? = null,
    val summary: DailySummary? = null,
    val llmConfig: LlmConfig = LlmConfig(),
    val isGeneratingSummary: Boolean = false,
    val summaryError: String? = null,
    val webDavConfig: WebDavConfig = WebDavConfig(),
    val isTestingWebDav: Boolean = false,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val metadata: AppMetadata? = null,
    val isLoading: Boolean = true
) {
    val completedCount: Int get() = todos.count { it.isCompleted }
    val progress: Float get() = if (todos.isEmpty()) 0f else completedCount.toFloat() / todos.size
    val progressPercent: Int get() = (progress * 100).toInt()
    val hasReview: Boolean get() = review?.hasContent() == true
}

@OptIn(ExperimentalCoroutinesApi::class)
class DayViewModel(
    private val repository: LimeDayRepository,
    private val llmConfigStore: SecureLlmConfigStore,
    private val llmClient: LlmClient,
    private val webDavConfigStore: SecureWebDavConfigStore,
    private val webDavClient: WebDavClient,
    private val syncCoordinator: WebDavSyncCoordinator,
    private val application: Application
) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val reviewDraft = MutableStateFlow<DailyReview?>(null)
    private val llmConfig = MutableStateFlow(llmConfigStore.load())
    private val webDavConfig = MutableStateFlow(webDavConfigStore.load())
    private val isGeneratingSummary = MutableStateFlow(false)
    private val summaryError = MutableStateFlow<String?>(null)
    private val isTestingWebDav = MutableStateFlow(false)
    private val isSyncing = MutableStateFlow(false)
    private val syncMessage = MutableStateFlow<String?>(null)
    private val metadata = MutableStateFlow<AppMetadata?>(null)
    private var reviewSaveJob: Job? = null
    private var summaryJob: Job? = null
    private var reviewDirty = false

    private val todos = selectedDate.flatMapLatest { repository.observeTodos(it.toString()) }
    private val storedReview = selectedDate.flatMapLatest { repository.observeReview(it.toString()) }
    private val storedSummary = selectedDate.flatMapLatest { repository.observeSummary(it.toString()) }

    private val dayContent = combine(selectedDate, todos, storedReview, reviewDraft) { date, items, stored, draft ->
        DayUiState(
            selectedDate = date,
            todos = items,
            review = draft?.takeIf { it.date == date.toString() } ?: stored,
            isLoading = false
        )
    }

    private val serviceState = combine(
        llmConfig,
        isGeneratingSummary,
        summaryError,
        webDavConfig,
        isTestingWebDav
    ) { llm, generating, error, webDav, testing ->
        ServiceState(llm, generating, error, webDav, testing)
    }

    private val contentState = combine(
        dayContent,
        storedSummary,
        serviceState
    ) { day, summary, services ->
        day.copy(
            summary = summary,
            llmConfig = services.llmConfig,
            isGeneratingSummary = services.isGenerating,
            summaryError = services.summaryError,
            webDavConfig = services.webDavConfig,
            isTestingWebDav = services.isTestingWebDav
        )
    }

    val uiState = combine(
        contentState,
        isSyncing,
        syncMessage,
        metadata
    ) { content, syncing, message, meta ->
        content.copy(
            isSyncing = syncing,
            syncMessage = message,
            metadata = meta
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayUiState())

    init {
        viewModelScope.launch {
            selectedDate.flatMapLatest { date ->
                repository.observeReview(date.toString()).map { date to it }
            }.collect { (date, stored) ->
                if (reviewDraft.value?.date != date.toString() || reviewSaveJob?.isActive != true) {
                    reviewDraft.value = stored ?: repository.newReview(date.toString())
                    reviewDirty = false
                }
            }
        }
        refreshMetadata()
    }

    fun previousDay() = selectDate(selectedDate.value.minusDays(1))
    fun nextDay() = selectDate(selectedDate.value.plusDays(1))
    fun today() = selectDate(LocalDate.now())

    private fun selectDate(date: LocalDate) {
        flushReview()
        selectedDate.value = date
        reviewDraft.value = null
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
        val current = reviewDraft.value ?: return
        val transformed = transform(current)
        reviewDraft.value = transformed.copy(
            highlight = transformed.highlight.take(1000),
            challenge = transformed.challenge.take(1000),
            learning = transformed.learning.take(1000),
            tomorrowFocus = transformed.tomorrowFocus.take(1000),
            mood = transformed.mood.coerceIn(0, 5)
        )
        reviewDirty = true
        reviewSaveJob?.cancel()
        reviewSaveJob = viewModelScope.launch {
            delay(500)
            reviewDraft.value?.let { repository.saveReview(it) }
            reviewDirty = false
        }
    }

    fun flushReview() {
        if (!reviewDirty) return
        reviewSaveJob?.cancel()
        val draft = reviewDraft.value ?: return
        viewModelScope.launch {
            repository.saveReview(draft)
            reviewDirty = false
        }
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
        llmConfigStore.save(normalized)
        llmConfig.value = normalized
        summaryError.value = null
    }

    fun clearLlmConfig() {
        llmConfigStore.clear()
        llmConfig.value = LlmConfig()
    }

    fun clearSummaryError() {
        summaryError.value = null
    }

    fun generateSummary() {
        val state = uiState.value
        val review = state.review
        if (!state.llmConfig.isConfigured) {
            summaryError.value = "请先配置大语言模型接口"
            return
        }
        if (state.todos.isEmpty() && review?.hasContent() != true) {
            summaryError.value = "请先记录待办或复盘内容"
            return
        }
        if (isGeneratingSummary.value) return
        flushReview()
        summaryJob = viewModelScope.launch {
            isGeneratingSummary.value = true
            summaryError.value = null
            runCatching { llmClient.summarize(state.llmConfig, buildSummaryPrompt(state)) }
                .onSuccess { content ->
                    repository.saveSummary(
                        date = state.selectedDate.toString(),
                        content = content,
                        provider = state.llmConfig.provider.displayName,
                        model = state.llmConfig.model,
                        current = state.summary
                    )
                }
                .onFailure { error ->
                    if (error !is kotlinx.coroutines.CancellationException) {
                        summaryError.value = error.message ?: "总结生成失败，请稍后重试"
                    }
                }
            isGeneratingSummary.value = false
        }
    }

    fun cancelSummary() {
        summaryJob?.cancel()
        isGeneratingSummary.value = false
    }

    fun saveWebDavConfig(config: WebDavConfig) {
        val normalized = config.normalized
        if (!normalized.isConfigured) {
            syncMessage.value = "请填写 HTTPS 地址、用户名和密码"
            return
        }
        webDavConfigStore.save(normalized)
        webDavConfig.value = normalized
        WebDavSyncWorker.schedule(application)
        syncMessage.value = "WebDAV 配置已保存"
    }

    fun clearWebDavConfig() {
        webDavConfigStore.clear()
        webDavConfig.value = WebDavConfig()
        WebDavSyncWorker.cancel(application)
        syncMessage.value = "WebDAV 配置已清除"
    }

    fun testWebDav(config: WebDavConfig) {
        if (isTestingWebDav.value) return
        viewModelScope.launch {
            isTestingWebDav.value = true
            syncMessage.value = null
            runCatching { webDavClient.test(config.normalized) }
                .onSuccess { syncMessage.value = "连接成功" }
                .onFailure { syncMessage.value = it.message ?: "连接失败" }
            isTestingWebDav.value = false
        }
    }

    fun syncNow(config: WebDavConfig = webDavConfig.value) {
        if (isSyncing.value) return
        viewModelScope.launch {
            isSyncing.value = true
            syncMessage.value = null
            runCatching { syncCoordinator.sync(config.normalized) }
                .onSuccess { syncMessage.value = it.message }
                .onFailure { error ->
                    val message = error.message ?: "同步失败"
                    repository.recordSync(message)
                    syncMessage.value = message
                }
            isSyncing.value = false
            refreshMetadata()
        }
    }

    private fun refreshMetadata() {
        viewModelScope.launch { metadata.value = repository.metadata() }
    }

    private fun buildSummaryPrompt(state: DayUiState): String = buildString {
        val review = state.review
        appendLine("日期：${state.selectedDate}")
        appendLine("待办完成：${state.completedCount}/${state.todos.size}")
        appendLine("待办记录：")
        if (state.todos.isEmpty()) appendLine("（无）")
        state.todos.forEach { appendLine("- [${if (it.isCompleted) "已完成" else "未完成"}] ${it.title}") }
        appendLine("复盘记录：")
        appendLine("今日亮点：${review?.highlight?.ifBlank { "（未填写）" } ?: "（未填写）"}")
        appendLine("困难：${review?.challenge?.ifBlank { "（未填写）" } ?: "（未填写）"}")
        appendLine("收获：${review?.learning?.ifBlank { "（未填写）" } ?: "（未填写）"}")
        appendLine("明日重点：${review?.tomorrowFocus?.ifBlank { "（未填写）" } ?: "（未填写）"}")
        appendLine("心情评分：${review?.mood?.takeIf { it > 0 }?.let { "$it/5" } ?: "未选择"}")
    }

    private data class ServiceState(
        val llmConfig: LlmConfig,
        val isGenerating: Boolean,
        val summaryError: String?,
        val webDavConfig: WebDavConfig,
        val isTestingWebDav: Boolean
    )
}

class DayViewModelFactory(
    private val repository: LimeDayRepository,
    private val llmConfigStore: SecureLlmConfigStore,
    private val llmClient: LlmClient,
    private val webDavConfigStore: SecureWebDavConfigStore,
    private val webDavClient: WebDavClient,
    private val syncCoordinator: WebDavSyncCoordinator,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = DayViewModel(
        repository,
        llmConfigStore,
        llmClient,
        webDavConfigStore,
        webDavClient,
        syncCoordinator,
        application
    ) as T
}
