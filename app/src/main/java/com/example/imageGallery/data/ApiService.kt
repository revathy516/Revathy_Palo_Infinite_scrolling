package com.example.imageGallery.data

import com.example.imageGallery.ui.ImageItem
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("list")
    suspend fun fetchImages(
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): List<ImageItem>
}