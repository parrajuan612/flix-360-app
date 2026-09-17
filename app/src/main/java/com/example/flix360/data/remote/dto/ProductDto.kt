package com.example.flix360.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProductDto(
    val id: String,
    @SerializedName("category_id") val categoryId: String?,
    val name: String,
    val sku: String?
)