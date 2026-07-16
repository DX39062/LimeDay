package com.limeday.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.limeday.app.ui.DayScreen
import com.limeday.app.ui.DayViewModel
import com.limeday.app.ui.DayViewModelFactory
import com.limeday.app.ui.theme.LimeDayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LimeDayTheme {
                val application = application as LimeDayApplication
                val viewModel: DayViewModel = viewModel(
                    factory = DayViewModelFactory(
                        application.repository,
                        application.llmConfigStore,
                        application.llmClient
                    )
                )
                DayScreen(viewModel)
            }
        }
    }
}
