package com.limeday.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.activity.compose.BackHandler
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun LimeDayApp(
    viewModel: DayViewModel,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestExport: () -> Unit,
    onRequestImport: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: "day"
    val mainRoutes = setOf("day", "summary", "settings")

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (route in mainRoutes) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == "day",
                        onClick = {
                            navController.navigate("day") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { NavigationDoodleIcon(DoodleIconType.Todo, route == "day", "待办") },
                        label = { Text("待办") }
                    )
                    NavigationBarItem(
                        selected = route == "summary",
                        onClick = {
                            navController.navigate("summary") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { NavigationDoodleIcon(DoodleIconType.Summary, route == "summary", "总结") },
                        label = { Text("总结") }
                    )
                    NavigationBarItem(
                        selected = route == "settings",
                        onClick = {
                            navController.navigate("settings") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { NavigationDoodleIcon(DoodleIconType.Settings, route == "settings", "设置") },
                        label = { Text("设置") }
                    )
                }
            }
        }
    ) { rootPadding ->
        NavHost(
            navController = navController,
            startDestination = "day",
            modifier = if (route in mainRoutes) Modifier.padding(rootPadding) else Modifier,
            enterTransition = { fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 12 } },
            exitTransition = { fadeOut(tween(150)) + slideOutHorizontally(tween(200)) { -it / 16 } },
            popEnterTransition = { fadeIn(tween(180)) + slideInHorizontally(tween(220)) { -it / 12 } },
            popExitTransition = { fadeOut(tween(150)) + slideOutHorizontally(tween(200)) { it / 16 } }
        ) {
            composable("day") {
                DayScreen(
                    state = state,
                    onPreviousDay = viewModel::previousDay,
                    onNextDay = viewModel::nextDay,
                    onToday = viewModel::today,
                    onAddTodo = viewModel::addTodo,
                    onToggleTodo = viewModel::toggleTodo,
                    onUpdateTodo = { todo, edit -> viewModel.updateTodo(todo, edit) },
                    onAddStep = viewModel::addTodoStep,
                    onToggleStep = viewModel::toggleTodoStep,
                    onUpdateStep = viewModel::updateTodoStep,
                    onMoveStep = viewModel::moveTodoStep,
                    onDeleteStep = viewModel::deleteTodoStep,
                    onSetTodoPriority = viewModel::setTodoPriority,
                    onMoveTodo = viewModel::moveTodo,
                    onDuplicateTodo = viewModel::duplicateTodo,
                    onDeleteTodo = viewModel::deleteTodo,
                    onRestoreTodo = viewModel::restoreTodo,
                    onOpenReview = { navController.navigate("review") },
                    onSelectDate = viewModel::selectDate,
                    onLoadMonth = viewModel::loadMonthTodoStatuses,
                    onSetViewMode = viewModel::setTodoViewMode,
                    onSearch = viewModel::setTodoSearchQuery,
                    onAddGroup = viewModel::addTodoGroup,
                    onUpdateGroup = viewModel::updateTodoGroup,
                    onMoveGroup = viewModel::moveTodoGroup,
                    onDeleteGroup = viewModel::deleteTodoGroup
                )
            }
            composable("summary") {
                SummaryScreen(
                    state = state,
                    onGenerate = viewModel::generateRangeSummary,
                    onCancel = viewModel::cancelRangeSummary,
                    onDelete = viewModel::deleteRangeSummary,
                    onClearError = viewModel::clearRangeSummaryError,
                    onToggleFavorite = viewModel::toggleFavoritePrompt
                )
            }
            composable("review") {
                ReviewScreen(
                    state = state,
                    onBack = navController::popBackStack,
                    onUpdateReview = viewModel::updateReview,
                    onFlushReview = viewModel::flushReview,
                    onToggleTodo = viewModel::toggleTodo,
                    onUpdateTodo = { todo, edit -> viewModel.updateTodo(todo, edit) },
                    onAddStep = viewModel::addTodoStep,
                    onToggleStep = viewModel::toggleTodoStep,
                    onUpdateStep = viewModel::updateTodoStep,
                    onMoveStep = viewModel::moveTodoStep,
                    onDeleteStep = viewModel::deleteTodoStep,
                    onSetTodoPriority = viewModel::setTodoPriority,
                    onMoveTodo = viewModel::moveTodo,
                    onDuplicateTodo = viewModel::duplicateTodo,
                    onDeleteTodo = viewModel::deleteTodo,
                    onRestoreTodo = viewModel::restoreTodo,
                    onGenerateSummary = viewModel::generateSummary,
                    onCancelSummary = viewModel::cancelSummary,
                    onClearError = viewModel::clearSummaryError,
                    onToggleFavorite = viewModel::toggleFavoritePrompt
                )
            }
            composable("settings") {
                SettingsScreen(
                    state = state,
                    notificationPermissionGranted = notificationPermissionGranted,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onSetThemeMode = viewModel::setThemeMode,
                    onSetLlmEnabled = viewModel::setLlmEnabled,
                    onSetTodoReminder = viewModel::setTodoReminder,
                    onSetReviewReminder = viewModel::setReviewReminder,
                    onRequestExport = onRequestExport,
                    onRequestImport = onRequestImport,
                    onClearDataMessage = viewModel::clearDataMessage,
                    onOpenTrash = { navController.navigate("trash") },
                    onOpenLlmProviders = { navController.navigate("llm-providers") },
                    onOpenWebDav = { navController.navigate("webdav") }
                )
            }
            composable("llm-providers") {
                LlmProviderSettingsScreen(
                    state = state,
                    onBack = navController::popBackStack,
                    onSave = viewModel::saveLlmProvider,
                    onActivate = viewModel::activateLlmProvider,
                    onDuplicate = viewModel::duplicateLlmProvider,
                    onDelete = viewModel::deleteLlmProvider,
                    onMove = viewModel::moveLlmProvider,
                    onFetchModels = viewModel::fetchLlmModels,
                    onLoadCachedModels = viewModel::loadCachedModels,
                    onClearMessage = viewModel::clearLlmProviderMessage
                )
            }
            composable("trash") {
                TrashScreen(
                    state = state,
                    onBack = navController::popBackStack,
                    onRestore = viewModel::restoreTodo,
                    onPermanentDelete = viewModel::permanentlyDeleteTodos
                )
            }
            composable("webdav") {
                WebDavSettingsScreen(
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

    BackHandler(enabled = route != "day") {
        if (route in mainRoutes) {
            navController.navigate("day") {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                launchSingleTop = true
            }
        } else {
            navController.popBackStack()
        }
    }
}
