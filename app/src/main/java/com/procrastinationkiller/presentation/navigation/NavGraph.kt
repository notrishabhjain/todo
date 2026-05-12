package com.procrastinationkiller.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.procrastinationkiller.presentation.dashboard.DashboardScreen
import com.procrastinationkiller.presentation.inbox.InboxScreen
import com.procrastinationkiller.presentation.taskdetail.TaskDetailScreen
import com.procrastinationkiller.presentation.tasks.TasksListScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val INBOX = "inbox"
    const val TASKS = "tasks"
    const val TASK_DETAIL = "task_detail/{taskId}"
    const val SETTINGS = "settings"

    fun taskDetail(taskId: Long): String = "task_detail/$taskId"
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onTaskClick = { taskId ->
                    navController.navigate(Routes.taskDetail(taskId))
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
            PlaceholderScreen("Settings")
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = name)
    }
}
