package com.sunpawtechnologies.scrollless.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sunpawtechnologies.scrollless.data.db.entity.LimitHistoryEntity
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import kotlinx.coroutines.flow.Flow

@Dao
interface LimitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(limit: LimitHistoryEntity)

    /** The value in effect on [epochDay], or null if the metric has never been configured by then. */
    @Query(
        """
        SELECT limit_value FROM limit_history
        WHERE metric = :metric AND effective_from_epoch_day <= :epochDay
        ORDER BY effective_from_epoch_day DESC LIMIT 1
        """,
    )
    fun observeLimitOn(metric: LimitMetric, epochDay: Long): Flow<Int?>

    @Query("SELECT * FROM limit_history WHERE metric = :metric ORDER BY effective_from_epoch_day")
    suspend fun history(metric: LimitMetric): List<LimitHistoryEntity>
}
