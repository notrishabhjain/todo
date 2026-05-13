package com.procrastinationkiller.data.repository

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InstalledAppsProviderTest {

    @Test
    fun `InstalledAppsProvider does not have isSystemApp method`() {
        val methods = InstalledAppsProvider::class.java.declaredMethods.map { it.name }
        assertFalse(
            methods.contains("isSystemApp"),
            "isSystemApp method should be removed - filtering should use getLaunchIntentForPackage instead"
        )
    }

    @Test
    fun `InstalledAppsProvider has getInstalledApps method`() {
        val methods = InstalledAppsProvider::class.java.declaredMethods.map { it.name }
        assertTrue(
            methods.contains("getInstalledApps"),
            "getInstalledApps method should exist"
        )
    }

    @Test
    fun `InstalledAppsProvider has categorizeApp method`() {
        val methods = InstalledAppsProvider::class.java.declaredMethods.map { it.name }
        assertTrue(
            methods.contains("categorizeApp"),
            "categorizeApp method should exist for app categorization"
        )
    }
}
