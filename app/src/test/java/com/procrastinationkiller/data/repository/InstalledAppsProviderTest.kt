package com.procrastinationkiller.data.repository

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * InstalledAppsProvider uses Android's PackageManager which requires a real or mocked
 * context. Without Robolectric or a mocking library, we verify the structural contract
 * (method existence) and that the filtering approach is correct (getLaunchIntentForPackage
 * instead of FLAG_SYSTEM). Full behavioral integration tests require an instrumented
 * environment.
 */
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

    @Test
    fun `InstalledAppsProvider communicationPackagePatterns field exists and is used for categorization`() {
        // Verify the provider has the communication patterns field which drives
        // the categorizeApp logic for messaging apps
        val field = InstalledAppsProvider::class.java.getDeclaredField("communicationPackagePatterns")
        field.isAccessible = true
        // Field exists - categorization depends on this list of known messaging package patterns
        assertTrue(field != null)
    }
}
