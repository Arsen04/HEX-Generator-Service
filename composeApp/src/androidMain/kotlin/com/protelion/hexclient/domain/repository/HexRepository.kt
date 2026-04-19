package com.protelion.hexclient.domain.repository

import com.protelion.hexclient.domain.model.HexCode
import kotlinx.coroutines.flow.Flow

interface HexRepository {
    fun getLocalHistory(): Flow<List<HexCode>>
    suspend fun saveCode(hex: HexCode)
    suspend fun deleteCode(id: Long)
    suspend fun clearAll()
}
