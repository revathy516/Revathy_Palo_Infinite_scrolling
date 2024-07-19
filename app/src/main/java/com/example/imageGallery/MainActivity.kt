package com.example.imageGallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import com.example.imageGallery.ui.ImageGalleryScreen
import com.example.imageGallery.ui.theme.InfiniteGalleryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InfiniteGalleryTheme {
                Surface(color = MaterialTheme.colors.background) {
                    ImageGalleryScreen()
                }
            }
        }
    }
}
