package com.limeday.app

import android.app.Application
import com.limeday.app.data.AppDatabase
import com.limeday.app.data.LimeDayRepository
import com.limeday.app.llm.LlmClient
import com.limeday.app.llm.SecureLlmConfigStore

class LimeDayApplication : Application() {
    val repository: LimeDayRepository by lazy {
        LimeDayRepository(AppDatabase.getInstance(this).limeDayDao())
    }
    val llmConfigStore: SecureLlmConfigStore by lazy { SecureLlmConfigStore(this) }
    val llmClient: LlmClient by lazy { LlmClient() }
}
