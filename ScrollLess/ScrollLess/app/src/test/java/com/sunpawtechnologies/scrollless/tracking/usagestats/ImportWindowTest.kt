package com.sunpawtechnologies.scrollless.tracking.usagestats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ImportWindowTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.parse("2026-10-25")
    private fun at(local: String) = LocalDateTime.parse(local).atZone(zone).toInstant()

    @Test
    fun `first run reaches back as far as Android may keep events`() {
        assertEquals(at("2026-10-18T00:00"), importWindowStart(today, zone, lastCoveredEnd = null))
    }

    @Test
    fun `regular use starts at yesterday's midnight`() {
        assertEquals(at("2026-10-24T00:00"), importWindowStart(today, zone, at("2026-10-25T09:00")))
    }

    @Test
    fun `after a gap it catches up from the day the last import ended`() {
        assertEquals(at("2026-10-21T00:00"), importWindowStart(today, zone, at("2026-10-21T22:30")))
    }

    @Test
    fun `catch-up never goes back more than seven days`() {
        assertEquals(at("2026-10-18T00:00"), importWindowStart(today, zone, at("2026-09-01T10:00")))
    }

    @Test
    fun `complete history covers the whole window`() {
        val start = at("2026-10-24T00:00")
        assertEquals(start, coverageStart(start, firstEvent = at("2026-10-24T00:40"), now = at("2026-10-25T10:00"), zone = zone))
    }

    @Test
    fun `history that begins mid-day leaves that day untracked`() {
        // Android had already discarded events before Oct 20 15:00, so Oct 20 is incomplete.
        val covered = coverageStart(at("2026-10-18T00:00"), firstEvent = at("2026-10-20T15:00"), now = at("2026-10-25T10:00"), zone = zone)
        assertEquals(at("2026-10-21T00:00"), covered)
    }

    @Test
    fun `no events means nothing can be claimed`() {
        assertNull(coverageStart(at("2026-10-24T00:00"), firstEvent = null, now = at("2026-10-25T10:00"), zone = zone))
    }
}
