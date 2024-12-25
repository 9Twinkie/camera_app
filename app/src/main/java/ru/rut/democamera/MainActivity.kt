package ru.rut.democamera

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import ru.rut.democamera.databinding.ActivityMainBinding
import ru.rut.democamera.utils.PermissionsUtil
import ru.rut.democamera.utils.DialogUtil
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), NavBarFragment.NavBarListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraSelector: CameraSelector
    private var imageCapture: ImageCapture? = null
    private lateinit var imageCaptureExecutor: ExecutorService

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (PermissionsUtil.arePermissionsGranted(this, PermissionsUtil.PHOTO_PERMISSIONS)) {
                setupCameraProvider()
            } else {
                DialogUtil.showPermissionDeniedDialog(this, packageName)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initCameraComponents()
        setupNavBar()
        setupListeners()
    }

    private fun initCameraComponents() {
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        imageCaptureExecutor = Executors.newSingleThreadExecutor()
        checkAndRequestPermissions()
    }

    private fun setupNavBar() {
        supportFragmentManager.beginTransaction()
            .replace(binding.navbarContainer.id, NavBarFragment(this, R.id.photoBtn))
            .commit()
    }

    private fun setupListeners() {
        binding.captureButton.setOnClickListener {
            if (PermissionsUtil.arePermissionsGranted(this, PermissionsUtil.PHOTO_PERMISSIONS)) {
                capturePhoto()
                animateFlashEffect()
            } else {
                checkAndRequestPermissions()
            }
        }

        binding.switchBtn.setOnClickListener {
            toggleCameraSelector()
            setupCameraProvider()
        }
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = PermissionsUtil.getMissingPermissions(this, PermissionsUtil.PHOTO_PERMISSIONS)
        if (missingPermissions.isNotEmpty()) {
            DialogUtil.showRationaleDialog(
                this,
                "Camera access is required to take photos. Please grant the permission."
            ) {
                permissionsLauncher.launch(missingPermissions.toTypedArray())
            }
        } else {
            setupCameraProvider()
        }
    }

    private fun setupCameraProvider() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to get CameraProvider", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val preview = Preview.Builder().build().apply {
            surfaceProvider = binding.preview.surfaceProvider
        }

        imageCapture = ImageCapture.Builder().build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        } catch (e: Exception) {
            Log.e("MainActivity", "Use case binding failed", e)
        }
    }

    private fun capturePhoto() {
        val fileName = "JPEG_${System.currentTimeMillis()}.jpg"
        val file = File(externalMediaDirs[0], fileName)

        imageCapture?.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            imageCaptureExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Photo saved: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Failed to capture photo", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("MainActivity", "Capture failed", exception)
                }
            }
        )
    }

    private fun animateFlashEffect() {
        binding.root.apply {
            foreground = ColorDrawable(Color.WHITE)
            postDelayed({ foreground = null }, 100)
        }
    }

    private fun toggleCameraSelector() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        imageCaptureExecutor.shutdown()
    }

    override fun onGallerySelected() {
        startActivity(Intent(this, GalleryActivity::class.java))
    }

    override fun onPhotoModeSelected() {
    }

    override fun onVideoModeSelected() {
        startActivity(Intent(this, VideoActivity::class.java))
    }
}
