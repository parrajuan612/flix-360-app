package com.example.flix360.data.remote

import com.example.flix360.data.remote.dto.LoginRequest
import com.example.flix360.data.remote.dto.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface FlixApiService {
    @POST("login")
    suspend fun login(@Body payload: LoginRequest): Response<LoginResponse>
}