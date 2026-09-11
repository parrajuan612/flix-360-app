package com.example.flix360.data.remote.dto

import com.google.gson.annotations.SerializedName

data class InventoryAssetDto(
    val id: String,
    @SerializedName("company_id") val companyId: String,
    @SerializedName("product_id") val productId: String,
    @SerializedName("rfid_tag_id") val rfidTagId: String?,
    @SerializedName("location_id") val locationId: String?,
    val quantity: Double,
    @SerializedName("inventory_mode") val inventoryMode: String,
    val status: String
)