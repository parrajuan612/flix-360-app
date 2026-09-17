package com.example.flix360.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flix360.data.local.entity.LocationEntity

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations ORDER BY name")
    suspend fun getAll(): List<LocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LocationEntity>)

    @Query("DELETE FROM locations")
    suspend fun clearAll()
}