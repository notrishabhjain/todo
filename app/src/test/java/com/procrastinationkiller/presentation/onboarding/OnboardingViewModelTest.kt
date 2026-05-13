package com.procrastinationkiller.presentation.onboarding

import com.procrastinationkiller.domain.repository.OnboardingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeOnboardingRepository
    private lateinit var viewModel: OnboardingViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeOnboardingRepository()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `completeOnboarding sets onboarding completed to true`() = runTest {
        viewModel = OnboardingViewModel(fakeRepository)

        viewModel.completeOnboarding()
        advanceUntilIdle()

        assertTrue(fakeRepository.isOnboardingCompleted())
    }

    @Test
    fun `onboarding is not completed by default`() = runTest {
        viewModel = OnboardingViewModel(fakeRepository)
        advanceUntilIdle()

        assertFalse(fakeRepository.isOnboardingCompleted())
    }

    @Test
    fun `completeOnboarding can be called multiple times without error`() = runTest {
        viewModel = OnboardingViewModel(fakeRepository)

        viewModel.completeOnboarding()
        viewModel.completeOnboarding()
        advanceUntilIdle()

        assertTrue(fakeRepository.isOnboardingCompleted())
    }

    private class FakeOnboardingRepository : OnboardingRepository {
        private val _onboardingCompleted = MutableStateFlow(false)

        override val onboardingCompleted: Flow<Boolean> = _onboardingCompleted

        override suspend fun setOnboardingCompleted(completed: Boolean) {
            _onboardingCompleted.value = completed
        }

        fun isOnboardingCompleted(): Boolean = _onboardingCompleted.value
    }
}
