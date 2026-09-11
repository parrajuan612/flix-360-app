package com.example.flix360.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LocationDto(
    val id: String,
    @SerializedName("company_id") val companyId: String,
    @SerializedName("parent_id") val parentId: String?,
    val name: String,
    val code: String?,
    val type: String,
    val status: String
)