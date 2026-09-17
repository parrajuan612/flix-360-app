package com.example.flix360.data.remote

import com.example.flix360.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface FlixApiService {
    @POST("login")
    suspend fun login(@Body payload: LoginRequest): Response<LoginResponse>

    @GET("locations")
    suspend fun getLocations(): Response<PaginatedResponse<LocationDto>>

    @GET("inventory-assets")
    suspend fun getInventoryAssets(@Query("location_id") locationId: String?): Response<PaginatedResponse<InventoryAssetDto>>

    @GET("products")
    suspend fun getProducts(): Response<PaginatedResponse<ProductDto>>

    @GET("categories")
    suspend fun getCategories(): Response<PaginatedResponse<CategoryDto>>

    @POST("rfid-tags")
    suspend fun createRfidTag(@Body body: RfidTagRequest): Response<RfidTagDto>

    @POST("inventory-assets")
    suspend fun createInventoryAsset(@Body body: AssetRequest): Response<InventoryAssetDto>
}