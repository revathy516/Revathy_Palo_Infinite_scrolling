package com.example.imageGallery.ui

/**
 * Data class item with the response data.
 */
data class ImageItem(
    val id: String,
    val author: String,
    val width: Int,
    val height: Int,
    val url: String,
    val download_url: String
)