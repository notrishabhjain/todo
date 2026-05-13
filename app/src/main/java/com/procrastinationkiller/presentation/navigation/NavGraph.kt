package com.procrastinationkiller.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.procrastinationkiller.presentation.analytics.AnalyticsScreen
import com.procrastinationkiller.presentation.dashboard.DashboardScreen
import com.procrastinationkiller.presentation.inbox.InboxScreen
import com.procrastinationkiller.presentation.insights.InsightsScreen
import com.procrastinationkiller.presentation.onboarding.OnboardingScreen
import com.procrastinationkiller.presentation.onboarding.OnboardingViewModel
import com.procrastinationkiller.presentation.settings.ExportImportScreen
import com.procrastinationkiller.presentation.settings.KeywordManagementScreen
import com.procrastinationkiller.presentation.settings.MonitoredAppsScreen
import com.procrastinationkiller.presentation.settings.SettingsScreen
import com.procrastinationkiller.presentation.taskdetail.TaskDetailScreen
import com.procrastinationkiller.presentation.tasks.TasksListScreen
import com.procrastinationkiller.presentation.transcript.MeetingTranscriptScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val INBOX = "inbox"
    const val TASKS = "tasks"
    const val TASK_DETAIL = "task_detail/{taskId}"
    const val SETTINGS = "settings"
    const val KEYWORD_MANAGEMENT = "keyword_management"
    const val ONBOARDING = "onboarding"
    const val ANALYTICS = "analytics"
    const val MEETING_TRANSCRIPT = "meeting_transcript"
    const val EXPORT_IMPORT = "export_import"
    const val INSIGHTS = "insights"
    const val MONITORED_APPS = "monitored_apps"

    fun taskDetail(taskId: Long): String = "task_detail/$taskId"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Routes.DASHBOARD,
    isNotificationListenerEnabled: Boolean = true,
    isPostNotificationsGranted: Boolean = true,
    onOpenNotificationSettings: () -> Unit = {},
    onRequestPostNotifications: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onTaskClick = { taskId ->
                    navController.navigate(Routes.taskDetail(taskId))
                },
                isNotificationListenerEnabled = isNotificationListenerEnabled,
                isPostNotificationsGranted = isPostNotificationsGranted,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onRequestPostNotifications = onRequestPostNotifications,
                onNavigateToTranscript = {
                    navController.navigate(Routes.MEETING_TRANSCRIPT)
                },
                onAddTaskManually = {
                    navController.navigate(Routes.TASKS)
                }
            )
        }
        composable(Routes.INBOX) {
            InboxScreen()
        }
        composable(Routes.TASKS) {
            TasksListScreen(
                onTaskClick = { taskId ->
                    navController.navigate(Routes.taskDetail(taskId))
                }
            )
        }
        composable(
            route = Routes.TASK_DETAIL,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) {
            TaskDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateToKeywords = {
                    navController.navigate(Routes.KEYWORD_MANAGEMENT)
                },
                onNavigateToAnalytics = {
                    navController.navigate(Routes.ANALYTICS)
                },
                onNavigateToInsights = {
                    navController.navigate(Routes.INSIGHTS)
                },
                onNavigateToExportImport = {
                    navController.navigate(Routes.EXPORT_IMPORT)
                },
                onNavigateToTranscript = {
                    navController.navigate(Routes.MEETING_TRANSCRIPT)
                },
                onNavigateToMonitoredApps = {
                    navController.navigate(Routes.MONITORED_APPS)
                }
            )
        }
        composable(Routes.KEYWORD_MANAGEMENT) {
            KeywordManagementScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.ONBOARDING) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val onboardingViewModel = hiltViewModel<OnboardingViewModel>()
            OnboardingScreen(
                onRequestNotificationAccess = {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                    )
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                },
                onSelectApps = {
                    navController.navigate(Routes.MONITORED_APPS)
                },
                onChooseAggressiveness = {
                    navController.navigate(Routes.SETTINGS)
                },
                onRequestBatteryOptimization = {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    ).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                },
                onComplete = {
                    onboardingViewModel.completeOnboarding()
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.ANALYTICS) {
            AnalyticsScreen()
        }
        composable(Routes.MEETING_TRANSCRIPT) {
            MeetingTranscriptScreen()
        }
        composable(Routes.EXPORT_IMPORT) {
            ExportImportScreen()
        }
        composable(Routes.INSIGHTS) {
            InsightsScreen()
        }
        composable(Routes.MONITORED_APPS) {
            MonitoredAppsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
