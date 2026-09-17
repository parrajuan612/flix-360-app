package com.example.flix360.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_scans")
data class PendingScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: String,
    val locationId: String,
    val epcsJson: String,
    val createdAt: Long = System.currentTimeMillis()
)