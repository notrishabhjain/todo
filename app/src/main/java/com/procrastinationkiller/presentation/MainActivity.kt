package com.procrastinationkiller.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.procrastinationkiller.presentation.navigation.NavGraph
import com.procrastinationkiller.presentation.navigation.Routes
import com.procrastinationkiller.presentation.theme.ProcrastinationKillerTheme
import dagger.hilt.android.AndroidEntryPoint

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProcrastinationKillerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
private fun MainScreen() {
    val mainViewModel: MainViewModel = hiltViewModel()
    val startDestination by mainViewModel.startDestination.collectAsState()
    val isNotificationListenerEnabled by mainViewModel.isNotificationListenerEnabled.collectAsState()
    val context = LocalContext.current

    if (startDestination == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem("Dashboard", Icons.Default.Home, Routes.DASHBOARD),
        BottomNavItem("Inbox", Icons.Default.Notifications, Routes.INBOX),
        BottomNavItem("Tasks", Icons.AutoMirrored.Filled.List, Routes.TASKS),
        BottomNavItem("Settings", Icons.Default.Settings, Routes.SETTINGS)
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavGraph(
                navController = navController,
                startDestination = startDestination!!,
                isNotificationListenerEnabled = isNotificationListenerEnabled,
                onOpenNotificationSettings = {
                    val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            )
        }
    }
}
