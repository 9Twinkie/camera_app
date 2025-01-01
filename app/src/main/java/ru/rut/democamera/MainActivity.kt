package ru.rut.democamera

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import ru.rut.democamera.databinding.ActivityMainBinding
import ru.rut.democamera.utils.CameraUtil
import ru.rut.democamera.utils.DialogUtil
import ru.rut.democamera.utils.PermissionsUtil
import java.io.File

class MainActivity : BaseCameraActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (PermissionsUtil.arePermissionsGranted(this, requiredPermissions())) {
                onPermissionsGranted()
            } else {
                DialogUtil.showPermissionDeniedDialog(this, packageName)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAndRequestPermissions(permissionsLauncher)
        setupNavBar(R.id.photoBtn)

        binding.preview.setOnTouchListener { view, event ->
            handleTouchEvent(view, event)
        }

        binding.flashBtn.setOnClickListener {
            toggleFlash(binding.flashBtn)
        }
        binding.captureButton.setOnClickListener { capturePhotoWithFlash() }
        binding.switchBtn.setOnClickListener { switchCamera() }
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
        CameraUtil.controlFlashDuringAction(camera, isFlashEnabled) {
            capturePhoto()
        }
    }

    private fun capturePhoto() {
        val file = File(externalMediaDirs.first(), "JPEG_${System.currentTimeMillis()}.jpg")
        imageCapture?.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            cameraExecutor,
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
                }
            }
        )
    }

    private fun switchCamera() {
        cameraSelector = CameraUtil.toggleCameraSelector(cameraSelector) { isFrontCamera ->
            binding.flashBtn.visibility = if (isFrontCamera) View.GONE else View.VISIBLE
        }
        onPermissionsGranted()
    }
}
