package ru.rut.democamera

import android.media.MediaScannerConnection
import android.os.Bundle
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import ru.rut.democamera.databinding.ActivityMainBinding
import ru.rut.democamera.utils.CameraUtil
import ru.rut.democamera.utils.PermissionsUtil

class MainActivity : BaseCameraActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    override val requiredPermissions = PermissionsUtil.PHOTO_PERMISSIONS
    override val rationaleMessage = "Camera access is required to take photos."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkAndRequestPermissions { onPermissionsGranted() }
        setupNavBar(R.id.photoBtn)
        setupListeners()
    }

    private fun setupListeners() {
        binding.preview.setOnTouchListener { view, event -> handleTouchEvent(view, event) }
        binding.flashBtn.setOnClickListener { toggleFlash(binding.flashBtn) }
        binding.captureButton.setOnClickListener { capturePhoto() }
        binding.switchBtn.setOnClickListener { switchCamera(binding.flashBtn) }
    }

    override fun onPermissionsGranted() {
        super.onPermissionsGranted()
        CameraUtil.getCameraProvider(this) { provider ->
            cameraProvider = provider
            setupCamera(binding.preview.surfaceProvider) {
                imageCapture = ImageCapture.Builder().build()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
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
        checkAndRequestPermissions {
            val file = CameraUtil.generateOutputFile("PHOTO", "JPEG")
            animateFlashEffect()
            CameraUtil.controlFlashDuringAction(camera, isFlashEnabled) {
                imageCapture?.takePicture(
                    ImageCapture.OutputFileOptions.Builder(file).build(),
                    cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(
                            outputFileResults: ImageCapture.OutputFileResults
                        ) {
                            runOnUiThread {
                                CameraUtil.showToast(
                                    this@MainActivity,
                                    "Photo saved: ${file.absolutePath}"
                                )
                                MediaScannerConnection.scanFile(
                                    this@MainActivity,
                                    arrayOf(file.absolutePath),
                                    null,
                                    null
                                )
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            runOnUiThread {
                                CameraUtil.showToast(
                                    this@MainActivity,
                                    "Failed to capture photo."
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

