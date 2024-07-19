package com.example.imageGallery.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.imageGallery.R
import com.example.imageGallery.viewModel.ImageViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL

@Composable
fun ImageGalleryScreen(viewModel: ImageViewModel = viewModel()) {
    val context = LocalContext.current
    val images by viewModel.images.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var pageSize by remember { mutableStateOf(20) }
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // Launcher for permission request
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.handlePermissionResult(context, isGranted)
    }

    Surface(color = MaterialTheme.colors.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Page size input field and button
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextField(
                    value = pageSize.toString(),
                    onValueChange = { newSize ->
                        pageSize = newSize.toIntOrNull() ?: 20
                    },
                    label = { Text("Page Size") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { viewModel.setPageSize(pageSize) }) {
                    Text("Set Page Size")
                }
            }

            // Error handling
            if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = errorMessage!!, color = MaterialTheme.colors.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadImages() }) {
                            Text(text = "Retry")
                        }
                    }
                }
            } else {
                // Pull-to-refresh
                val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)
                SwipeRefresh(
                    state = swipeRefreshState,
                    onRefresh = { viewModel.refreshImages() }
                ) {
                    if (images.isEmpty() && isLoading) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            items(images.size) { index ->
                                if (index == images.size - 1 && !isLoading) {
                                    viewModel.loadImages()
                                }
                                ImageItem(image = images[index], viewModel = viewModel, context = context, permissionLauncher = permissionLauncher)
                            }

                            if (isLoading && images.isNotEmpty()) {
                                item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageItem(image: ImageItem, viewModel: ImageViewModel, context: Context, permissionLauncher: ManagedActivityResultLauncher<String, Boolean>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = image.author,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        AsyncImage(
            model = image.download_url,
            contentDescription = image.author,
            placeholder = painterResource(R.drawable.ic_placeholder),
            error = painterResource(R.drawable.ic_error_placeholder),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(R.drawable.ic_save),
                contentDescription = "Save",
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            viewModel.pendingImageItem = image
                            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            viewModel.saveImage(image, context)
                        }
                    }
            )
            Image(
                painter = painterResource(R.drawable.ic_share),
                contentDescription = "Share",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { viewModel.shareImage(image, context) }
            )
        }
    }
}

private fun shareImage(image: ImageItem, context: Context) {
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
        e.printStackTrace()
        Toast.makeText(context, "Failed to share image", Toast.LENGTH_SHORT).show()
    }
}
