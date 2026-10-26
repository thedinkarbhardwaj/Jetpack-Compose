package com.sunpawtechnologies.scrollless.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.sunpawtechnologies.scrollless.data.db.entity.WarningFiredEntity

@Dao
interface WarningDao {
    /** Returns -1 if this warning already fired today, so callers can skip a repeat notification. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(warning: WarningFiredEntity): Long
}
