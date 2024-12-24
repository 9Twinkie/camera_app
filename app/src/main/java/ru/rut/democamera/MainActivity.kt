package ru.rut.democamera

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import ru.rut.democamera.databinding.ActivityMainBinding
import ru.rut.democamera.utils.PermissionsUtil
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), NavBarFragment.NavBarListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraSelector: CameraSelector
    private var imageCapture: ImageCapture? = null
    private lateinit var imageCaptureExecutor: ExecutorService

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            if (PermissionsUtil.arePermissionsGranted(this, PermissionsUtil.PHOTO_PERMISSIONS)) {
                setupCameraProvider()
            } else {
                showPermissionDeniedDialog()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        imageCaptureExecutor = Executors.newSingleThreadExecutor()

        checkAndRequestCameraPermission()

        supportFragmentManager.beginTransaction()
            .replace(binding.navbarContainer.id, NavBarFragment(this, R.id.photoBtn))
            .commit()

        binding.captureButton.setOnClickListener {
            if (PermissionsUtil.arePermissionsGranted(this, PermissionsUtil.PHOTO_PERMISSIONS)) {
                takePhoto()
                animateFlash()
            } else {
                checkAndRequestCameraPermission()
            }
        }

        binding.switchBtn.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            setupCameraProvider()
        }
    }

    private fun checkAndRequestCameraPermission() {
        val missingPermissions = PermissionsUtil.getMissingPermissions(this, PermissionsUtil.PHOTO_PERMISSIONS)
        if (missingPermissions.isNotEmpty()) {
            PermissionsUtil.showRationaleDialog(
                this,
                "Camera access is required to take photos. Please grant the permission."
            ) {
                requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
            }
        } else {
            setupCameraProvider()
        }
    }

    private fun showPermissionDeniedDialog() {
        PermissionsUtil.showRationaleDialog(
            this,
            "Camera access is required to take photos. Please enable it in the app settings."
        ) {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    private fun setupCameraProvider() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                startCamera()
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to get ProcessCameraProvider", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startCamera() {
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = binding.preview.surfaceProvider
        }

        imageCapture = ImageCapture.Builder().build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        } catch (e: Exception) {
            Log.e("MainActivity", "Use case binding failed", e)
        }
    }

    private fun takePhoto() {
        imageCapture?.let {
            val fileName = "JPEG_${System.currentTimeMillis()}.jpg"
            val file = File(externalMediaDirs[0], fileName)
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
            it.takePicture(
                outputFileOptions,
                imageCaptureExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Log.i("MainActivity", "Image saved at ${file.absolutePath}")
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Photo saved!", Toast.LENGTH_SHORT).show()
                        }
                    }


                    override fun onError(exception: ImageCaptureException) {
                        Log.e("MainActivity", "Error taking photo: ${exception.message}")
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Error taking photo.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }

    private fun animateFlash() {
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }

    override fun onDestroy() {
        super.onDestroy()
        imageCaptureExecutor.shutdown()
    }

    override fun onGallerySelected() {
        val intent = Intent(this, GalleryActivity::class.java)
        startActivity(intent)
    }

    override fun onPhotoModeSelected() {
    }

    override fun onVideoModeSelected() {
        val intent = Intent(this, VideoActivity::class.java)
        startActivity(intent)
    }
}
