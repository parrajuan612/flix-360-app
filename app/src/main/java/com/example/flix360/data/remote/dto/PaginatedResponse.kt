package com.example.flix360.data.remote.dto

data class PaginatedResponse<T>(
    val total: Long,
    val limit: Int,
    val offset: Int,
    val data: List<T>
)