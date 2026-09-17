package com.example.flix360.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flix360.data.local.entity.ProductEntity

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name")
    suspend fun getAll(): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ProductEntity>)

    @Query("DELETE FROM products")
    suspend fun clearAll()
}