package com.protelion.hexclient.data.local.dao

import androidx.room.*
import com.protelion.hexclient.data.local.entity.HexEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HexDao {
    @Query("SELECT * FROM hex_history ORDER BY timestamp DESC LIMIT 100")
    fun getHistory(): Flow<List<HexEntity>>

    @Query("SELECT COUNT(*) FROM hex_history")
    fun getCount(): Flow<Int>

    @Query("SELECT * FROM hex_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLastN(limit: Int): List<HexEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hex: HexEntity)

    suspend fun insertHex(value: String) {
        insert(HexEntity(value = value, timestamp = System.currentTimeMillis()))
    }

    @Transaction
    suspend fun insertAndTrim(hex: HexEntity) {
        insert(hex)
        deleteOldRecords(100)
    }

    @Query("DELETE FROM hex_history WHERE id NOT IN (SELECT id FROM hex_history ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun deleteOldRecords(limit: Int)

    @Query("DELETE FROM hex_history WHERE value = :value")
    suspend fun deleteByValue(value: String)

    @Query("DELETE FROM hex_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM hex_history")
    suspend fun clearAll()
}
