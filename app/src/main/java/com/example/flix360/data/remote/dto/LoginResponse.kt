package com.example.flix360.data.remote.dto

import com.example.flix360.domain.model.User

data class LoginResponse(
    val token: String,
    val user: User
)