package com.example.flix360.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flix360.data.local.entity.PendingScanEntity

@Dao
interface PendingScanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PendingScanEntity)

    @Query("SELECT * FROM pending_scans ORDER BY createdAt DESC")
    suspend fun getAll(): List<PendingScanEntity>

    @Query("DELETE FROM pending_scans WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pending_scans")
    suspend fun clearAll()
}