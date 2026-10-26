package com.sunpawtechnologies.scrollless.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sunpawtechnologies.scrollless.data.db.ScrollLessDatabase
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class RoomLimitRepositoryTest {

    private lateinit var database: ScrollLessDatabase
    private lateinit var repository: RoomLimitRepository

    private val today = LocalDate.parse("2026-10-24")

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ScrollLessDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomLimitRepository(database.limitDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `unconfigured metrics use their defaults`() = runTest {
        val limits = repository.observeLimitsOn(today).first()
        assertEquals(
            mapOf(
                LimitMetric.INSTAGRAM_REELS to 50,
                LimitMetric.YOUTUBE_SHORTS to 50,
                LimitMetric.YOUTUBE_REGULAR_VIDEO to 60,
                LimitMetric.INSTAGRAM_APP_TIME to 60,
                LimitMetric.YOUTUBE_APP_TIME to 60,
            ),
            limits,
        )
    }

    @Test
    fun `past days keep the limit that applied then`() = runTest {
        repository.setLimit(LimitMetric.INSTAGRAM_REELS, 40, effectiveFrom = today.minusDays(10))
        repository.setLimit(LimitMetric.INSTAGRAM_REELS, 25, effectiveFrom = today)

        assertEquals(50, reelsLimitOn(today.minusDays(11)))
        assertEquals(40, reelsLimitOn(today.minusDays(1)))
        assertEquals(25, reelsLimitOn(today))
        assertEquals(25, reelsLimitOn(today.plusDays(30)))
    }

    @Test
    fun `changing the limit twice on one day keeps the latest value`() = runTest {
        repository.setLimit(LimitMetric.YOUTUBE_SHORTS, 30, effectiveFrom = today)
        repository.setLimit(LimitMetric.YOUTUBE_SHORTS, 20, effectiveFrom = today)

        assertEquals(20, repository.observeLimitsOn(today).first()[LimitMetric.YOUTUBE_SHORTS])
    }

    @Test
    fun `changing one metric does not affect others`() = runTest {
        repository.setLimit(LimitMetric.YOUTUBE_REGULAR_VIDEO, 90, effectiveFrom = today)

        val limits = repository.observeLimitsOn(today).first()
        assertEquals(90, limits[LimitMetric.YOUTUBE_REGULAR_VIDEO])
        assertEquals(50, limits[LimitMetric.INSTAGRAM_REELS])
    }

    @Test(expected = IllegalArgumentException::class)
    fun `non-positive limits are rejected`() = runTest {
        repository.setLimit(LimitMetric.INSTAGRAM_REELS, 0, effectiveFrom = today)
    }

    private suspend fun reelsLimitOn(date: LocalDate): Int? =
        repository.observeLimitsOn(date).first()[LimitMetric.INSTAGRAM_REELS]
}
