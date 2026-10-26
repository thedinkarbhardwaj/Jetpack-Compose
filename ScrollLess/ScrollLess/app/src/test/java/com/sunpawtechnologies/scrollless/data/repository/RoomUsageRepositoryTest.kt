package com.sunpawtechnologies.scrollless.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sunpawtechnologies.scrollless.data.db.ScrollLessDatabase
import com.sunpawtechnologies.scrollless.domain.model.ContentKind
import com.sunpawtechnologies.scrollless.domain.model.Platform
import com.sunpawtechnologies.scrollless.domain.model.TrackingSource
import com.sunpawtechnologies.scrollless.domain.model.UsageMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class RoomUsageRepositoryTest {

    private val kolkata = ZoneId.of("Asia/Kolkata")
    private lateinit var database: ScrollLessDatabase
    private lateinit var repository: RoomUsageRepository

    private fun at(local: String, zone: ZoneId = kolkata): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()
    private fun day(text: String) = LocalDate.parse(text)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ScrollLessDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomUsageRepository(database.contentViewDao(), database.usageSessionDao())
    }

    @After
    fun tearDown() = database.close()

    private suspend fun recordReel(local: String, key: String, zone: ZoneId = kolkata) =
        repository.recordContentView(ContentKind.INSTAGRAM_REEL, at(local, zone), zone, key, detectorVersion = 1)

    @Test
    fun `duplicate view events are counted once`() = runTest {
        assertTrue(recordReel("2026-10-24T10:00", key = "reel-1"))
        assertFalse(recordReel("2026-10-24T10:00", key = "reel-1"))
        assertTrue(recordReel("2026-10-24T10:01", key = "reel-2"))

        assertEquals(2, repository.observeContentViewCount(ContentKind.INSTAGRAM_REEL, day("2026-10-24")).first())
    }

    @Test
    fun `views count toward the local day they happened on`() = runTest {
        recordReel("2026-10-24T23:59", key = "late")
        recordReel("2026-10-25T00:01", key = "early")

        assertEquals(1, repository.observeContentViewCount(ContentKind.INSTAGRAM_REEL, day("2026-10-24")).first())
        assertEquals(1, repository.observeContentViewCount(ContentKind.INSTAGRAM_REEL, day("2026-10-25")).first())
    }

    @Test
    fun `a view keeps its original local day after the user changes time zone`() = runTest {
        // 01:30 on Oct 25 in India is still Oct 24 in UTC; the event stays on the day the user lived.
        recordReel("2026-10-25T01:30", key = "travel", zone = kolkata)
        recordReel("2026-10-24T22:00", key = "abroad", zone = ZoneOffset.UTC)

        assertEquals(1, repository.observeContentViewCount(ContentKind.INSTAGRAM_REEL, day("2026-10-25")).first())
        assertEquals(1, repository.observeContentViewCount(ContentKind.INSTAGRAM_REEL, day("2026-10-24")).first())
    }

    @Test
    fun `reels and shorts are counted separately`() = runTest {
        recordReel("2026-10-24T10:00", key = "reel")
        repository.recordContentView(ContentKind.YOUTUBE_SHORT, at("2026-10-24T10:05"), kolkata, "short", 1)

        assertEquals(1, repository.observeContentViewCount(ContentKind.YOUTUBE_SHORT, day("2026-10-24")).first())
        assertEquals(1, repository.observeContentViewCount(ContentKind.INSTAGRAM_REEL, day("2026-10-24")).first())
    }

    @Test
    fun `session crossing midnight is split between days`() = runTest {
        repository.recordSession(
            Platform.YOUTUBE, UsageMode.YOUTUBE_WATCH, TrackingSource.ACCESSIBILITY,
            at("2026-10-24T23:40"), at("2026-10-25T00:25"), kolkata,
        )

        assertEquals(Duration.ofMinutes(20), watchTimeOn("2026-10-24"))
        assertEquals(Duration.ofMinutes(25), watchTimeOn("2026-10-25"))
    }

    @Test
    fun `re-recording a session that grew replaces it instead of double counting`() = runTest {
        val start = at("2026-10-24T09:00")
        repository.recordSession(Platform.YOUTUBE, UsageMode.YOUTUBE_WATCH, TrackingSource.ACCESSIBILITY, start, at("2026-10-24T09:10"), kolkata)
        repository.recordSession(Platform.YOUTUBE, UsageMode.YOUTUBE_WATCH, TrackingSource.ACCESSIBILITY, start, at("2026-10-24T09:30"), kolkata)

        assertEquals(Duration.ofMinutes(30), watchTimeOn("2026-10-24"))
    }

    @Test
    fun `separate sessions add up and other modes are excluded`() = runTest {
        repository.recordSession(Platform.YOUTUBE, UsageMode.YOUTUBE_WATCH, TrackingSource.ACCESSIBILITY, at("2026-10-24T09:00"), at("2026-10-24T09:10"), kolkata)
        repository.recordSession(Platform.YOUTUBE, UsageMode.YOUTUBE_WATCH, TrackingSource.ACCESSIBILITY, at("2026-10-24T18:00"), at("2026-10-24T18:05"), kolkata)
        repository.recordSession(Platform.YOUTUBE, UsageMode.SHORTS, TrackingSource.ACCESSIBILITY, at("2026-10-24T19:00"), at("2026-10-24T19:30"), kolkata)

        assertEquals(Duration.ofMinutes(15), watchTimeOn("2026-10-24"))
    }

    @Test
    fun `day with no sessions reports zero duration`() = runTest {
        // Whether the day was tracked at all is decided from tracking coverage, not from this sum.
        assertEquals(Duration.ZERO, watchTimeOn("2026-10-24"))
    }

    private suspend fun watchTimeOn(date: String): Duration =
        repository.observeUsageDuration(Platform.YOUTUBE, UsageMode.YOUTUBE_WATCH, day(date)).first()
}
