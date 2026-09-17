package com.example.flix360.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.flix360.data.local.dao.LocationDao
import com.example.flix360.data.local.dao.PendingScanDao
import com.example.flix360.data.local.dao.ProductDao
import com.example.flix360.data.local.entity.LocationEntity
import com.example.flix360.data.local.entity.PendingScanEntity
import com.example.flix360.data.local.entity.ProductEntity

@Database(
    entities = [ProductEntity::class, LocationEntity::class, PendingScanEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FlixDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun locationDao(): LocationDao
    abstract fun pendingScanDao(): PendingScanDao

    companion object {
        @Volatile private var INSTANCE: FlixDatabase? = null
        fun getInstance(context: Context): FlixDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                FlixDatabase::class.java,
                "flix360.db"
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}