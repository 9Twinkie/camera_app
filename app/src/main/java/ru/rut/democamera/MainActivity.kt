package ru.rut.democamera

import android.os.Bundle
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import ru.rut.democamera.databinding.ActivityMainBinding
import ru.rut.democamera.utils.CameraUtil
import ru.rut.democamera.utils.PermissionsUtil
import java.io.File

class MainActivity : BaseCameraActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAndRequestPermissions()
        setupNavBar(R.id.photoBtn)

        binding.preview.setOnTouchListener { view, event -> handleTouchEvent(view, event) }
        binding.flashBtn.setOnClickListener { toggleFlash(binding.flashBtn) }
        binding.captureButton.setOnClickListener { capturePhotoWithFlash() }
        binding.switchBtn.setOnClickListener { switchCamera(binding.flashBtn) }
    }

    override fun requiredPermissions() = PermissionsUtil.PHOTO_PERMISSIONS

    override fun rationaleMessage() = "Camera access is required to take photos."

    override fun onPermissionsGranted() {
        CameraUtil.getCameraProvider(this) { provider ->
            cameraProvider = provider
            bindCameraUseCases()
        }
    }

    private fun bindCameraUseCases() {
        val preview = androidx.camera.core.Preview.Builder().build().also {
            it.surfaceProvider = binding.preview.surfaceProvider
        }

        imageCapture = ImageCapture.Builder().build()

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            CameraUtil.initPinchToZoom(this, camera!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun capturePhotoWithFlash() {
        attemptActionOrRequestPermissions {
            CameraUtil.controlFlashDuringAction(camera, isFlashEnabled) {
                capturePhoto()
            }
        }
    }
    private fun animateFlashEffect() {
        binding.root.apply {
            foreground = android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE)
            postDelayed({ foreground = null }, 100)
        }
    }

    private fun capturePhoto() {
        val file = File(externalMediaDirs.first(), "JPEG_${System.currentTimeMillis()}.jpg")
        animateFlashEffect()
        imageCapture?.takePicture(

            ImageCapture.OutputFileOptions.Builder(file).build(),
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    runOnUiThread {
                        CameraUtil.showToast(this@MainActivity, "Photo saved: ${file.absolutePath}")
                    }

                }

                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        CameraUtil.showToast(this@MainActivity, "Failed to capture photo")
                    }
                }
            }
        )
    }
}
