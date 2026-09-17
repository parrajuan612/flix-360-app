package com.example.flix360.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RfidTagRequest(
    @SerializedName("epc") val epc: String,
    @SerializedName("status") val status: String = "assigned"
)