package gh.otumfour.o24.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingRouterTest {
    @Test
    fun finishKeepsTheUserOnSettings() {
        assertFalse(OnboardingRouter.showSettings(onboarded = false))
        assertTrue(OnboardingRouter.showSettings(onboarded = true))
    }

    @Test
    fun incompleteOnboardingResumesTheStoredStep() {
        assertEquals(1, OnboardingRouter.resumePage(stored = 1))
        assertEquals(2, OnboardingRouter.resumePage(stored = 2, requested = null, restored = null))
    }

    @Test
    fun restoredPageWinsOverAStaleRequest() {
        assertEquals(3, OnboardingRouter.resumePage(stored = 1, requested = 0, restored = 3))
    }

    @Test
    fun pageIsClamped() {
        assertEquals(0, OnboardingRouter.resumePage(stored = -4))
        assertEquals(OnboardingRouter.PAGE_COUNT - 1, OnboardingRouter.resumePage(stored = 40))
    }
}
