package com.example.flix360.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AssetRequest(
    @SerializedName("product_id") val productId: String,
    @SerializedName("location_id") val locationId: String,
    @SerializedName("rfid_tag_id") val rfidTagId: String,
    @SerializedName("quantity") val quantity: Double = 1.0,
    @SerializedName("inventory_mode") val inventoryMode: String = "unit"
)