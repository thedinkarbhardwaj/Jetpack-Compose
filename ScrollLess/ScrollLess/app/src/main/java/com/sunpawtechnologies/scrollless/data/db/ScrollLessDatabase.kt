package com.sunpawtechnologies.scrollless.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sunpawtechnologies.scrollless.data.db.dao.ContentViewDao
import com.sunpawtechnologies.scrollless.data.db.dao.LimitDao
import com.sunpawtechnologies.scrollless.data.db.dao.TrackingCoverageDao
import com.sunpawtechnologies.scrollless.data.db.dao.UsageSessionDao
import com.sunpawtechnologies.scrollless.data.db.dao.WarningDao
import com.sunpawtechnologies.scrollless.data.db.entity.ContentViewEventEntity
import com.sunpawtechnologies.scrollless.data.db.entity.LimitHistoryEntity
import com.sunpawtechnologies.scrollless.data.db.entity.TrackingCoverageEntity
import com.sunpawtechnologies.scrollless.data.db.entity.UsageSessionEntity
import com.sunpawtechnologies.scrollless.data.db.entity.WarningFiredEntity

@Database(
    entities = [
        ContentViewEventEntity::class,
        UsageSessionEntity::class,
        TrackingCoverageEntity::class,
        LimitHistoryEntity::class,
        WarningFiredEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ScrollLessDatabase : RoomDatabase() {
    abstract fun contentViewDao(): ContentViewDao
    abstract fun usageSessionDao(): UsageSessionDao
    abstract fun trackingCoverageDao(): TrackingCoverageDao
    abstract fun limitDao(): LimitDao
    abstract fun warningDao(): WarningDao

    companion object {
        const val NAME = "scrollless.db"

        // No destructive-migration fallback: losing the user's history silently is worse than a crash in testing.
        fun build(context: Context): ScrollLessDatabase =
            Room.databaseBuilder(context.applicationContext, ScrollLessDatabase::class.java, NAME).build()
    }
}
