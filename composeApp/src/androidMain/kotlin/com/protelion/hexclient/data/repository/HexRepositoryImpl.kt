package com.protelion.hexclient.data.repository

import com.protelion.hexclient.data.local.dao.HexDao
import com.protelion.hexclient.data.local.entity.HexEntity
import com.protelion.hexclient.domain.model.HexCode
import com.protelion.hexclient.domain.repository.HexRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HexRepositoryImpl(private val hexDao: HexDao) : HexRepository {
    override fun getLocalHistory(): Flow<List<HexCode>> =
        hexDao.getHistory().map { list ->
            list.map { HexCode(it.id, it.value, it.timestamp) }
        }

    override suspend fun saveCode(hex: HexCode) {
        hexDao.insert(HexEntity(value = hex.value, timestamp = hex.timestamp))
    }

    override suspend fun deleteCode(id: Long) = hexDao.deleteById(id)

    override suspend fun clearAll() = hexDao.clearAll()
}
