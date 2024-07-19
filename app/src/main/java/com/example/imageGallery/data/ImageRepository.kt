// ImageRepository.kt
package com.example.imageGallery.data

import com.example.imageGallery.ui.ImageItem
import com.squareup.moshi.Moshi
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface ImageApiService {
    @GET("v2/list")
    suspend fun getImages(
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): List<ImageItem>
}

class ImageRepository {

    private val api: ImageApiService = Retrofit.Builder()
        .baseUrl("https://picsum.photos/")
        .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
        .build()
        .create(ImageApiService::class.java)

    suspend fun getImages(page: Int, pageSize: Int): List<ImageItem> {
        return api.getImages(page = page, limit = pageSize)
    }
}
