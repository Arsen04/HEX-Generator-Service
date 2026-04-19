package com.protelion.hexclient.data.local.dao

import androidx.room.*
import com.protelion.hexclient.data.local.entity.HexEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HexDao {
    @Query("SELECT * FROM hex_history ORDER BY timestamp DESC LIMIT 100")
    fun getHistory(): Flow<List<HexEntity>>

    @Query("SELECT * FROM hex_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLastN(limit: Int): List<HexEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hex: HexEntity)

    @Delete
    suspend fun delete(hex: HexEntity)

    @Query("DELETE FROM hex_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM hex_history")
    suspend fun clearAll()
}
