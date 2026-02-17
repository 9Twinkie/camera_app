package ru.rut.democamera

import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import ru.rut.democamera.databinding.FragmentVideoBinding
import ru.rut.democamera.utils.CameraUtil
import ru.rut.democamera.utils.OrientationUtil
import ru.rut.democamera.utils.VideoMerger
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class VideoFragment : BaseCameraFragment() {
    private var _binding: FragmentVideoBinding? = null
    private val binding get() = _binding!!

    private lateinit var videoCapture: VideoCapture<Recorder>
    private var recording: Recording? = null
    private var isRecording = false

    private val timerHandler = Handler(Looper.getMainLooper())
    private var secondsElapsed = 0
    private var currentVideoFile: File? = null
    private var segmentIndex = 0

    private val videoSegments = CopyOnWriteArrayList<File>()
    private var currentRotation = Surface.ROTATION_0
    private var isFirstSegment = true

    private val timerRunnable = object : Runnable {
        override fun run() {
            secondsElapsed++
            binding.recordTimer.text = formatTime(secondsElapsed)
            timerHandler.postDelayed(this, 1000)
        }
    }

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

        initOrientationListener()
        setupListeners()
    }

    private fun initOrientationListener() {
        OrientationUtil.initOrientationListener(requireContext()) { rotation ->
            currentRotation = rotation

            if (isRecording) {
                videoCapture.targetRotation = rotation
            }
        }

        currentRotation = requireActivity().windowManager.defaultDisplay.rotation
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
            if (isRecording) {
                switchCameraDuringRecording()
            } else {
                switchCamera()
            }
        }
    }

    // ✅ ПЕРЕКЛЮЧЕНИЕ КАМЕРЫ ВО ВРЕМЯ ЗАПИСИ
    private fun switchCameraDuringRecording() {
        // 1. Останавливаем текущую запись
        recording?.stop()
        recording = null

        // 2. Переключаем камеру
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        // 3. Скрываем кнопку вспышки для фронтальной камеры
        val isFrontCamera = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
        flashButton.visibility = if (isFrontCamera) View.GONE else View.VISIBLE

        // 4. Перезапускаем камеру
        CameraUtil.getCameraProvider(requireContext()) { provider ->
            // ✅ Отвязываем всё перед новой привязкой
            provider.unbindAll()

            setupCamera(provider)

            activity?.runOnUiThread {
                isFirstSegment = false
                startRecording()
                CameraUtil.showToast(requireContext(), "Камера переключена")
            }
        }
    }

    // ✅ ИСПРАВЛЕННЫЙ МЕТОД - привязываем VideoCapture вместе с Preview
    override fun setupAdditionalUseCases(cameraProvider: ProcessCameraProvider) {
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()

        videoCapture = VideoCapture.withOutput(recorder)
        videoCapture.targetRotation = currentRotation

        // ✅ Привязываем VideoCapture к уже существующей камере
        camera = cameraProvider.bindToLifecycle(
            this,
            cameraSelector,
            videoCapture
        )
    }

    private fun startRecording() {
        currentVideoFile = CameraUtil.generateOutputFile(
            "VIDEO_SEGMENT_${segmentIndex++}_${System.currentTimeMillis()}",
            "mp4"
        )

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

            if (isFirstSegment) {
                CameraUtil.showToast(requireContext(), "Запись началась")
            }
        } catch (e: SecurityException) {
            CameraUtil.showToast(requireContext(), "Нет разрешений")
        } catch (e: Exception) {
            e.printStackTrace()
            CameraUtil.showToast(requireContext(), "Ошибка: ${e.message}")
        }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
        isRecording = false
        timerHandler.removeCallbacks(timerRunnable)
        binding.recordTimer.visibility = View.GONE
        binding.captureButton.setIconResource(R.drawable.ic_circle)

        if (videoSegments.isNotEmpty()) {
            mergeAllSegments()
        }

        segmentIndex = 0
        isFirstSegment = true
        videoSegments.clear()

        secondsElapsed = 0
        binding.recordTimer.text = "00:00"
    }

    // ✅ ОБЪЕДИНЕНИЕ ВСЕХ СЕГМЕНТОВ В ОДИН ФАЙЛ
    private fun mergeAllSegments() {
        if (videoSegments.isEmpty()) return

        try {
            val outputFile = CameraUtil.generateOutputFile(
                "VIDEO_FINAL_${System.currentTimeMillis()}",
                "mp4"
            )

            val success = VideoMerger.mergeVideos(
                outputFile,
                *videoSegments.toTypedArray()
            )

            if (success) {
                MediaScannerConnection.scanFile(
                    requireContext(),
                    arrayOf(outputFile.absolutePath),
                    arrayOf("video/mp4"),
                    null
                )

                CameraUtil.showToast(
                    requireContext(),
                    "Видео сохранено (${videoSegments.size} сегмент(а) объединено)"
                )
            } else {
                videoSegments.forEach { file ->
                    MediaScannerConnection.scanFile(
                        requireContext(),
                        arrayOf(file.absolutePath),
                        arrayOf("video/mp4"),
                        null
                    )
                }

                CameraUtil.showToast(
                    requireContext(),
                    "Видео сохранено (${videoSegments.size} файлов)"
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
            CameraUtil.showToast(requireContext(), "Ошибка объединения: ${e.message}")

            videoSegments.forEach { file ->
                MediaScannerConnection.scanFile(
                    requireContext(),
                    arrayOf(file.absolutePath),
                    arrayOf("video/mp4"),
                    null
                )
            }
        }
    }

    private fun handleRecordingFinalize(event: VideoRecordEvent.Finalize) {
        if (event.hasError()) {
            CameraUtil.showToast(requireContext(), "Ошибка записи: ${event.error}")
            return
        }

        currentVideoFile?.let { file ->
            videoSegments.add(file)

            if (!isFirstSegment) {
                CameraUtil.showToast(requireContext(), "Сегмент записан")
            }
        }
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

        OrientationUtil.disableOrientationListener()

        if (recording != null) {
            recording?.stop()
            recording = null
        }
    }
}