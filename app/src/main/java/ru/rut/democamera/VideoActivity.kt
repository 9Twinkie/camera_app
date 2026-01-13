package ru.rut.democamera

import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import ru.rut.democamera.databinding.ActivityVideoBinding
import ru.rut.democamera.utils.CameraUtil
import ru.rut.democamera.utils.DialogUtil
import ru.rut.democamera.utils.PermissionsUtil
import java.io.File
import java.util.concurrent.TimeUnit

class VideoActivity : BaseCameraActivity() {

    private lateinit var binding: ActivityVideoBinding
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var recording: Recording? = null

    override val requiredPermissions = PermissionsUtil.VIDEO_PERMISSIONS
    override val rationaleMessage =
        "Camera and audio access are required to record videos."

    /* ===== Таймер ===== */

    private val timerHandler = Handler(Looper.getMainLooper())
    private var secondsElapsed = 0

    private val timerRunnable = object : Runnable {
        override fun run() {
            secondsElapsed++
            binding.recordTimer.text = formatTime(secondsElapsed)
            timerHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAndRequestPermissions { onPermissionsGranted() }
        setupNavBar(R.id.videoBtn)
        setupListeners()

        binding.recordTimer.text = "00:00"
        binding.recordTimer.visibility = android.view.View.GONE
    }

    private fun setupListeners() {
        binding.preview.setOnTouchListener { _, event ->
            handleTouchEvent(
                binding.preview,
                binding.focusView,
                event
            )
        }

        binding.flashBtn.setOnClickListener {
            toggleFlash(binding.flashBtn)
        }
        binding.captureButton.setOnClickListener {
            toggleRecording()
        }
        binding.switchBtn.setOnClickListener {
            switchCamera(binding.flashBtn)
        }
    }

    override fun onPermissionsGranted() {
        super.onPermissionsGranted()
        CameraUtil.getCameraProvider(this) { provider ->
            cameraProvider = provider
            setupCamera(binding.preview.surfaceProvider) {
                val recorder = Recorder.Builder().build()
                videoCapture = VideoCapture.withOutput(recorder)
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    videoCapture
                )
            }
        }
    }

    private fun toggleRecording() {
        checkAndRequestPermissions {
            if (recording == null) {
                startRecording()
                binding.captureButton.icon =
                    ContextCompat.getDrawable(this, R.drawable.ic_square)
                binding.flashBtn.isEnabled = false
                binding.switchBtn.isEnabled = false
            } else {
                stopRecording()
                binding.captureButton.icon =
                    ContextCompat.getDrawable(this, R.drawable.ic_circle)
                binding.flashBtn.isEnabled = true
                binding.switchBtn.isEnabled = true
            }
        }
    }

    private fun startRecording() {
        if (!PermissionsUtil.arePermissionsGranted(this, requiredPermissions)) {
            CameraUtil.showToast(this, "Permissions are missing.")
            return
        }

        val file = CameraUtil.generateOutputFile("VIDEO", "mp4")
        val outputOptions = FileOutputOptions.Builder(file).build()

        try {
            camera?.cameraControl?.enableTorch(isFlashEnabled)

            recording = videoCapture.output
                .prepareRecording(this, outputOptions)
                .withAudioEnabled()
                .start(
                    ContextCompat.getMainExecutor(this)
                ) { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        handleRecordingFinalized(event, file)
                    }
                }

            /* ---- запуск таймера ---- */
            secondsElapsed = 0
            binding.recordTimer.text = "00:00"
            binding.recordTimer.visibility = android.view.View.VISIBLE
            timerHandler.post(timerRunnable)

        } catch (se: SecurityException) {
            CameraUtil.showToast(
                this,
                "SecurityException: Missing required permissions."
            )
            DialogUtil.showPermissionDeniedDialog(this, packageName)
        }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null

        camera?.cameraControl?.enableTorch(false)

        /* ---- остановка таймера ---- */
        timerHandler.removeCallbacks(timerRunnable)
        binding.recordTimer.visibility = android.view.View.GONE
        secondsElapsed = 0
    }

    private fun handleRecordingFinalized(
        event: VideoRecordEvent.Finalize,
        file: File
    ) {
        if (event.hasError()) {
            CameraUtil.showToast(this, "Error recording video.")
        } else {
            CameraUtil.showToast(
                this,
                "Video saved: ${file.absolutePath}"
            )
            MediaScannerConnection.scanFile(
                this,
                arrayOf(file.absolutePath),
                null,
                null
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun formatTime(seconds: Int): String {
        val minutes = TimeUnit.SECONDS.toMinutes(seconds.toLong())
        val remainingSeconds =
            seconds - TimeUnit.MINUTES.toSeconds(minutes).toInt()
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
}
