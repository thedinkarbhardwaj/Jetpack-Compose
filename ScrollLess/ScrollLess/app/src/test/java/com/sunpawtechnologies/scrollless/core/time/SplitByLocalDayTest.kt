package com.sunpawtechnologies.scrollless.core.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class SplitByLocalDayTest {

    private val kolkata = ZoneId.of("Asia/Kolkata")
    private val newYork = ZoneId.of("America/New_York")

    private fun at(local: String, zone: ZoneId): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    @Test
    fun `interval within one day is a single slice`() {
        val slices = splitByLocalDay(at("2026-10-24T10:00", kolkata), at("2026-10-24T10:30", kolkata), kolkata)
        assertEquals(1, slices.size)
        assertEquals(LocalDate.parse("2026-10-24"), slices.single().date)
        assertEquals(Duration.ofMinutes(30), slices.single().duration)
    }

    @Test
    fun `interval crossing midnight is credited to both days`() {
        val slices = splitByLocalDay(at("2026-10-24T23:30", kolkata), at("2026-10-25T00:45", kolkata), kolkata)
        assertEquals(listOf(LocalDate.parse("2026-10-24"), LocalDate.parse("2026-10-25")), slices.map { it.date })
        assertEquals(Duration.ofMinutes(30), slices[0].duration)
        assertEquals(Duration.ofMinutes(45), slices[1].duration)
        assertEquals(slices[0].endExclusive, slices[1].start)
    }

    @Test
    fun `multi-day interval keeps its total duration`() {
        val start = at("2026-10-23T22:00", kolkata)
        val end = at("2026-10-25T02:00", kolkata)
        val slices = splitByLocalDay(start, end, kolkata)
        assertEquals(3, slices.size)
        assertEquals(Duration.ofHours(24), slices[1].duration)
        assertEquals(Duration.between(start, end), slices.fold(Duration.ZERO) { acc, s -> acc + s.duration })
    }

    @Test
    fun `split respects a 23-hour DST day`() {
        val slices = splitByLocalDay(at("2026-03-07T23:00", newYork), at("2026-03-08T23:00", newYork), newYork)
        assertEquals(Duration.ofHours(1), slices[0].duration)
        assertEquals(Duration.ofHours(22), slices[1].duration)
    }

    @Test
    fun `the same instant belongs to different local days in different zones`() {
        val start = Instant.parse("2026-10-24T20:00:00Z")
        val end = start.plus(Duration.ofMinutes(10))
        assertEquals(LocalDate.parse("2026-10-24"), splitByLocalDay(start, end, ZoneOffset.UTC).single().date)
        assertEquals(LocalDate.parse("2026-10-25"), splitByLocalDay(start, end, kolkata).single().date)
    }

    @Test
    fun `empty interval produces no slices`() {
        val instant = at("2026-10-24T10:00", kolkata)
        assertTrue(splitByLocalDay(instant, instant, kolkata).isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `end before start is rejected`() {
        splitByLocalDay(at("2026-10-24T10:00", kolkata), at("2026-10-24T09:00", kolkata), kolkata)
    }
}
