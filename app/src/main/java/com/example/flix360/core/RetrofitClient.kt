package com.example.flix360.core

import com.example.flix360.data.remote.FlixApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Nota la inclusión de "api/v1/" y la barra diagonal "/" al final
    private const val BASE_URL = "http://127.0.0.1:8590/api/v1/"

    // Mutable provider to be attached from app layer (e.g., MainActivity/Application)
    @Volatile
    private var tokenProvider: (() -> String?) = { null }

    fun attachSessionManager(sessionManager: SessionManager) {
        tokenProvider = { sessionManager.getToken() }
    }

    private val logging: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { tokenProvider.invoke() })
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val api: FlixApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FlixApiService::class.java)
    }
}