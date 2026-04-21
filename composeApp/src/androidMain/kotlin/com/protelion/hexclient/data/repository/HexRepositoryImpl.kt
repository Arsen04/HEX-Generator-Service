package com.protelion.hexclient.data.repository

import com.protelion.hexclient.data.local.dao.HexDao
import com.protelion.hexclient.domain.model.HexCode
import com.protelion.hexclient.domain.repository.HexRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HexRepositoryImpl(private val hexDao: HexDao) : HexRepository {
    override fun getLatestCodes(limit: Int): Flow<List<HexCode>> = 
        hexDao.getHistory().map { list -> list.take(limit).map { it.toDomain() } }

    override fun getTotalCount(): Flow<Int> = 
        hexDao.getCount()

    override suspend fun getAllCodesList(): List<HexCode> = 
        hexDao.getLastN(Int.MAX_VALUE).map { it.toDomain() }

    override suspend fun insertHex(value: String) {
        hexDao.insertHex(value)
    }

    override suspend fun deleteCode(id: Long) {
        hexDao.deleteById(id)
    }

    override suspend fun deleteAll() {
        hexDao.clearAll()
    }
}
