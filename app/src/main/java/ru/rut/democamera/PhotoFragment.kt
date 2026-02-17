package ru.rut.democamera

import android.media.MediaScannerConnection
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import ru.rut.democamera.databinding.FragmentPhotoBinding
import ru.rut.democamera.utils.CameraUtil

class PhotoFragment : BaseCameraFragment() {

    private var _binding: FragmentPhotoBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null

    override val requiredPermissions = arrayOf(android.Manifest.permission.CAMERA)
    override val rationaleMessage = "Camera access is required to take photos."

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация view элементов
        previewView = binding.preview
        focusView = binding.focusView
        flashButton = binding.flashBtn
        switchButton = binding.switchBtn

        setupListeners()
    }

    protected fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(previewView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Применяем отступы для preview
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                0  // Снизу не добавляем, там кнопка
            )

            insets
        }
    }

    private fun setupListeners() {
        // Обработка касаний (зум + фокус)
        binding.preview.setOnTouchListener { _, event ->
            handleTouchEvent(event)
        }

        // Вспышка
        binding.flashBtn.setOnClickListener {
            toggleFlash()
        }

        // Съемка фото
        binding.captureButton.setOnClickListener {
            capturePhoto()
        }

        // Переключение камеры
        binding.switchBtn.setOnClickListener {
            switchCamera()
        }
    }

    override fun setupAdditionalUseCases(cameraProvider: ProcessCameraProvider) {
        imageCapture = ImageCapture.Builder()
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
            .build()

        cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
    }

    private fun capturePhoto() {
        val file = CameraUtil.generateOutputFile("PHOTO", "jpg")

        // Эффект вспышки на экране
        binding.root.apply {
            foreground = android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE)
            postDelayed({ foreground = null }, 100)
        }

        // Устанавливаем FLASH_MODE для фото
        imageCapture?.flashMode = if (isFlashEnabled) {
            ImageCapture.FLASH_MODE_ON
        } else {
            ImageCapture.FLASH_MODE_OFF
        }

        imageCapture?.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    activity?.runOnUiThread {
                        CameraUtil.showToast(requireContext(), "Photo saved")
                        MediaScannerConnection.scanFile(
                            requireContext(),
                            arrayOf(file.absolutePath),
                            null,
                            null
                        )
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    activity?.runOnUiThread {
                        CameraUtil.showToast(requireContext(), "Failed to capture photo")
                    }
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}