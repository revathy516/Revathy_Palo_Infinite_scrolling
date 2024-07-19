// NetworkModule.kt
package com.example.imageGallery.network

import android.content.Context
import coil.ImageLoader
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

object NetworkModule {

    private const val CACHE_SIZE = 10L * 1024L * 1024L // 10MB

    private fun provideOkHttpClient(context: Context): OkHttpClient {
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, CACHE_SIZE)

        return OkHttpClient.Builder()
            .cache(cache)
            .build()
    }

    fun provideImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .okHttpClient { provideOkHttpClient(context) }
            .build()
    }
}
