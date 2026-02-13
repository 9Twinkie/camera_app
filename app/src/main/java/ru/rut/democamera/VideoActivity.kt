package ru.rut.democamera

import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
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
    private var pendingStartRecording = false


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
        binding.recordTimer.visibility = View.GONE
    }

    private fun setupListeners() {
        binding.preview.setOnTouchListener { _, event ->
            handleTouchEvent(binding.preview, binding.focusView, event)
        }

        binding.flashBtn.setOnClickListener {
            toggleFlash(binding.flashBtn)
        }

        binding.captureButton.setOnClickListener {
            toggleRecording()
        }

        binding.switchBtn.setOnClickListener {
            switchCameraDuringRecording()
        }
    }

    override fun onPermissionsGranted() {
        super.onPermissionsGranted()
        CameraUtil.getCameraProvider(this) { provider ->
            cameraProvider = provider
            setupCamera(binding.preview.surfaceProvider) {
                val recorder = Recorder.Builder().build()
                videoCapture = VideoCapture.withOutput(recorder)
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, videoCapture)

                camera?.cameraInfo?.cameraState?.observe(this) { state ->
                    if (
                        state.type == androidx.camera.core.CameraState.Type.OPEN &&
                        pendingStartRecording
                    ) {
                        pendingStartRecording = false
                        startRecording()
                    }
                }

            }
        }
    }

    /* ================== ЗАПИСЬ ================== */

    private fun toggleRecording() {
        checkAndRequestPermissions {
            if (recording == null) {
                startRecording()
                binding.captureButton.icon =
                    ContextCompat.getDrawable(this, R.drawable.ic_square)
            } else {
                stopRecording()
                binding.captureButton.icon =
                    ContextCompat.getDrawable(this, R.drawable.ic_circle)
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
                .start(ContextCompat.getMainExecutor(this)) { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        handleRecordingFinalized(event, file)
                    }
                }

            if (secondsElapsed == 0) {
                binding.recordTimer.text = "00:00"
            }
            binding.recordTimer.visibility = View.VISIBLE
            timerHandler.post(timerRunnable)

        } catch (se: SecurityException) {
            CameraUtil.showToast(this, "Missing permissions.")
            DialogUtil.showPermissionDeniedDialog(this, packageName)
        }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null

        camera?.cameraControl?.enableTorch(false)

        timerHandler.removeCallbacks(timerRunnable)
        binding.recordTimer.visibility = View.GONE
        secondsElapsed = 0
    }

    /* ================== ПЕРЕКЛЮЧЕНИЕ КАМЕРЫ ================== */

    private fun switchCameraDuringRecording() {
        val wasRecording = recording != null

        if (wasRecording) {
            pendingStartRecording = true
            recording?.stop()
            recording = null
        }

        cameraSelector = CameraUtil.toggleCameraSelector(cameraSelector) { isFront ->
            binding.flashBtn.visibility = if (isFront) View.GONE else View.VISIBLE
        }

        onPermissionsGranted()
    }


    /* ================== CALLBACK ================== */

    private fun handleRecordingFinalized(
        event: VideoRecordEvent.Finalize,
        file: File
    ) {
        if (event.hasError()) {
            CameraUtil.showToast(this, "Error recording video.")
        } else {
            CameraUtil.showToast(this, "Video saved")
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

    /* ================== UTILS ================== */

    private fun formatTime(seconds: Int): String {
        val minutes = TimeUnit.SECONDS.toMinutes(seconds.toLong())
        val remainingSeconds =
            seconds - TimeUnit.MINUTES.toSeconds(minutes).toInt()
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
}
