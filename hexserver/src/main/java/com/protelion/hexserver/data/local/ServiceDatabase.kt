package com.protelion.hexserver.data.local

import androidx.room.*
import com.protelion.hexserver.data.local.entity.HexEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceHexDao {
    @Query("SELECT * FROM service_hex_history ORDER BY timestamp DESC LIMIT 100")
    fun getHistory(): Flow<List<HexEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hex: HexEntity)

    @Transaction
    suspend fun insertAndTrim(hex: HexEntity) {
        insert(hex)
        deleteOldRecords(100)
    }

    @Query("DELETE FROM service_hex_history WHERE id NOT IN (SELECT id FROM service_hex_history ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun deleteOldRecords(limit: Int)

    @Query("SELECT * FROM service_hex_history ORDER BY timestamp DESC LIMIT 50")
    suspend fun getLast50(): List<HexEntity>
}

@Database(entities = [HexEntity::class], version = 1, exportSchema = false)
abstract class ServiceDatabase : RoomDatabase() {
    abstract fun serviceHexDao(): ServiceHexDao
}
