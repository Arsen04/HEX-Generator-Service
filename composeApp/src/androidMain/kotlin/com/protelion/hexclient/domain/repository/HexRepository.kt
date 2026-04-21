package com.protelion.hexclient.domain.repository

import com.protelion.hexclient.domain.model.HexCode
import kotlinx.coroutines.flow.Flow

interface HexRepository {
    fun getLatestCodes(limit: Int): Flow<List<HexCode>>
    fun getTotalCount(): Flow<Int>
    suspend fun getAllCodesList(): List<HexCode>
    suspend fun insertHex(value: String)
    suspend fun deleteCode(id: Long)
    suspend fun deleteAll()
}
