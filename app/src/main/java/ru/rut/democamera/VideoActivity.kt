package ru.rut.democamera

import android.os.Bundle
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import ru.rut.democamera.databinding.ActivityVideoBinding
import ru.rut.democamera.utils.CameraUtil
import ru.rut.democamera.utils.PermissionsUtil
import java.io.File

class VideoActivity : BaseCameraActivity() {

    private lateinit var binding: ActivityVideoBinding
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var recording: Recording? = null

    override val requiredPermissions = PermissionsUtil.VIDEO_PERMISSIONS
    override val rationaleMessage = "Camera and audio access are required to record videos."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAndRequestPermissions { onPermissionsGranted() }
        setupNavBar(R.id.videoBtn)

        setupListeners()
    }

    private fun setupListeners() {
        binding.preview.setOnTouchListener { view, event -> handleTouchEvent(view, event) }
        binding.flashBtn.setOnClickListener { toggleFlash(binding.flashBtn) }
        binding.captureButton.setOnClickListener { toggleRecording() }
        binding.switchBtn.setOnClickListener { switchCamera(binding.flashBtn) }
    }

    override fun onPermissionsGranted() {
        CameraUtil.getCameraProvider(this) { provider ->
            cameraProvider = provider
            setupCamera(binding.preview.surfaceProvider) {
                val recorder = Recorder.Builder().build()
                videoCapture = VideoCapture.withOutput(recorder)
                cameraProvider.bindToLifecycle(this, cameraSelector, videoCapture)
            }
        }
    }

    private fun toggleRecording() {
        if (recording == null) {
            startRecording()
            binding.captureButton.icon = ContextCompat.getDrawable(this, R.drawable.ic_square)
            binding.flashBtn.isEnabled = false
            binding.switchBtn.isEnabled = false
        } else {
            stopRecording()
            binding.captureButton.icon = ContextCompat.getDrawable(this, R.drawable.ic_circle)
            binding.flashBtn.isEnabled = true
            binding.switchBtn.isEnabled = true
        }
    }

    private fun startRecording() {
        if (!PermissionsUtil.arePermissionsGranted(this, requiredPermissions)) {
            CameraUtil.showToast(this, "Permissions are missing.")
            return
        }

        val file = File(externalMediaDirs.first(), "VID_${System.currentTimeMillis()}.mp4")
        val outputOptions = FileOutputOptions.Builder(file).build()

        try {
            camera?.cameraControl?.enableTorch(isFlashEnabled)
            recording = videoCapture.output.prepareRecording(this, outputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this)) { event ->
                    if (event is VideoRecordEvent.Finalize) handleRecordingFinalized(event, file)
                }
        } catch (e: SecurityException) {
            CameraUtil.showToast(this, "Permissions are missing.")
        }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
    }

    private fun handleRecordingFinalized(event: VideoRecordEvent.Finalize, file: File) {
        if (event.hasError()) {
            CameraUtil.showToast(this, "Error recording video.")
        } else {
            CameraUtil.showToast(this, "Video saved: ${file.absolutePath}")
        }
    }
}

