package com.procrastinationkiller.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.procrastinationkiller.domain.model.AppCategory
import com.procrastinationkiller.domain.model.InstalledAppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstalledAppsProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val communicationPackagePatterns = listOf(
        "messenger", "chat", "sms", "mail", "whatsapp", "telegram",
        "slack", "teams", "signal", "viber", "wechat", "skype",
        "discord", "hangouts", "messages"
    )

    fun getInstalledApps(): List<InstalledAppInfo> {
        val packageManager = context.packageManager
        val installedApps = packageManager.getInstalledApplications(0)

        return installedApps
            .filter { appInfo ->
                packageManager.getLaunchIntentForPackage(appInfo.packageName) != null &&
                    appInfo.packageName != context.packageName
            }
            .map { appInfo ->
                InstalledAppInfo(
                    packageName = appInfo.packageName,
                    label = packageManager.getApplicationLabel(appInfo).toString(),
                    category = categorizeApp(appInfo),
                    hasIcon = true
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private fun categorizeApp(appInfo: ApplicationInfo): AppCategory {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.SOCIAL
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.PRODUCTIVITY
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_VIDEO,
                ApplicationInfo.CATEGORY_NEWS,
                ApplicationInfo.CATEGORY_GAME,
                ApplicationInfo.CATEGORY_IMAGE,
                ApplicationInfo.CATEGORY_MAPS -> return AppCategory.OTHER
            }
        }

        val packageLower = appInfo.packageName.lowercase()
        if (communicationPackagePatterns.any { pattern -> packageLower.contains(pattern) }) {
            return AppCategory.COMMUNICATION
        }

        return AppCategory.OTHER
    }
}
