package com.protelion.hexclient.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.protelion.hexclient.data.local.dao.HexDao
import com.protelion.hexclient.data.local.entity.HexEntity

@Database(entities = [HexEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hexDao(): HexDao
}
