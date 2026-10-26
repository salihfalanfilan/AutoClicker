package com.kinetic.autoclicker.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kinetic.autoclicker.data.local.db.dao.ScriptProfileDao
import com.kinetic.autoclicker.data.local.db.entity.ScriptProfileEntity
import com.kinetic.autoclicker.data.local.db.entity.TargetPointEntity

@Database(
    entities = [
        ScriptProfileEntity::class,
        TargetPointEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AutoClickerDatabase : RoomDatabase() {
    abstract fun scriptProfileDao(): ScriptProfileDao
}
