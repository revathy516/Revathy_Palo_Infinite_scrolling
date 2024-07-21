package com.example.imageGallery


import com.example.imageGallery.ui.theme.InfiniteGalleryTheme
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.core.content.ContextCompat
import com.example.imageGallery.ui.ImageGalleryScreen
import com.example.imageGallery.viewModel.ImageViewModel

class MainActivity : ComponentActivity() {

    private val imageViewModel: ImageViewModel by viewModels()

    // Register the permissions callback, which handles the user's response to the system permissions dialog.
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted. Continue the action or workflow in your app.
            imageViewModel.handlePermissionResult(this, isGranted)
        } else {
            // Permission denied, inform the user.
            Toast.makeText(this, "Permission denied. Unable to save image.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Companion.REQUEST_CODE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with the operation
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, "Permission denied. Unable to save or share images.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InfiniteGalleryTheme {
                Surface(color = MaterialTheme.colors.background) {
                    ImageGalleryScreen(viewModel = imageViewModel)
                }
            }
        }

        // Observe for permission requests from the ViewModel
        imageViewModel.requestPermission.observe(this) { permissionNeeded ->
            if (permissionNeeded) {
                requestStoragePermission()
            }
        }
    }

    private fun requestStoragePermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted
                imageViewModel.handlePermissionResult(this, true)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                // Explain why the permission is needed
                Toast.makeText(this, "Storage permission is required to save images.", Toast.LENGTH_SHORT).show()
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            else -> {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSION = 1001
    }
}
