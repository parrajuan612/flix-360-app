package com.example.flix360.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: String? = null
)