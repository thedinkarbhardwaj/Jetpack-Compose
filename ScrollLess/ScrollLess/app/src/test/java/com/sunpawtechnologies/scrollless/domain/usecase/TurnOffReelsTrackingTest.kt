package com.sunpawtechnologies.scrollless.domain.usecase

import com.sunpawtechnologies.scrollless.domain.model.AppSettings
import com.sunpawtechnologies.scrollless.testing.FakeSettingsRepository
import com.sunpawtechnologies.scrollless.tracking.accessibility.ReelsTrackingSwitch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TurnOffReelsTrackingTest {

    private val settings = FakeSettingsRepository().apply { emit(AppSettings()) }

    @Test
    fun `turning off records the choice and switches off only scrollless's service`() = runTest {
        var calls = 0
        val turnOff = TurnOffReelsTracking(settings, ReelsTrackingSwitch { calls++; true })

        assertTrue(turnOff())
        assertEquals(1, calls)
        assertTrue(settings.settings.first().reelsTrackingTurnedOffByUser)
    }

    @Test
    fun `when the service isn't running here the caller is told to use settings`() = runTest {
        val turnOff = TurnOffReelsTracking(settings, ReelsTrackingSwitch { false })

        assertFalse(turnOff())
        // Still recorded, so switching it off in system settings isn't then reported as a problem.
        assertTrue(settings.settings.first().reelsTrackingTurnedOffByUser)
    }
}
