package ru.rut.democamera

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import ru.rut.democamera.databinding.ActivityVideoBinding
import ru.rut.democamera.utils.CameraUtil
import ru.rut.democamera.utils.DialogUtil
import ru.rut.democamera.utils.PermissionsUtil
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VideoActivity : AppCompatActivity(), NavBarFragment.NavBarListener {

    private lateinit var binding: ActivityVideoBinding
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraSelector: CameraSelector
    private var recording: Recording? = null
    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (PermissionsUtil.arePermissionsGranted(this, PermissionsUtil.VIDEO_PERMISSIONS)) {
                setupCameraProvider()
            } else {
                DialogUtil.showPermissionDeniedDialog(this, packageName)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraExecutor = Executors.newSingleThreadExecutor()

        checkAndRequestPermissions()
        setupNavBar()

        binding.preview.setOnTouchListener { _, event ->
            camera?.let { CameraUtil.handleTouchEvent(event) } ?: false
        }

        binding.captureButton.setOnClickListener {
            if (PermissionsUtil.arePermissionsGranted(this, PermissionsUtil.VIDEO_PERMISSIONS)) {
                if (recording == null) {
                    startRecording()
                    binding.captureButton.icon = ContextCompat.getDrawable(this, R.drawable.ic_square)
                } else {
                    stopRecording()
                    binding.captureButton.icon = ContextCompat.getDrawable(this, R.drawable.ic_circle)
                }
            } else {
                checkAndRequestPermissions()
            }
        }


        binding.switchBtn.setOnClickListener {
            cameraSelector = CameraUtil.toggleCameraSelector(cameraSelector)
            setupCameraProvider()
        }
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = PermissionsUtil.getMissingPermissions(this, PermissionsUtil.VIDEO_PERMISSIONS)
        if (missingPermissions.isNotEmpty()) {
            DialogUtil.showRationaleDialog(this, "Camera and audio permissions are required.") {
                requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
            }
        } else {
            setupCameraProvider()
        }
    }

    private fun setupCameraProvider() {
        CameraUtil.getCameraProvider(this) { provider ->
            cameraProvider = provider
            startCamera()
        }
    }

    private fun startCamera() {
        val preview = androidx.camera.core.Preview.Builder().build().also {
            it.surfaceProvider = binding.preview.surfaceProvider
        }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
            camera?.let { CameraUtil.initPinchToZoom(this, it) }
        } catch (exc: Exception) {
            Log.e("VideoActivity", "Failed to bind camera use cases.", exc)
        }
    }

    private fun startRecording() {
        val name = "VID_${System.currentTimeMillis()}.mp4"
        val file = File(externalMediaDirs[0], name)
        val outputOptions = FileOutputOptions.Builder(file).build()

        try {
            recording = videoCapture.output
                .prepareRecording(this, outputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this)) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> CameraUtil.showToast(this, "Recording started.")
                        is VideoRecordEvent.Finalize -> {
                            if (event.hasError()) {
                                CameraUtil.showToast(this, "Error recording video.")
                            } else {
                                CameraUtil.showToast(this, "Video saved: ${file.absolutePath}")
                            }
                            recording = null
                        }
                    }
                }
        } catch (e: SecurityException) {
            CameraUtil.showToast(this, "Permissions are missing.")
        }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
    }

    private fun setupNavBar() {
        supportFragmentManager.beginTransaction()
            .replace(binding.navbarContainer.id, NavBarFragment(this, R.id.videoBtn))
            .commit()
    }

    override fun onGallerySelected() {
        startActivity(Intent(this, GalleryActivity::class.java))
        finish()
    }

    override fun onPhotoModeSelected() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onVideoModeSelected() {}
}
