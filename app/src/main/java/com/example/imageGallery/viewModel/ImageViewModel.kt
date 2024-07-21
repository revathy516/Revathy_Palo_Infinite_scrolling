package com.example.imageGallery.viewModel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imageGallery.data.ApiService
import com.example.imageGallery.network.RetrofitInstance
import com.example.imageGallery.ui.ImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL

/**
 * ViewModel class to do logic for displaying the items , setting the pageSize by user and save, share functionality.
 */
class ImageViewModel : ViewModel() {

    private val _images = MutableStateFlow<List<ImageItem>>(emptyList())
    val images: StateFlow<List<ImageItem>> get() = _images

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> get() = _isRefreshing

    private val _requestPermission = MutableLiveData(false)
    val requestPermission: LiveData<Boolean> get() = _requestPermission

    var pageSize = 20
        private set
    private var currentPage = 1
    private var hasMoreData = true
    private var isSetPageCalled= false

    private val apiService = RetrofitInstance.getRetrofitInstance().create(ApiService::class.java)

     var pendingImageItem: ImageItem? = null

    init {
        loadImages()
    }

    fun setPageSize(size: Int) {
        pageSize = size
        currentPage = 1
        hasMoreData = true
        isSetPageCalled = true
        _images.value = _images.value.take(pageSize)
        loadImages()
    }

    fun loadImages() {
        if (!hasMoreData) return
        viewModelScope.launch {
            if(!isSetPageCalled){
            _isLoading.value = true
            }
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
                isSetPageCalled= false
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
        viewModelScope.launch {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Request permission if not granted
                return@launch
            }

            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "saved_image.jpg")
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeStream(URL(image.download_url).openStream())
                }
                withContext(Dispatchers.IO) {
                    FileOutputStream(file).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    }
                }
                Toast.makeText(context, "Successfully Saved", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                // Handle error, e.g., show Toast
                Toast.makeText(context, "Failed to share image", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    fun shareImage(image: ImageItem, context: Context) {
        viewModelScope.launch {
            // Check for permissions
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Handle permission request here
                // You can show a message to the user or request permission
                return@launch
            }

            // Perform network operation in a background thread
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeStream(URL(image.download_url).openStream())
                }

                val file = File(context.cacheDir, "image_to_share.jpg")
                withContext(Dispatchers.IO) {
                    FileOutputStream(file).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    }
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
                e.printStackTrace()
                Toast.makeText(context, "Failed to share image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handlePermissionResult(context: Context, isGranted: Boolean) {
        if (isGranted) {
            pendingImageItem?.let {
                saveImage(it, context)
            }
        } else {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
        pendingImageItem = null
        _requestPermission.value = false
    }
}
