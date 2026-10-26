package com.sunpawtechnologies.scrollless.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sunpawtechnologies.scrollless.data.db.ScrollLessDatabase
import com.sunpawtechnologies.scrollless.domain.model.ContentKind
import com.sunpawtechnologies.scrollless.domain.model.CoverageEndReason
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.model.Platform
import com.sunpawtechnologies.scrollless.domain.model.TrackingSource
import com.sunpawtechnologies.scrollless.domain.model.UsageMode
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Room queries behind analytics and coverage, on a real (in-memory) SQLite database. */
@RunWith(RobolectricTestRunner::class)
class AnalyticsQueriesTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private lateinit var database: ScrollLessDatabase
    private lateinit var usage: RoomUsageRepository
    private lateinit var coverage: RoomTrackingCoverageRepository
    private lateinit var limits: RoomLimitRepository

    private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()
    private fun day(text: String) = LocalDate.parse(text)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ScrollLessDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        usage = RoomUsageRepository(database.contentViewDao(), database.usageSessionDao())
        coverage = RoomTrackingCoverageRepository(database.trackingCoverageDao())
        limits = RoomLimitRepository(database.limitDao())
    }

    @After
    fun tearDown() = database.close()

    private suspend fun reel(local: String, key: String) =
        usage.recordContentView(ContentKind.INSTAGRAM_REEL, at(local), zone, key, 1)

    @Test
    fun `reel counts group by local day and hour`() = runTest {
        reel("2026-10-24T09:10", "a")
        reel("2026-10-24T09:50", "b")
        reel("2026-10-24T21:00", "c")
        reel("2026-10-26T08:00", "d")

        assertEquals(
            mapOf(day("2026-10-24") to 3L, day("2026-10-26") to 1L),
            usage.contentViewCountsByDay(ContentKind.INSTAGRAM_REEL, day("2026-10-20"), day("2026-10-26")),
        )
        assertEquals(mapOf(9 to 2L, 21 to 1L), usage.contentViewCountsByHour(ContentKind.INSTAGRAM_REEL, day("2026-10-24")))
    }

    @Test
    fun `usage time groups by day and splits across hours`() = runTest {
        usage.recordSession(Platform.INSTAGRAM, UsageMode.APP_FOREGROUND, TrackingSource.USAGE_STATS, at("2026-10-24T09:40"), at("2026-10-24T10:10"), zone)
        usage.recordSession(Platform.INSTAGRAM, UsageMode.APP_FOREGROUND, TrackingSource.USAGE_STATS, at("2026-10-25T23:50"), at("2026-10-26T00:20"), zone)

        val byDay = usage.usageMillisByDay(Platform.INSTAGRAM, UsageMode.APP_FOREGROUND, day("2026-10-24"), day("2026-10-26"))
        assertEquals(30 * 60_000L, byDay[day("2026-10-24")])
        assertEquals(10 * 60_000L, byDay[day("2026-10-25")])
        assertEquals(20 * 60_000L, byDay[day("2026-10-26")])

        val byHour = usage.usageMillisByHour(Platform.INSTAGRAM, UsageMode.APP_FOREGROUND, day("2026-10-24"))
        assertEquals(mapOf(9 to 20 * 60_000L, 10 to 10 * 60_000L), byHour)
    }

    @Test
    fun `tracked days come from coverage intervals, including open ones`() = runTest {
        val id = coverage.start(TrackingSource.ACCESSIBILITY, at("2026-10-20T22:00"))
        coverage.end(id, at("2026-10-22T08:00"), CoverageEndReason.STOPPED_BY_USER)
        coverage.start(TrackingSource.ACCESSIBILITY, at("2026-10-25T10:00")) // still running

        val days = coverage.trackedDays(TrackingSource.ACCESSIBILITY, day("2026-10-19"), day("2026-10-26"), zone, now = at("2026-10-26T12:00"))
        assertEquals(setOf("2026-10-20", "2026-10-21", "2026-10-22", "2026-10-25", "2026-10-26").map(::day).toSet(), days)
    }

    @Test
    fun `an interval ending exactly at midnight does not mark the next day`() = runTest {
        coverage.recordCovered(TrackingSource.USAGE_STATS, at("2026-10-23T00:00"), at("2026-10-24T00:00"))
        val days = coverage.trackedDays(TrackingSource.USAGE_STATS, day("2026-10-20"), day("2026-10-26"), zone, now = at("2026-10-26T12:00"))
        assertEquals(setOf(day("2026-10-23")), days)
    }

    @Test
    fun `repeated usage imports extend one coverage interval`() = runTest {
        coverage.recordCovered(TrackingSource.USAGE_STATS, at("2026-10-23T00:00"), at("2026-10-24T10:00"))
        coverage.recordCovered(TrackingSource.USAGE_STATS, at("2026-10-23T00:00"), at("2026-10-24T18:00"))
        coverage.recordCovered(TrackingSource.USAGE_STATS, at("2026-10-24T00:00"), at("2026-10-25T09:00"))

        val intervals = database.trackingCoverageDao().overlapping(TrackingSource.USAGE_STATS, 0, Long.MAX_VALUE)
        assertEquals(1, intervals.size)
        assertEquals(at("2026-10-25T09:00").toEpochMilli(), intervals.single().endUtcMs)
    }

    @Test
    fun `an abandoned interval ends at the last reel recorded during it`() = runTest {
        coverage.start(TrackingSource.ACCESSIBILITY, at("2026-10-24T09:00"))
        reel("2026-10-24T09:30", "x")
        reel("2026-10-24T11:15", "y")

        coverage.closeAbandoned(TrackingSource.ACCESSIBILITY)

        val interval = database.trackingCoverageDao().overlapping(TrackingSource.ACCESSIBILITY, 0, Long.MAX_VALUE).single()
        assertEquals(at("2026-10-24T11:15").toEpochMilli(), interval.endUtcMs)
        assertEquals(CoverageEndReason.SERVICE_INTERRUPTED, interval.endReason)
    }

    @Test
    fun `limit history is returned oldest first`() = runTest {
        limits.setLimit(LimitMetric.INSTAGRAM_REELS, 20, day("2026-10-20"))
        limits.setLimit(LimitMetric.INSTAGRAM_REELS, 40, day("2026-10-01"))
        assertEquals(listOf(day("2026-10-01"), day("2026-10-20")), limits.history(LimitMetric.INSTAGRAM_REELS).map { it.effectiveFrom })
    }
}
