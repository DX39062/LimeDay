package com.limeday.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.limeday.app.data.AppMetadata
import com.limeday.app.data.DailyReview
import com.limeday.app.data.DailySummary
import com.limeday.app.data.LimeDayRepository
import com.limeday.app.data.RangeSourceData
import com.limeday.app.data.RangeSummary
import com.limeday.app.data.TodoItem
import com.limeday.app.data.TodoEdit
import com.limeday.app.data.TodoGroup
import com.limeday.app.data.TodoStep
import com.limeday.app.llm.LlmClient
import com.limeday.app.llm.LlmModelCache
import com.limeday.app.llm.LlmServiceConfig
import com.limeday.app.llm.LlmSettings
import com.limeday.app.llm.SecureLlmConfigStore
import com.limeday.app.settings.AppSettings
import com.limeday.app.settings.AppSettingsStore
import com.limeday.app.settings.DailyReminderWorker
import com.limeday.app.settings.ThemeMode
import com.limeday.app.settings.TodoReminderWorker
import com.limeday.app.sync.SecureWebDavConfigStore
import com.limeday.app.sync.WebDavClient
import com.limeday.app.sync.WebDavConfig
import com.limeday.app.sync.WebDavSyncCoordinator
import com.limeday.app.sync.WebDavSyncWorker
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class DayUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val todos: List<TodoItem> = emptyList(),
    val displayedTodos: List<TodoItem> = emptyList(),
    val todoGroups: List<TodoGroup> = emptyList(),
    val todoSteps: List<TodoStep> = emptyList(),
    val todoViewMode: TodoViewMode = TodoViewMode.DAY,
    val todoSearchQuery: String = "",
    val deletedTodos: List<TodoItem> = emptyList(),
    val review: DailyReview? = null,
    val summary: DailySummary? = null,
    val rangeSummaries: List<RangeSummary> = emptyList(),
    val llmSettings: LlmSettings = LlmSettings(),
    val fetchedModels: List<String> = emptyList(),
    val isFetchingModels: Boolean = false,
    val llmProviderMessage: String? = null,
    val isGeneratingSummary: Boolean = false,
    val summaryError: String? = null,
    val isGeneratingRangeSummary: Boolean = false,
    val rangeSummaryError: String? = null,
    val webDavConfig: WebDavConfig = WebDavConfig(),
    val isTestingWebDav: Boolean = false,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val metadata: AppMetadata? = null,
    val appSettings: AppSettings = AppSettings(),
    val dataMessage: String? = null,
    val monthTodoStatuses: Map<LocalDate, MonthTodoStatus> = emptyMap(),
    val isLoading: Boolean = true
) {
    val completedCount: Int get() = todos.count { it.isCompleted }
    val progress: Float get() = if (todos.isEmpty()) 0f else completedCount.toFloat() / todos.size
    val progressPercent: Int get() = (progress * 100).toInt()
    val hasReview: Boolean get() = review?.hasContent() == true
    val activeLlmProvider: LlmServiceConfig? get() = llmSettings.activeProvider
}

enum class TodoViewMode(val label: String) {
    DAY("当天"),
    OVERDUE("逾期"),
    PLANNED("计划中")
}

data class MonthTodoStatus(val total: Int, val completed: Int) {
    val allCompleted: Boolean get() = total > 0 && completed == total
}

internal fun shouldEnableLlmForUpgrade(hasExplicitSetting: Boolean, savedProviderCount: Int): Boolean =
    !hasExplicitSetting && savedProviderCount > 0

