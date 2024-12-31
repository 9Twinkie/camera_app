package ru.rut.democamera

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import ru.rut.democamera.databinding.ActivityMainBinding
import ru.rut.democamera.utils.CameraUtil
import ru.rut.democamera.utils.DialogUtil
import ru.rut.democamera.utils.PermissionsUtil
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), NavBarFragment.NavBarListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraSelector: CameraSelector
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private var isFlashEnabled = false

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

        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraExecutor = Executors.newSingleThreadExecutor()

        checkAndRequestPermissions()
        setupNavBar()

        binding.preview.setOnTouchListener { view, event ->
            camera?.let {
                CameraUtil.handleTouchEvent(event)
            }

            if (event.action == MotionEvent.ACTION_UP) {
                view.performClick()
            }

            true
        }
        binding.flashBtn.setOnClickListener {
            isFlashEnabled = !isFlashEnabled
            binding.flashBtn.setImageResource(
                if (isFlashEnabled) R.drawable.ic_flash_state_on else R.drawable.ic_flash_state_off
            )
        }

        binding.captureButton.setOnClickListener {
            if (PermissionsUtil.arePermissionsGranted(this, PermissionsUtil.PHOTO_PERMISSIONS)) {
                capturePhoto()
                animateFlashEffect()
            } else {
                checkAndRequestPermissions()
            }
        }

        binding.switchBtn.setOnClickListener {
            cameraSelector = CameraUtil.toggleCameraSelector(cameraSelector) { isFrontCamera ->
                binding.flashBtn.visibility = if (isFrontCamera) View.GONE else View.VISIBLE
            }
            setupCameraProvider()
        }

    }

    private fun checkAndRequestPermissions() {
        PermissionsUtil.handlePermissions(
            this,
            PermissionsUtil.PHOTO_PERMISSIONS,
            permissionsLauncher,
            "Camera access is required to take photos. Please grant the permission.",
            ::setupCameraProvider
        ) { message, onConfirm ->
            DialogUtil.showRationaleDialog(this, message, onConfirm)
        }
    }



    private fun setupCameraProvider() {
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
            camera?.let { CameraUtil.initPinchToZoom(this, it) }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to bind camera use cases.", e)
        }
    }

    private fun capturePhoto() {
        val fileName = "JPEG_${System.currentTimeMillis()}.jpg"
        val file = File(externalMediaDirs[0], fileName)

        if (isFlashEnabled) {
            camera?.cameraControl?.enableTorch(true)
        }

        imageCapture?.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    runOnUiThread {
                        if (isFlashEnabled) {
                            camera?.cameraControl?.enableTorch(false)
                        }
                        Toast.makeText(this@MainActivity, "Photo saved: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        if (isFlashEnabled) {
                            camera?.cameraControl?.enableTorch(false)
                        }
                        Toast.makeText(this@MainActivity, "Failed to capture photo", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("MainActivity", "Capture failed.", exception)
                }
            }
        )
    }

    private fun animateFlashEffect() {
        binding.root.apply {
            foreground = android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE)
            postDelayed({ foreground = null }, 100)
        }
    }

    private fun setupNavBar() {
        supportFragmentManager.beginTransaction()
            .replace(binding.navbarContainer.id, NavBarFragment(this, R.id.photoBtn))
            .commit()
    }

    override fun onGallerySelected() {
        startActivity(Intent(this, GalleryActivity::class.java))
        finish()
    }

    override fun onPhotoModeSelected() {}

    override fun onVideoModeSelected() {
        startActivity(Intent(this, VideoActivity::class.java))
        finish()
    }
}
