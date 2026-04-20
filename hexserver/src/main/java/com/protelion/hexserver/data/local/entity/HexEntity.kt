package com.protelion.hexserver.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_hex_history")
data class HexEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val value: String,
    val timestamp: Long
)