@OptIn(ExperimentalCoroutinesApi::class)
class DayViewModel(
    private val repository: LimeDayRepository,
    private val llmConfigStore: SecureLlmConfigStore,
    private val llmClient: LlmClient,
    private val webDavConfigStore: SecureWebDavConfigStore,
    private val webDavClient: WebDavClient,
    private val syncCoordinator: WebDavSyncCoordinator,
    private val appSettingsStore: AppSettingsStore,
    private val application: Application
) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val reviewDraft = MutableStateFlow<DailyReview?>(null)
    private val llmSettings = MutableStateFlow(llmConfigStore.load())
    private val fetchedModels = MutableStateFlow<List<String>>(emptyList())
    private val isFetchingModels = MutableStateFlow(false)
    private val llmProviderMessage = MutableStateFlow<String?>(null)
    private val webDavConfig = MutableStateFlow(webDavConfigStore.load())
    private val isGeneratingSummary = MutableStateFlow(false)
    private val summaryError = MutableStateFlow<String?>(null)
    private val isGeneratingRangeSummary = MutableStateFlow(false)
    private val rangeSummaryError = MutableStateFlow<String?>(null)
    private val isTestingWebDav = MutableStateFlow(false)
    private val isSyncing = MutableStateFlow(false)
    private val syncMessage = MutableStateFlow<String?>(null)
    private val metadata = MutableStateFlow<AppMetadata?>(null)
    private val dataMessage = MutableStateFlow<String?>(null)
    private val monthTodoStatuses = MutableStateFlow<Map<LocalDate, MonthTodoStatus>>(emptyMap())
    private val todoViewMode = MutableStateFlow(TodoViewMode.DAY)
    private val todoSearchQuery = MutableStateFlow("")
    private var reviewSaveJob: Job? = null
    private var summaryJob: Job? = null
    private var rangeSummaryJob: Job? = null
    private var modelFetchJob: Job? = null
    private var reviewDirty = false

    init {
        viewModelScope.launch { repository.ensureDefaultGroupName() }
        if (shouldEnableLlmForUpgrade(appSettingsStore.hasExplicitLlmSetting, llmSettings.value.providers.size)) {
            appSettingsStore.setLlmEnabled(true)
        }
    }

    private val todos = selectedDate.flatMapLatest { repository.observeTodos(it.toString()) }
    private val storedReview = selectedDate.flatMapLatest { repository.observeReview(it.toString()) }
    private val storedSummary = selectedDate.flatMapLatest { repository.observeSummary(it.toString()) }
    private val deletedTodos = repository.observeDeletedTodos()
    private val rangeSummaries = repository.observeRangeSummaries()
    private val groups = repository.observeGroups()
    private val steps = repository.observeSteps()

    private val displayedTodos = combine(todoViewMode, todoSearchQuery, selectedDate) { mode, query, date ->
        Triple(mode, query.trim(), date)
    }.flatMapLatest { (mode, query, date) ->
        when {
            query.isNotBlank() -> repository.searchTodos(query)
            mode == TodoViewMode.DAY -> repository.observeTodos(date.toString())
            mode == TodoViewMode.OVERDUE -> repository.overdueTodos()
            mode == TodoViewMode.PLANNED -> repository.plannedTodos()
            else -> flowOf(emptyList())
        }
    }

    private val todoPresentation = combine(groups, steps, displayedTodos, todoViewMode, todoSearchQuery) { groupList, stepList, visible, mode, query ->
        TodoPresentation(groupList, stepList, visible, mode, query)
    }

    private val dayContent = combine(selectedDate, todos, storedReview, reviewDraft, todoPresentation) { date, items, stored, draft, presentation ->
        DayUiState(
            selectedDate = date,
            todos = items,
            displayedTodos = presentation.todos,
            todoGroups = presentation.groups,
            todoSteps = presentation.steps,
            todoViewMode = presentation.mode,
            todoSearchQuery = presentation.query,
            review = draft?.takeIf { it.date == date.toString() } ?: stored,
            isLoading = false
        )
    }

    private val contentState = combine(dayContent, storedSummary, deletedTodos, rangeSummaries) { day, summary, deleted, ranges ->
        day.copy(summary = summary, deletedTodos = deleted, rangeSummaries = ranges)
    }

    private val llmGenerationState = combine(
        llmSettings,
        isGeneratingSummary,
        summaryError,
        isGeneratingRangeSummary,
        rangeSummaryError
    ) { settings, dailyGenerating, dailyError, rangeGenerating, rangeError ->
        LlmGenerationState(settings, dailyGenerating, dailyError, rangeGenerating, rangeError)
    }

    private val llmToolState = combine(fetchedModels, isFetchingModels, llmProviderMessage) { models, fetching, message ->
        LlmToolState(models, fetching, message)
    }

    private val contentWithLlm = combine(contentState, llmGenerationState, llmToolState) { content, generation, tools ->
        content.copy(
            llmSettings = generation.settings,
            isGeneratingSummary = generation.isGeneratingDaily,
            summaryError = generation.dailyError,
            isGeneratingRangeSummary = generation.isGeneratingRange,
            rangeSummaryError = generation.rangeError,
            fetchedModels = tools.models,
            isFetchingModels = tools.isFetching,
            llmProviderMessage = tools.message
        )
    }

    private val syncState = combine(webDavConfig, isTestingWebDav, isSyncing, syncMessage, metadata) { config, testing, syncing, message, meta ->
        SyncUiState(config, testing, syncing, message, meta)
    }

    private val localSettingsState = combine(appSettingsStore.settings, dataMessage) { settings, message ->
        LocalSettingsState(settings, message)
    }

    val uiState = combine(contentWithLlm, syncState, localSettingsState, monthTodoStatuses) { content, sync, local, monthStatuses ->
        content.copy(
            webDavConfig = sync.config,
            isTestingWebDav = sync.isTesting,
            isSyncing = sync.isSyncing,
            syncMessage = sync.message,
            metadata = sync.metadata,
            appSettings = local.settings,
            dataMessage = local.dataMessage,
            monthTodoStatuses = monthStatuses
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayUiState())

    init {
        viewModelScope.launch {
            selectedDate.flatMapLatest { date -> repository.observeReview(date.toString()).map { date to it } }
                .collect { (date, stored) ->
                    if (reviewDraft.value?.date != date.toString() || reviewSaveJob?.isActive != true) {
                        reviewDraft.value = stored ?: repository.newReview(date.toString())
                        reviewDirty = false
                    }
                }
        }
        refreshMetadata()
        rescheduleTodoReminders()
    }

    fun previousDay() = selectDate(selectedDate.value.minusDays(1))
    fun nextDay() = selectDate(selectedDate.value.plusDays(1))
    fun today() = selectDate(LocalDate.now())

    fun selectDate(date: LocalDate) {
        flushReview()
        selectedDate.value = date
        reviewDraft.value = null
    }

    fun setTodoViewMode(mode: TodoViewMode) {
        todoViewMode.value = mode
        if (mode != TodoViewMode.DAY) todoSearchQuery.value = ""
    }

    fun setTodoSearchQuery(query: String) {
        todoSearchQuery.value = query.take(80)
    }

    fun loadMonthTodoStatuses(date: LocalDate) {
        val month = YearMonth.from(date)
        viewModelScope.launch {
            val statuses = repository.todosBetween(month.atDay(1).toString(), month.atEndOfMonth().toString())
                .groupBy { LocalDate.parse(it.date) }
                .mapValues { (_, items) -> MonthTodoStatus(items.size, items.count(TodoItem::isCompleted)) }
            monthTodoStatuses.value = statuses
        }
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

    fun toggleTodo(todo: TodoItem) = viewModelScope.launch {
        val next = repository.setTodoCompleted(todo, !todo.isCompleted)
        repository.todoById(todo.id)?.let { TodoReminderWorker.schedule(application, it) }
        next?.let { TodoReminderWorker.schedule(application, it) }
    }
    fun setTodoPriority(todo: TodoItem, priority: Int) = viewModelScope.launch { repository.setTodoPriority(todo, priority) }
    fun moveTodo(todo: TodoItem, date: LocalDate) = viewModelScope.launch { repository.moveTodo(todo, date.toString()) }
    fun duplicateTodo(todo: TodoItem) = viewModelScope.launch {
        TodoReminderWorker.schedule(application, repository.duplicateTodo(todo))
    }
    fun deleteTodo(todo: TodoItem) = viewModelScope.launch {
        repository.deleteTodo(todo)
        TodoReminderWorker.cancel(application, todo.id)
    }
    fun restoreTodo(todo: TodoItem) = viewModelScope.launch {
        repository.restoreTodo(todo)
        repository.todoById(todo.id)?.let { TodoReminderWorker.schedule(application, it) }
    }

    fun updateTodo(todo: TodoItem, edit: TodoEdit) = viewModelScope.launch {
        TodoReminderWorker.schedule(application, repository.updateTodo(todo, edit))
    }

    fun addTodoStep(todoId: String, title: String) = viewModelScope.launch { repository.addStep(todoId, title) }
    fun toggleTodoStep(step: TodoStep) = viewModelScope.launch { repository.updateStep(step, completed = !step.isCompleted) }
    fun updateTodoStep(step: TodoStep, title: String) = viewModelScope.launch { repository.updateStep(step, title = title) }
    fun moveTodoStep(step: TodoStep, offset: Int) = viewModelScope.launch { repository.moveStep(step, offset) }
    fun deleteTodoStep(step: TodoStep) = viewModelScope.launch { repository.deleteStep(step) }
    fun addTodoGroup(name: String, iconKey: String, colorKey: String) = viewModelScope.launch {
        repository.addGroup(name, iconKey, colorKey)
    }
    fun updateTodoGroup(group: TodoGroup, name: String, iconKey: String, colorKey: String) = viewModelScope.launch {
        repository.updateGroup(group, name, iconKey, colorKey)
    }
    fun deleteTodoGroup(group: TodoGroup) = viewModelScope.launch { repository.deleteGroup(group) }
    fun moveTodoGroup(group: TodoGroup, offset: Int) = viewModelScope.launch { repository.moveGroup(group, offset) }
    fun permanentlyDeleteTodos(todos: Collection<TodoItem>) = viewModelScope.launch {
        todos.forEach { TodoReminderWorker.cancel(application, it.id) }
        repository.permanentlyDeleteTodos(todos)
    }

    fun setThemeMode(mode: ThemeMode) = appSettingsStore.setThemeMode(mode)
    fun setLlmEnabled(enabled: Boolean) = appSettingsStore.setLlmEnabled(enabled)

    fun setTodoReminder(enabled: Boolean, hour: Int, minute: Int) {
        appSettingsStore.setTodoReminder(enabled, hour, minute)
        DailyReminderWorker.scheduleAll(application, appSettingsStore.settings.value)
    }

    fun setReviewReminder(enabled: Boolean, hour: Int, minute: Int) {
        appSettingsStore.setReviewReminder(enabled, hour, minute)
        DailyReminderWorker.scheduleAll(application, appSettingsStore.settings.value)
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            dataMessage.value = null
            runCatching {
                val value = repository.exportJson()
                withContext(Dispatchers.IO) {
                    application.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(value) }
                        ?: error("无法写入所选文件")
                }
            }.onSuccess {
                dataMessage.value = "数据已导出，备份不包含密码、端点和 API Key"
            }.onFailure {
                dataMessage.value = it.message ?: "数据导出失败"
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            dataMessage.value = null
            runCatching {
                val value = withContext(Dispatchers.IO) {
                    val bytes = application.contentResolver.openInputStream(uri)?.use { input ->
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            require(output.size() + count <= MAX_BACKUP_BYTES) { "备份文件过大" }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    } ?: error("无法读取所选文件")
                    bytes.toString(Charsets.UTF_8)
                }
                repository.importJson(value)
                rescheduleTodoRemindersNow()
            }.onSuccess {
                dataMessage.value = "数据导入完成，已按更新时间合并"
            }.onFailure {
                dataMessage.value = it.message ?: "数据导入失败"
            }
        }
    }

    fun clearDataMessage() {
        dataMessage.value = null
    }

    fun updateReview(transform: (DailyReview) -> DailyReview) {
        val current = reviewDraft.value ?: return
        val transformed = transform(current)
        reviewDraft.value = transformed.copy(
            highlight = transformed.highlight.take(3200),
            challenge = transformed.challenge.take(1000),
            learning = transformed.learning.take(1000),
            tomorrowFocus = transformed.tomorrowFocus.take(1000),
            mood = transformed.mood.coerceIn(0, 5)
        )
        reviewDirty = true
        reviewSaveJob?.cancel()
        reviewSaveJob = viewModelScope.launch {
            delay(500)
            persistReviewDraft()
        }
    }

    fun flushReview() {
        if (!reviewDirty) return
        reviewSaveJob?.cancel()
        viewModelScope.launch { persistReviewDraft() }
    }

    private suspend fun persistReviewDraft() {
        if (!reviewDirty) return
        reviewDraft.value?.let { repository.saveReview(it) }
        reviewDirty = false
    }

    fun saveLlmProvider(config: LlmServiceConfig): Boolean {
        val normalized = config.normalized.copy(updatedAt = System.currentTimeMillis())
        val error = when {
            normalized.name.isBlank() -> "请填写模型服务名称"
            !normalized.endpointAllowed -> "接口地址必须使用 HTTPS；本地 HTTP 需单独开启"
            normalized.baseUrl.toHttpUrlOrNull() == null -> "Base URL 格式无效"
            normalized.modelsUrl.isNotBlank() && normalized.modelsUrl.toHttpUrlOrNull() == null -> "模型列表地址格式无效"
            normalized.model.isBlank() -> "请填写模型名称"
            normalized.apiKey.isBlank() && normalized.presetId != "ollama" -> "请填写 API Key"
            else -> null
        }
        if (error != null) {
            llmProviderMessage.value = error
            return false
        }
        val current = llmSettings.value
        val index = current.providers.indexOfFirst { it.id == normalized.id }
        val providers = current.providers.toMutableList().apply {
            if (index >= 0) set(index, normalized) else add(normalized)
        }
        val caches = if (fetchedModels.value.isNotEmpty()) {
            current.modelCaches.filterNot { it.providerId == normalized.id } +
                LlmModelCache(normalized.id, fetchedModels.value, System.currentTimeMillis())
        } else {
            current.modelCaches
        }
        updateLlmSettings(current.copy(
            providers = providers,
            activeProviderId = current.activeProviderId ?: normalized.id,
            modelCaches = caches
        ))
        llmProviderMessage.value = "模型服务已保存"
        return true
    }

    fun activateLlmProvider(providerId: String) {
        if (llmSettings.value.providers.none { it.id == providerId }) return
        updateLlmSettings(llmSettings.value.copy(activeProviderId = providerId))
        llmProviderMessage.value = "默认模型服务已切换"
    }

    fun duplicateLlmProvider(provider: LlmServiceConfig) {
        val now = System.currentTimeMillis()
        val copy = provider.copy(
            id = UUID.randomUUID().toString(),
            name = "${provider.name} 副本",
            createdAt = now,
            updatedAt = now
        )
        val providers = llmSettings.value.providers.toMutableList()
        val index = providers.indexOfFirst { it.id == provider.id }
        providers.add(if (index >= 0) index + 1 else providers.size, copy)
        updateLlmSettings(llmSettings.value.copy(providers = providers))
        llmProviderMessage.value = "已创建模型服务副本"
    }

    fun deleteLlmProvider(provider: LlmServiceConfig) {
        val providers = llmSettings.value.providers.filterNot { it.id == provider.id }
        val active = llmSettings.value.activeProviderId.takeUnless { it == provider.id } ?: providers.firstOrNull()?.id
        updateLlmSettings(llmSettings.value.copy(
            providers = providers,
            activeProviderId = active,
            modelCaches = llmSettings.value.modelCaches.filterNot { it.providerId == provider.id }
        ))
        llmProviderMessage.value = "模型服务已删除"
    }

    fun moveLlmProvider(providerId: String, offset: Int) {
        val providers = llmSettings.value.providers.toMutableList()
        val from = providers.indexOfFirst { it.id == providerId }
        val to = (from + offset).coerceIn(0, providers.lastIndex)
        if (from < 0 || from == to) return
        val item = providers.removeAt(from)
        providers.add(to, item)
        updateLlmSettings(llmSettings.value.copy(providers = providers))
    }

    fun loadCachedModels(providerId: String) {
        fetchedModels.value = llmSettings.value.cacheFor(providerId)?.takeIf { it.isFresh() }?.models.orEmpty()
        llmProviderMessage.value = null
    }

    fun fetchLlmModels(config: LlmServiceConfig, connectionTest: Boolean = false) {
        if (isFetchingModels.value) return
        modelFetchJob?.cancel()
        modelFetchJob = viewModelScope.launch {
            isFetchingModels.value = true
            llmProviderMessage.value = null
            runCatching { llmClient.fetchModels(config.normalized) }
                .onSuccess { models ->
                    fetchedModels.value = models
                    if (llmSettings.value.providers.any { it.id == config.id }) {
                        val cache = LlmModelCache(config.id, models, System.currentTimeMillis())
                        val caches = llmSettings.value.modelCaches.filterNot { it.providerId == config.id } + cache
                        updateLlmSettings(llmSettings.value.copy(modelCaches = caches))
                    }
                    llmProviderMessage.value = if (connectionTest) "连接成功，发现 ${models.size} 个模型" else "已获取 ${models.size} 个模型"
                }
                .onFailure { llmProviderMessage.value = it.message ?: "模型列表获取失败，可继续手动填写" }
            isFetchingModels.value = false
        }
    }

    fun clearLlmProviderMessage() {
        llmProviderMessage.value = null
    }

    fun toggleFavoritePrompt(prompt: String) {
        val clean = prompt.trim().take(MAX_PROMPT_LENGTH)
        if (clean.isBlank() || clean in LlmSettings.builtInPrompts) return
        val favorites = llmSettings.value.favoritePrompts.toMutableList().apply {
            if (!remove(clean)) add(clean)
        }
        updateLlmSettings(llmSettings.value.copy(favoritePrompts = favorites))
    }

    private fun rememberPrompt(prompt: String) {
        val clean = prompt.trim().take(MAX_PROMPT_LENGTH)
        if (clean.isBlank()) return
        val recent = listOf(clean) + llmSettings.value.recentPrompts.filterNot { it == clean }
        updateLlmSettings(llmSettings.value.copy(recentPrompts = recent.take(10)))
    }

    private fun updateLlmSettings(settings: LlmSettings) {
        llmConfigStore.save(settings)
        llmSettings.value = settings
    }

    fun clearSummaryError() {
        summaryError.value = null
    }

    fun generateSummary(instruction: String, providerId: String? = null, modelOverride: String = "") {
        val state = uiState.value
        if (!state.appSettings.llmEnabled) {
            summaryError.value = "智能总结已关闭，请先在设置中开启"
            return
        }
        val provider = resolveProvider(providerId, modelOverride) ?: run {
            summaryError.value = "请先配置可用的模型服务"
            return
        }
        val review = state.review
        if (state.todos.isEmpty() && review?.hasContent() != true) {
            summaryError.value = "请先记录待办或复盘内容"
            return
        }
        if (isGeneratingSummary.value) return
        rememberPrompt(instruction)
        summaryJob = viewModelScope.launch {
            isGeneratingSummary.value = true
            summaryError.value = null
            runCatching {
                persistReviewDraft()
                llmClient.generate(provider, buildDailySummaryPrompt(state, instruction))
            }.onSuccess { content ->
                repository.saveSummary(
                    date = state.selectedDate.toString(),
                    content = content,
                    provider = provider.name,
                    model = provider.model,
                    current = state.summary
                )
            }.onFailure { error ->
                if (error !is CancellationException) summaryError.value = error.message ?: "总结生成失败，请稍后重试"
            }
            isGeneratingSummary.value = false
        }
    }

    fun cancelSummary() {
        summaryJob?.cancel()
        isGeneratingSummary.value = false
    }

    fun generateRangeSummary(
        start: LocalDate,
        end: LocalDate,
        periodType: String,
        instruction: String,
        includeExistingSummaries: Boolean,
        providerId: String? = null,
        modelOverride: String = ""
    ) {
        if (!appSettingsStore.settings.value.llmEnabled) {
            rangeSummaryError.value = "智能总结已关闭，请先在设置中开启"
            return
        }
        val days = ChronoUnit.DAYS.between(start, end) + 1
        val provider = resolveProvider(providerId, modelOverride) ?: run {
            rangeSummaryError.value = "请先配置可用的模型服务"
            return
        }
        if (days !in 1..MAX_RANGE_DAYS) {
            rangeSummaryError.value = "日期范围必须为 1 至 $MAX_RANGE_DAYS 天"
            return
        }
        if (isGeneratingRangeSummary.value) return
        rememberPrompt(instruction)
        rangeSummaryJob = viewModelScope.launch {
            isGeneratingRangeSummary.value = true
            rangeSummaryError.value = null
            runCatching {
                if (!selectedDate.value.isBefore(start) && !selectedDate.value.isAfter(end)) persistReviewDraft()
                val data = repository.rangeData(start.toString(), end.toString(), includeExistingSummaries)
                if (data.isEmpty) error("所选范围没有可总结的记录")
                generateRangeContent(provider, start, end, instruction, data)
            }.onSuccess { content ->
                repository.saveRangeSummary(
                    start = start.toString(),
                    end = end.toString(),
                    periodType = periodType,
                    prompt = instruction.trim().ifBlank { LlmSettings.builtInPrompts.first() },
                    content = content,
                    providerId = provider.id,
                    providerName = provider.name,
                    model = provider.model,
                    includeExistingSummaries = includeExistingSummaries
                )
            }.onFailure { error ->
                if (error !is CancellationException) rangeSummaryError.value = error.message ?: "范围总结生成失败"
            }
            isGeneratingRangeSummary.value = false
        }
    }

    private suspend fun generateRangeContent(
        provider: LlmServiceConfig,
        start: LocalDate,
        end: LocalDate,
        instruction: String,
        data: RangeSourceData
    ): String {
        val blocks = buildRangeBlocks(data)
        val cleanInstruction = instruction.trim().ifBlank { "总结这段时间的进展" }
        if (blocks.sumOf(String::length) <= MAX_RANGE_CHARS) {
            return llmClient.generate(provider, buildRangePrompt(start, end, cleanInstruction, blocks.joinToString("\n\n")))
        }
        val chunks = chunkBlocks(blocks, MAX_RANGE_CHARS)
        var partials = chunks.mapIndexed { index, chunk ->
            llmClient.generate(
                provider,
                "这是 ${start} 至 ${end} 记录的第 ${index + 1}/${chunks.size} 段。请只提炼事实、进展、问题和趋势，为最终综合保留关键信息。\n\n${chunk.joinToString("\n\n")}"
            )
        }
        var level = 1
        while (partials.sumOf(String::length) > MAX_RANGE_CHARS) {
            val groups = chunkBlocks(partials, MAX_RANGE_CHARS)
            partials = groups.mapIndexed { index, group ->
                llmClient.generate(
                    provider,
                    "这是范围总结的第 $level 轮归并、第 ${index + 1}/${groups.size} 组。请继续去重并保留事实、趋势、问题和建议。\n\n${group.joinToString("\n\n")}"
                )
            }
            level += 1
        }
        return llmClient.generate(
            provider,
            "日期范围：$start 至 $end\n本次指令：$cleanInstruction\n下面是分段摘要，请去重后完成最终综合，不要提及分段过程。\n\n" +
                partials.mapIndexed { index, value -> "分段 ${index + 1}：\n$value" }.joinToString("\n\n")
        )
    }

    fun cancelRangeSummary() {
        rangeSummaryJob?.cancel()
        isGeneratingRangeSummary.value = false
    }

    fun deleteRangeSummary(summary: RangeSummary) = viewModelScope.launch { repository.deleteRangeSummary(summary) }

    fun clearRangeSummaryError() {
        rangeSummaryError.value = null
    }

    private fun resolveProvider(providerId: String?, modelOverride: String): LlmServiceConfig? {
        val provider = llmSettings.value.providers.firstOrNull { it.id == providerId } ?: llmSettings.value.activeProvider
        val resolved = provider?.copy(model = modelOverride.trim().ifBlank { provider.model })
        return resolved?.takeIf(LlmServiceConfig::isConfigured)
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
                .onSuccess {
                    syncMessage.value = it.message
                    rescheduleTodoRemindersNow()
                }
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

    private fun rescheduleTodoReminders() {
        viewModelScope.launch { rescheduleTodoRemindersNow() }
    }

    private suspend fun rescheduleTodoRemindersNow() {
        repository.activeReminderTodos().forEach { TodoReminderWorker.schedule(application, it) }
    }

    private fun buildDailySummaryPrompt(state: DayUiState, instruction: String): String = buildString {
        val review = state.review
        appendLine("本次指令：${instruction.trim().ifBlank { "总结今日进展" }}")
        appendLine("日期：${state.selectedDate}")
        appendLine("待办完成：${state.completedCount}/${state.todos.size}")
        appendLine("待办记录：")
        if (state.todos.isEmpty()) appendLine("（无）")
        state.todos.forEach { appendLine("- [${if (it.isCompleted) "已完成" else "未完成"}] ${it.title}") }
        appendLine("复盘记录：")
        appendLine("解决了什么问题：${review?.challenge?.ifBlank { "（未填写）" } ?: "（未填写）"}")
        appendLine("随便写写：${review?.freeWriteText()?.ifBlank { "（未填写）" } ?: "（未填写）"}")
    }

    private fun buildRangeBlocks(data: RangeSourceData): List<String> {
        val todos = data.todos.groupBy(TodoItem::date)
        val reviews = data.reviews.associateBy(DailyReview::date)
        val summaries = data.summaries.associateBy(DailySummary::date)
        return (todos.keys + reviews.keys + summaries.keys).distinct().sorted().map { date ->
            buildString {
                appendLine("日期：$date")
                val dayTodos = todos[date].orEmpty()
                appendLine("待办：")
                if (dayTodos.isEmpty()) appendLine("（无）")
                dayTodos.forEach { appendLine("- [${if (it.isCompleted) "已完成" else "未完成"}] ${it.title}") }
                reviews[date]?.let { review ->
                    if (review.hasContent()) {
                        appendLine("解决了什么问题：${review.challenge.ifBlank { "（未填写）" }}")
                        appendLine("随便写写：${review.freeWriteText().ifBlank { "（未填写）" }}")
                    }
                }
                summaries[date]?.let { appendLine("已有每日总结：${it.content}") }
            }.trim()
        }
    }

    private fun buildRangePrompt(start: LocalDate, end: LocalDate, instruction: String, content: String): String =
        "日期范围：$start 至 $end\n本次指令：$instruction\n请综合下面的原始记录，区分已完成与未完成事项，不要编造缺失信息。\n\n$content"

    private fun chunkBlocks(blocks: List<String>, limit: Int): List<List<String>> {
        val chunks = mutableListOf<MutableList<String>>()
        val safeBlocks = blocks.flatMap { block ->
            if (block.length <= limit) listOf(block) else block.chunked(limit - 120).mapIndexed { index, part ->
                "同一日期记录续段 ${index + 1}：\n$part"
            }
        }
        safeBlocks.forEach { block ->
            val current = chunks.lastOrNull()
            if (current == null || current.sumOf(String::length) + block.length > limit) {
                chunks += mutableListOf(block)
            } else {
                current += block
            }
        }
        return chunks
    }

    private data class LlmGenerationState(
        val settings: LlmSettings,
        val isGeneratingDaily: Boolean,
        val dailyError: String?,
        val isGeneratingRange: Boolean,
        val rangeError: String?
    )

    private data class LlmToolState(val models: List<String>, val isFetching: Boolean, val message: String?)

    private data class SyncUiState(
        val config: WebDavConfig,
        val isTesting: Boolean,
        val isSyncing: Boolean,
        val message: String?,
        val metadata: AppMetadata?
    )

    private data class LocalSettingsState(val settings: AppSettings, val dataMessage: String?)

    private data class TodoPresentation(
        val groups: List<TodoGroup>,
        val steps: List<TodoStep>,
        val todos: List<TodoItem>,
        val mode: TodoViewMode,
        val query: String
    )

    companion object {
        private const val MAX_BACKUP_BYTES = 10_000_000
        private const val MAX_PROMPT_LENGTH = 240
        private const val MAX_RANGE_DAYS = 93L
        private const val MAX_RANGE_CHARS = 12_000
    }
}

fun DailyReview.freeWriteText(): String {
    if (learning.isBlank() && tomorrowFocus.isBlank()) return highlight
    return buildList {
        if (highlight.isNotBlank()) add("今日亮点：$highlight")
        if (learning.isNotBlank()) add("今日收获：$learning")
        if (tomorrowFocus.isNotBlank()) add("明日重点：$tomorrowFocus")
    }.joinToString("\n\n")
}

fun DailyReview.withFreeWrite(value: String): DailyReview = copy(highlight = value, learning = "", tomorrowFocus = "")

class DayViewModelFactory(
    private val repository: LimeDayRepository,
    private val llmConfigStore: SecureLlmConfigStore,
    private val llmClient: LlmClient,
    private val webDavConfigStore: SecureWebDavConfigStore,
    private val webDavClient: WebDavClient,
    private val syncCoordinator: WebDavSyncCoordinator,
    private val appSettingsStore: AppSettingsStore,
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
        appSettingsStore,
        application
    ) as T
}
