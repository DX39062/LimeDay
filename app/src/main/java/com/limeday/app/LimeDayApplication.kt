package com.limeday.app

import android.app.Application
import com.limeday.app.data.AppDatabase
import com.limeday.app.data.LimeDayRepository
import com.limeday.app.llm.LlmClient
import com.limeday.app.llm.SecureLlmConfigStore
import com.limeday.app.settings.AppSettingsStore
import com.limeday.app.settings.DailyReminderWorker
import com.limeday.app.sync.SecureWebDavConfigStore
import com.limeday.app.sync.WebDavClient
import com.limeday.app.sync.WebDavSyncCoordinator
import com.limeday.app.sync.WebDavSyncWorker

class LimeDayApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: LimeDayRepository by lazy { LimeDayRepository(database) }
    val llmConfigStore: SecureLlmConfigStore by lazy { SecureLlmConfigStore(this) }
    val llmClient: LlmClient by lazy { LlmClient() }
    val webDavConfigStore: SecureWebDavConfigStore by lazy { SecureWebDavConfigStore(this) }
    val webDavClient: WebDavClient by lazy { WebDavClient() }
    val appSettingsStore: AppSettingsStore by lazy { AppSettingsStore(this) }
    val syncCoordinator: WebDavSyncCoordinator by lazy {
        WebDavSyncCoordinator(repository, webDavClient)
    }

    override fun onCreate() {
        super.onCreate()
        DailyReminderWorker.createChannel(this)
        DailyReminderWorker.scheduleAll(this, appSettingsStore.settings.value)
        if (webDavConfigStore.load().isConfigured) WebDavSyncWorker.schedule(this)
    }
}
