package ru.rut.democamera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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

    private val TAG = "VideoActivity"

    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraPermission = permissions[Manifest.permission.CAMERA] ?: false
            val audioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: false

            if (cameraPermission) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission needed", Toast.LENGTH_SHORT).show()
            }

            if (!audioPermission) {
                Toast.makeText(this, "Audio permission not granted. Video will have no sound.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Request necessary permissions
        requestPermissions()

        // Add NavBarFragment to reuse buttons from the PhotoActivity
        supportFragmentManager.beginTransaction()
            .replace(binding.navbarContainer.id, NavBarFragment(this))
            .commit()

        // Handle Capture button click (start or stop recording)
        binding.captureButton.setOnClickListener {
            if (recording == null) {
                startRecording()
            } else {
                stopRecording()
            }
        }

        // Handle camera switch button click (switch between front and back cameras)
        binding.switchBtn.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }
    }

    private fun requestPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

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
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun startRecording() {
        val name = "VID_${System.currentTimeMillis()}.mp4"
        val file = File(externalMediaDirs[0], name)
        val outputOptions = FileOutputOptions.Builder(file).build()

        recording = videoCapture.output
            .prepareRecording(this, outputOptions)
            .apply {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    withAudioEnabled()
                } else {
                    Toast.makeText(this@VideoActivity, "Audio permission not granted. Recording without sound.", Toast.LENGTH_SHORT).show()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
                        binding.captureButton.text = "Stop"
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (recordEvent.hasError()) {
                            Toast.makeText(this, "Error: ${recordEvent.error}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Video saved: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                        }
                        binding.captureButton.text = "Start"
                        recording = null
                    }
                }
            }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
    }

    // NavBarFragment listener methods
    override fun onGallerySelected() {
        finish()
        startActivity(Intent(this, GalleryActivity::class.java))
    }

    override fun onPhotoModeSelected() {
        finish()
        startActivity(Intent(this, MainActivity::class.java))
    }

    override fun onVideoModeSelected() {
        // Already in video mode, so do nothing
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
