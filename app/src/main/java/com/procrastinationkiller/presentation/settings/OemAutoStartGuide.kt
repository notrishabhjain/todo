package com.procrastinationkiller.presentation.settings

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Provides OEM-specific guidance for enabling auto-start/background protection.
 * Many Android OEMs aggressively kill background services; this guide helps users
 * configure their device to allow reliable reminder delivery.
 */

data class OemAutoStartInfo(
    val manufacturer: String,
    val title: String,
    val instructions: String,
    val intent: Intent?
)

/**
 * Detects the device manufacturer and returns appropriate auto-start guidance.
 */
fun getOemAutoStartInfo(): OemAutoStartInfo? {
    val manufacturer = Build.MANUFACTURER.lowercase()

    return when {
        manufacturer.contains("xiaomi") -> OemAutoStartInfo(
            manufacturer = "Xiaomi",
            title = "Enable AutoStart (Xiaomi/MIUI)",
            instructions = "Go to Security app > Permissions > AutoStart, then enable this app. " +
                "Also check Settings > Battery & performance > App battery saver and set this app to 'No restrictions'.",
            intent = try {
                Intent().apply {
                    component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } catch (e: Exception) { null }
        )

        manufacturer.contains("samsung") -> OemAutoStartInfo(
            manufacturer = "Samsung",
            title = "Disable Battery Optimization (Samsung)",
            instructions = "Go to Settings > Battery and device care > Battery > Background usage limits. " +
                "Add this app to 'Never sleeping apps'. Also disable 'Put unused apps to sleep'.",
            intent = try {
                Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.battery.ui.BatteryActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } catch (e: Exception) { null }
        )

        manufacturer.contains("huawei") || manufacturer.contains("honor") -> OemAutoStartInfo(
            manufacturer = "Huawei",
            title = "Add to Protected Apps (Huawei)",
            instructions = "Go to Settings > Battery > App launch. Find this app and set it to " +
                "'Manage manually', then enable Auto-launch, Secondary launch, and Run in background.",
            intent = try {
                Intent().apply {
                    component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } catch (e: Exception) { null }
        )

        manufacturer.contains("oppo") || manufacturer.contains("realme") -> OemAutoStartInfo(
            manufacturer = "Oppo/Realme",
            title = "Enable AutoStart (Oppo/Realme)",
            instructions = "Go to Settings > App Management > App List > This app > Auto-start and enable it. " +
                "Also check Battery > Power saving exemptions and add this app.",
            intent = try {
                Intent().apply {
                    component = ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.startupapp.StartupAppListActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } catch (e: Exception) { null }
        )

        manufacturer.contains("vivo") -> OemAutoStartInfo(
            manufacturer = "Vivo",
            title = "Enable AutoStart (Vivo)",
            instructions = "Go to Settings > Battery > Background power consumption management. " +
                "Find this app and allow background activity. Also enable AutoStart in " +
                "Settings > More Settings > Applications > Autostart.",
            intent = try {
                Intent().apply {
                    component = ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } catch (e: Exception) { null }
        )

        manufacturer.contains("oneplus") -> OemAutoStartInfo(
            manufacturer = "OnePlus",
            title = "Disable Battery Optimization (OnePlus)",
            instructions = "Go to Settings > Battery > Battery Optimization. Select 'All apps' from " +
                "the dropdown, find this app, and select 'Don't optimize'. Also check Settings > Apps > " +
                "This app > Battery > Allow background activity.",
            intent = try {
                Intent().apply {
                    component = ComponentName(
                        "com.oneplus.security",
                        "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } catch (e: Exception) { null }
        )

        else -> null
    }
}

/**
 * Card composable showing OEM-specific auto-start/background protection guidance.
 * Only displays when the device manufacturer is recognized as one that aggressively
 * kills background services.
 */
@Composable
fun OemAutoStartGuideCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val oemInfo = getOemAutoStartInfo() ?: return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = oemInfo.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = oemInfo.instructions,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            oemInfo.intent?.let { intent ->
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Intent not available on this device variant
                        }
                    }
                ) {
                    Text("Open Settings")
                }
            }
        }
    }
}
