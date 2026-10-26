package com.sunpawtechnologies.scrollless.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunpawtechnologies.scrollless.data.db.entity.UsageSessionEntity
import com.sunpawtechnologies.scrollless.domain.model.Platform
import com.sunpawtechnologies.scrollless.domain.model.UsageMode
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(sessions: List<UsageSessionEntity>)

    @Query(
        """
        SELECT COALESCE(SUM(end_utc_ms - start_utc_ms), 0) FROM usage_session
        WHERE platform = :platform AND mode = :mode AND local_epoch_day = :epochDay
        """,
    )
    fun observeTotalMillisOnDay(platform: Platform, mode: UsageMode, epochDay: Long): Flow<Long>

    @Query(
        """
        SELECT local_epoch_day AS group_key, SUM(end_utc_ms - start_utc_ms) AS total FROM usage_session
        WHERE platform = :platform AND mode = :mode AND local_epoch_day BETWEEN :fromEpochDay AND :toEpochDay
        GROUP BY local_epoch_day
        """,
    )
    suspend fun millisByDay(platform: Platform, mode: UsageMode, fromEpochDay: Long, toEpochDay: Long): List<KeyedTotal>

    @Query(
        """
        SELECT * FROM usage_session
        WHERE platform = :platform AND mode = :mode AND local_epoch_day = :epochDay
        ORDER BY start_utc_ms
        """,
    )
    suspend fun sessionsOnDay(platform: Platform, mode: UsageMode, epochDay: Long): List<UsageSessionEntity>
}
