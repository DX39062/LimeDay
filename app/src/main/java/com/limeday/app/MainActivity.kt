package com.limeday.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.limeday.app.ui.DayViewModel
import com.limeday.app.ui.DayViewModelFactory
import com.limeday.app.ui.LimeDayApp
import com.limeday.app.ui.theme.LimeDayTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private var notificationPermissionGranted by mutableStateOf(false)
    private lateinit var dayViewModel: DayViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as LimeDayApplication
        val viewModel = ViewModelProvider(
            this,
            DayViewModelFactory(
                repository = app.repository,
                llmConfigStore = app.llmConfigStore,
                llmClient = app.llmClient,
                webDavConfigStore = app.webDavConfigStore,
                webDavClient = app.webDavClient,
                syncCoordinator = app.syncCoordinator,
                appSettingsStore = app.appSettingsStore,
                application = app
            )
        )[DayViewModel::class.java]
        dayViewModel = viewModel
        intent.getStringExtra(EXTRA_SELECTED_DATE)?.let { value ->
            runCatching { LocalDate.parse(value) }.getOrNull()?.let(viewModel::selectDate)
        }

        notificationPermissionGranted = hasNotificationPermission()
        val notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted -> notificationPermissionGranted = granted }
        val exportLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri -> uri?.let(viewModel::exportData) }
        val importLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri -> uri?.let(viewModel::importData) }

        setContent {
            val settings by app.appSettingsStore.settings.collectAsStateWithLifecycle()
            LimeDayTheme(settings.themeMode) {
                LimeDayApp(
                    viewModel = viewModel,
                    notificationPermissionGranted = notificationPermissionGranted,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission()) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onRequestExport = {
                        exportLauncher.launch("LimeDay-backup-${LocalDate.now()}.json")
                    },
                    onRequestImport = { importLauncher.launch(arrayOf("application/json", "text/plain")) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        notificationPermissionGranted = hasNotificationPermission()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_SELECTED_DATE)?.let { value ->
            runCatching { LocalDate.parse(value) }.getOrNull()?.let(dayViewModel::selectDate)
        }
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val EXTRA_SELECTED_DATE = "com.limeday.app.SELECTED_DATE"
    }
}
