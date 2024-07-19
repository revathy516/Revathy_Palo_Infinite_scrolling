// MyApplication.kt
package com.example.imageGallery

import android.app.Application
import coil.Coil
import com.example.imageGallery.network.NetworkModule


class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val imageLoader = NetworkModule.provideImageLoader(this)
        Coil.setImageLoader(imageLoader)
    }
}
