package com.sunpawtechnologies.scrollless.domain

import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.model.LimitStatus
import com.sunpawtechnologies.scrollless.domain.model.limitStatus
import com.sunpawtechnologies.scrollless.domain.repository.LimitChange
import com.sunpawtechnologies.scrollless.domain.repository.limitOn
import com.sunpawtechnologies.scrollless.ui.components.niceCeil
import com.sunpawtechnologies.scrollless.ui.limits.LimitError
import com.sunpawtechnologies.scrollless.ui.limits.validateLimit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class LimitRulesTest {

    @Test
    fun `limit status boundaries`() {
        assertEquals(LimitStatus.WITHIN, limitStatus(39, 50, 80))
        assertEquals(LimitStatus.APPROACHING, limitStatus(40, 50, 80))
        assertEquals(LimitStatus.APPROACHING, limitStatus(49, 50, 80))
        assertEquals(LimitStatus.REACHED, limitStatus(50, 50, 80))
        assertEquals(LimitStatus.REACHED, limitStatus(120, 50, 80))
        assertEquals(LimitStatus.WITHIN, limitStatus(10, 0, 80))
    }

    @Test
    fun `limit input validation`() {
        assertNull(validateLimit(LimitMetric.INSTAGRAM_REELS, "50"))
        assertNull(validateLimit(LimitMetric.INSTAGRAM_REELS, " 1 "))
        assertEquals(LimitError.NOT_A_NUMBER, validateLimit(LimitMetric.INSTAGRAM_REELS, ""))
        assertEquals(LimitError.NOT_A_NUMBER, validateLimit(LimitMetric.INSTAGRAM_REELS, "abc"))
        assertEquals(LimitError.OUT_OF_RANGE, validateLimit(LimitMetric.INSTAGRAM_REELS, "0"))
        assertEquals(LimitError.OUT_OF_RANGE, validateLimit(LimitMetric.INSTAGRAM_REELS, "1001"))
        assertEquals(LimitError.OUT_OF_RANGE, validateLimit(LimitMetric.YOUTUBE_REGULAR_VIDEO, "4"))
        assertNull(validateLimit(LimitMetric.YOUTUBE_REGULAR_VIDEO, "720"))
    }

    @Test
    fun `limit history resolves the value in effect on a day`() {
        val history = listOf(LimitChange(LocalDate.parse("2026-10-01"), 40), LimitChange(LocalDate.parse("2026-10-20"), 25))
        assertEquals(50, history.limitOn(LocalDate.parse("2026-09-30"), default = 50))
        assertEquals(40, history.limitOn(LocalDate.parse("2026-10-19"), default = 50))
        assertEquals(25, history.limitOn(LocalDate.parse("2026-10-20"), default = 50))
    }

    @Test
    fun `chart axis maximum rounds up to readable steps`() {
        assertEquals(2L, niceCeil(0))
        assertEquals(10L, niceCeil(7))
        assertEquals(50L, niceCeil(37))
        assertEquals(50L, niceCeil(50))
        assertEquals(100L, niceCeil(51))
        // Minutes: 47 minutes of usage gets a 50-minute axis.
        assertEquals(50 * 60_000L, niceCeil(47 * 60_000L, unit = 60_000))
    }
}
