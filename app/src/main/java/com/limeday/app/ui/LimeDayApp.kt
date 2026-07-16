package com.limeday.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun LimeDayApp(viewModel: DayViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "day") {
        composable("day") {
            DayScreen(
                state = state,
                onPreviousDay = viewModel::previousDay,
                onNextDay = viewModel::nextDay,
                onToday = viewModel::today,
                onAddTodo = viewModel::addTodo,
                onToggleTodo = viewModel::toggleTodo,
                onUpdateTodo = viewModel::updateTodo,
                onDeleteTodo = viewModel::deleteTodo,
                onOpenReview = { navController.navigate("review") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable("review") {
            ReviewScreen(
                state = state,
                onBack = navController::popBackStack,
                onUpdateReview = viewModel::updateReview,
                onFlushReview = viewModel::flushReview,
                onSaveLlmConfig = viewModel::saveLlmConfig,
                onClearLlmConfig = viewModel::clearLlmConfig,
                onGenerateSummary = viewModel::generateSummary,
                onCancelSummary = viewModel::cancelSummary,
                onClearError = viewModel::clearSummaryError
            )
        }
        composable("settings") {
            SettingsScreen(
                state = state,
                onBack = navController::popBackStack,
                onSave = viewModel::saveWebDavConfig,
                onClear = viewModel::clearWebDavConfig,
                onTest = viewModel::testWebDav,
                onSync = viewModel::syncNow
            )
        }
    }
}
