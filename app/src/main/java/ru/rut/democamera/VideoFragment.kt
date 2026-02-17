package ru.rut.democamera

import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import ru.rut.democamera.databinding.FragmentVideoBinding
import ru.rut.democamera.utils.CameraUtil
import java.io.File
import java.util.concurrent.TimeUnit

class VideoFragment : BaseCameraFragment() {
    private var _binding: FragmentVideoBinding? = null
    private val binding get() = _binding!!

    private lateinit var videoCapture: VideoCapture<Recorder>
    private var recording: Recording? = null
    private var isRecording = false

    private val timerHandler = Handler(Looper.getMainLooper())
    private var secondsElapsed = 0
    private val timerRunnable = object : Runnable {
        override fun run() {
            secondsElapsed++
            binding.recordTimer.text = formatTime(secondsElapsed)
            timerHandler.postDelayed(this, 1000)
        }
    }

    private var currentVideoFile: File? = null

    override val requiredPermissions = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO
    )
    override val rationaleMessage = "Camera and audio access are required to record videos."

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        previewView = binding.preview
        focusView = binding.focusView
        flashButton = binding.flashBtn
        switchButton = binding.switchBtn

        binding.recordTimer.text = "00:00"
        binding.recordTimer.visibility = View.GONE

        setupListeners()
    }

    private fun setupListeners() {
        binding.preview.setOnTouchListener { _, event ->
            handleTouchEvent(event)
        }

        binding.flashBtn.setOnClickListener {
            toggleFlash()
        }

        binding.captureButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        binding.switchBtn.setOnClickListener {
            // Переключение камеры только когда НЕ идет запись
            if (!isRecording) {
                switchCamera()
            } else {
                CameraUtil.showToast(requireContext(), "Сначала остановите запись")
            }
        }
    }

    override fun setupAdditionalUseCases(cameraProvider: ProcessCameraProvider) {
        val recorder = Recorder.Builder().build()
        videoCapture = VideoCapture.withOutput(recorder)

        camera = cameraProvider.bindToLifecycle(
            this,
            cameraSelector,
            videoCapture
        )
    }

    private fun startRecording() {
        currentVideoFile = CameraUtil.generateOutputFile("VIDEO", "mp4")
        val outputOptions = FileOutputOptions.Builder(currentVideoFile!!).build()

        try {
            recording = videoCapture.output
                .prepareRecording(requireActivity(), outputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(requireContext())) { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        handleRecordingFinalize(event)
                    }
                }

            isRecording = true
            binding.captureButton.setIconResource(R.drawable.ic_square)
            binding.recordTimer.visibility = View.VISIBLE
            timerHandler.post(timerRunnable)
            CameraUtil.showToast(requireContext(), "Запись началась")

        } catch (e: SecurityException) {
            CameraUtil.showToast(requireContext(), "Нет разрешений")
        }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
        isRecording = false
        timerHandler.removeCallbacks(timerRunnable)
        binding.recordTimer.visibility = View.GONE
        binding.captureButton.setIconResource(R.drawable.ic_circle)
    }

    private fun handleRecordingFinalize(event: VideoRecordEvent.Finalize) {
        if (event.hasError()) {
            CameraUtil.showToast(requireContext(), "Ошибка записи")
            return
        }

        currentVideoFile?.let { file ->
            MediaScannerConnection.scanFile(
                requireContext(),
                arrayOf(file.absolutePath),
                arrayOf("video/mp4"),
                null
            )
            CameraUtil.showToast(requireContext(), "Видео сохранено!")
        }

        secondsElapsed = 0
        binding.recordTimer.text = "00:00"
    }

    private fun formatTime(seconds: Int): String {
        val minutes = TimeUnit.SECONDS.toMinutes(seconds.toLong())
        val remainingSeconds = seconds - TimeUnit.MINUTES.toSeconds(minutes).toInt()
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        timerHandler.removeCallbacks(timerRunnable)
        if (recording != null) {
            recording?.stop()
            recording = null
        }
    }
}