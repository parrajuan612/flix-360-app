package com.example.flix360.data.repo

import android.content.Context
import com.example.flix360.core.RetrofitClient
import com.example.flix360.data.local.FlixDatabase
import com.example.flix360.data.local.entity.ProductEntity

class ProductRepository(private val context: Context) {
    private val db by lazy { FlixDatabase.getInstance(context) }

    suspend fun getProducts(): List<ProductEntity> {
        return try {
            val resp = RetrofitClient.api.getProducts()
            if (resp.isSuccessful) {
                val list = resp.body()?.data.orEmpty().map { ProductEntity(it.id, it.name, it.sku) }
                db.productDao().clearAll(); db.productDao().insertAll(list)
                list
            } else {
                db.productDao().getAll()
            }
        } catch (_: Exception) {
            db.productDao().getAll()
        }
    }
}