package com.example.flix360.data.repo

import android.content.Context
import com.example.flix360.core.RetrofitClient
import com.example.flix360.data.local.FlixDatabase
import com.example.flix360.data.local.entity.LocationEntity

class LocationRepository(private val context: Context) {
    private val db by lazy { FlixDatabase.getInstance(context) }

    suspend fun getLocations(): List<LocationEntity> {
        return try {
            val resp = RetrofitClient.api.getLocations()
            if (resp.isSuccessful) {
                val list = resp.body()?.data.orEmpty().map { LocationEntity(it.id, it.name, it.code) }
                db.locationDao().clearAll(); db.locationDao().insertAll(list)
                list
            } else {
                db.locationDao().getAll()
            }
        } catch (_: Exception) {
            db.locationDao().getAll()
        }
    }
}