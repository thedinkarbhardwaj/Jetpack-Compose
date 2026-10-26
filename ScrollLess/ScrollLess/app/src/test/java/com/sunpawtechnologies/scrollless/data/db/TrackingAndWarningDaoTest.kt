package com.sunpawtechnologies.scrollless.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sunpawtechnologies.scrollless.data.db.entity.TrackingCoverageEntity
import com.sunpawtechnologies.scrollless.data.db.entity.WarningFiredEntity
import com.sunpawtechnologies.scrollless.data.repository.RoomAlertLog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.time.Instant
import com.sunpawtechnologies.scrollless.domain.model.CoverageEndReason
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.model.TrackingSource
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrackingAndWarningDaoTest {

    private lateinit var database: ScrollLessDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ScrollLessDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `a warning threshold fires only once per day`() = runTest {
        val dao = database.warningDao()
        val warning = WarningFiredEntity(localEpochDay = 20_385, metric = LimitMetric.INSTAGRAM_REELS, thresholdPercent = 80, firedAtUtcMs = 1)

        assertNotEquals(-1L, dao.insert(warning))
        assertEquals(-1L, dao.insert(warning.copy(firedAtUtcMs = 2)))
        assertNotEquals(-1L, dao.insert(warning.copy(thresholdPercent = 100)))
        assertNotEquals(-1L, dao.insert(warning.copy(localEpochDay = 20_386)))
    }

    @Test
    fun `alert log reports each app-time alert as new exactly once per day`() = runTest {
        val log = RoomAlertLog(database.warningDao())
        val at = Instant.parse("2026-10-24T06:30:00Z")

        assertTrue(log.markFired(20_385, LimitMetric.INSTAGRAM_APP_TIME, 80, at))
        assertFalse("duplicate", log.markFired(20_385, LimitMetric.INSTAGRAM_APP_TIME, 80, at.plusSeconds(900)))
        assertTrue("reached is separate from warning", log.markFired(20_385, LimitMetric.INSTAGRAM_APP_TIME, 100, at))
        assertTrue("other app", log.markFired(20_385, LimitMetric.YOUTUBE_APP_TIME, 80, at))
        assertTrue("next day", log.markFired(20_386, LimitMetric.INSTAGRAM_APP_TIME, 80, at))
    }

    @Test
    fun `open and closed coverage intervals are found by overlap`() = runTest {
        val dao = database.trackingCoverageDao()
        val closedId = dao.insert(TrackingCoverageEntity(source = TrackingSource.ACCESSIBILITY, startUtcMs = 100))
        dao.close(closedId, endUtcMs = 200, reason = CoverageEndReason.SERVICE_INTERRUPTED)
        dao.insert(TrackingCoverageEntity(source = TrackingSource.ACCESSIBILITY, startUtcMs = 300))

        assertEquals(listOf(100L), dao.overlapping(TrackingSource.ACCESSIBILITY, 150, 250).map { it.startUtcMs })
        assertEquals(emptyList<Long>(), dao.overlapping(TrackingSource.ACCESSIBILITY, 200, 300).map { it.startUtcMs })
        assertEquals(listOf(300L), dao.overlapping(TrackingSource.ACCESSIBILITY, 1_000, 2_000).map { it.startUtcMs })
        assertEquals(emptyList<Long>(), dao.overlapping(TrackingSource.USAGE_STATS, 0, 2_000).map { it.startUtcMs })
    }

    @Test
    fun `closing an interval twice keeps the first end`() = runTest {
        val dao = database.trackingCoverageDao()
        val id = dao.insert(TrackingCoverageEntity(source = TrackingSource.USAGE_STATS, startUtcMs = 100))
        dao.close(id, endUtcMs = 200, reason = CoverageEndReason.PERMISSION_REVOKED)
        dao.close(id, endUtcMs = 900, reason = CoverageEndReason.STOPPED_BY_USER)

        val interval = dao.overlapping(TrackingSource.USAGE_STATS, 0, 1_000).single()
        assertEquals(200L, interval.endUtcMs)
        assertEquals(CoverageEndReason.PERMISSION_REVOKED, interval.endReason)
    }

    @Test
    fun `new coverage interval starts open`() = runTest {
        val dao = database.trackingCoverageDao()
        dao.insert(TrackingCoverageEntity(source = TrackingSource.USAGE_STATS, startUtcMs = 100))
        assertNull(dao.overlapping(TrackingSource.USAGE_STATS, 0, 1_000).single().endUtcMs)
    }
}
