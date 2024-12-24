package ru.rut.democamera

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import ru.rut.democamera.databinding.ActivityVideoBinding
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

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (PermissionsUtil.arePermissionsGranted(this, PermissionsUtil.VIDEO_PERMISSIONS)) {
                setupCameraProvider()
            } else {
                Toast.makeText(
                    this,
                    "Camera and audio permissions are required to record videos.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraExecutor = Executors.newSingleThreadExecutor()

        checkAndRequestPermissions()

        supportFragmentManager.beginTransaction()
            .replace(binding.navbarContainer.id, NavBarFragment(this, R.id.videoBtn))
            .commit()

        binding.captureButton.setOnClickListener {
            if (recording == null) {
                if (PermissionsUtil.arePermissionsGranted(this, PermissionsUtil.VIDEO_PERMISSIONS)) {
                    startRecording()
                    binding.captureButton.icon = ContextCompat.getDrawable(this, R.drawable.ic_square)
                } else {
                    Toast.makeText(
                        this,
                        "Camera and audio permissions are required to record videos.",
                        Toast.LENGTH_SHORT
                    ).show()
                    checkAndRequestPermissions()
                }
            } else {
                stopRecording()
                binding.captureButton.icon = ContextCompat.getDrawable(this, R.drawable.ic_circle)
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

    private fun checkAndRequestPermissions() {
        val missingPermissions = PermissionsUtil.getMissingPermissions(this, PermissionsUtil.VIDEO_PERMISSIONS)
        if (missingPermissions.isNotEmpty()) {
            PermissionsUtil.showRationaleDialog(
                this,
                "Camera and audio access are required to record videos. Please grant the permissions."
            ) {
                requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
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
                startCamera()
            } catch (e: Exception) {
                Log.e("VideoActivity", "Failed to get ProcessCameraProvider", e)
            }
        }, ContextCompat.getMainExecutor(this))
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
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
        } catch (exc: Exception) {
            Log.e("VideoActivity", "Use case binding failed", exc)
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
                .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
                            binding.captureButton.text = "Stop"
                        }
                        is VideoRecordEvent.Finalize -> {
                            if (recordEvent.hasError()) {
                                Toast.makeText(
                                    this,
                                    "Error: ${recordEvent.error}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Video saved: ${file.absolutePath}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            binding.captureButton.text = "Start"
                            recording = null
                        }
                    }
                }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Required permissions are missing.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
    }

    override fun onGallerySelected() {
        finish()
        startActivity(Intent(this, GalleryActivity::class.java))
    }

    override fun onPhotoModeSelected() {
        finish()
        startActivity(Intent(this, MainActivity::class.java))
    }

    override fun onVideoModeSelected() {}

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
