package com.sunpawtechnologies.scrollless.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunpawtechnologies.scrollless.data.db.entity.ContentViewEventEntity
import com.sunpawtechnologies.scrollless.domain.model.ContentKind
import kotlinx.coroutines.flow.Flow

/** One aggregated row: a group key (local epoch day or hour) and its total. */
data class KeyedTotal(
    @ColumnInfo(name = "group_key") val key: Long,
    @ColumnInfo(name = "total") val total: Long,
)

@Dao
interface ContentViewDao {
    /** Returns the new row id, or -1 if an event with the same dedup key already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: ContentViewEventEntity): Long

    @Query("SELECT COUNT(*) FROM content_view_event WHERE kind = :kind AND local_epoch_day = :epochDay")
    fun observeCountOnDay(kind: ContentKind, epochDay: Long): Flow<Int>

    @Query(
        """
        SELECT local_epoch_day AS group_key, COUNT(*) AS total FROM content_view_event
        WHERE kind = :kind AND local_epoch_day BETWEEN :fromEpochDay AND :toEpochDay
        GROUP BY local_epoch_day
        """,
    )
    suspend fun countsByDay(kind: ContentKind, fromEpochDay: Long, toEpochDay: Long): List<KeyedTotal>

    @Query(
        """
        SELECT local_hour AS group_key, COUNT(*) AS total FROM content_view_event
        WHERE kind = :kind AND local_epoch_day = :epochDay
        GROUP BY local_hour
        """,
    )
    suspend fun countsByHour(kind: ContentKind, epochDay: Long): List<KeyedTotal>
}
