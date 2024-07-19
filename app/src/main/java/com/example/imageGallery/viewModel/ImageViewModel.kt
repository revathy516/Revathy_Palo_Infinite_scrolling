package com.example.imageGallery.viewModel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imageGallery.ui.ImageItem
import com.example.imageGallery.data.ApiService
import com.example.imageGallery.network.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL

class ImageViewModel : ViewModel() {

    private val _images = MutableStateFlow<List<ImageItem>>(emptyList())
    val images: StateFlow<List<ImageItem>> get() = _images

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> get() = _isRefreshing

    var pageSize = 20
        private set
    private var currentPage = 1
    private var hasMoreData = true

    private val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)

    init {
        loadImages()
    }

    fun setPageSize(size: Int) {
        pageSize = size
        currentPage = 1
        hasMoreData = true
        _images.value = _images.value.take(pageSize)
        loadImages()
    }

    fun loadImages() {
        if (!hasMoreData) return
        viewModelScope.launch {
           // _isLoading.value = true
            try {
                val response = apiService.fetchImages(page = currentPage, limit = pageSize)
                _images.value = _images.value + response.take(pageSize - _images.value.size)
                currentPage++
                hasMoreData = response.isNotEmpty()
                _errorMessage.value = null
            } catch (e: HttpException) {
                when (e.code()) {
                    400 -> _errorMessage.value = "Bad Request: The server could not understand the request."
                    401 -> _errorMessage.value = "Unauthorized: Access is denied due to invalid credentials."
                    403 -> _errorMessage.value = "Forbidden: You do not have permission to access this resource."
                    404 -> _errorMessage.value = "Not Found: The requested resource could not be found."
                    500 -> _errorMessage.value = "Internal Server Error: The server encountered an error."
                    502 -> _errorMessage.value = "Bad Gateway: The server received an invalid response from the upstream server."
                    503 -> _errorMessage.value = "Service Unavailable: The server is currently unable to handle the request."
                    504 -> _errorMessage.value = "Gateway Timeout: The server took too long to respond."
                    else -> _errorMessage.value = "Error: ${e.message()}"
                }
            } catch (e: IOException) {
                _errorMessage.value = "Network error occurred. Please check your internet connection."
            } catch (e: Exception) {
                _errorMessage.value = "An unknown error occurred."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshImages() {
        currentPage = 1
        hasMoreData = true
        _images.value = emptyList()
        loadImages()
    }

    fun saveImage(image: ImageItem, context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Handle permission request here
            return
        }
        viewModelScope.launch {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "saved_image.jpg")
            try {
                val bitmap = BitmapFactory.decodeStream(URL(image.download_url).openStream())
                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                }
                // Notify user of successful save, e.g., with a Toast
            } catch (e: IOException) {
                // Handle error, e.g., show Toast
            }
        }
    }

    fun shareImage(image: ImageItem, context: Context) {
        viewModelScope.launch {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Handle permission request here
                //return
            }
            val file = File(context.cacheDir, "image_to_share.jpg")
            try {
                val bitmap = BitmapFactory.decodeStream(URL(image.download_url).openStream())
                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "image/jpeg"
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
            } catch (e: IOException) {
                // Handle error, e.g., show Toast
            }
        }
    }
}
