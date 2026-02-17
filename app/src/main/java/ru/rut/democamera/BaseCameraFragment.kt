package ru.rut.democamera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import ru.rut.democamera.utils.CameraUtil
import ru.rut.democamera.utils.DialogUtil
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class BaseCameraFragment : Fragment() {
    protected lateinit var cameraExecutor: ExecutorService
    protected var camera: Camera? = null
    protected var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    protected var isFlashEnabled = false
    protected lateinit var previewView: PreviewView
    protected lateinit var focusView: View
    protected lateinit var flashButton: ImageButton
    protected lateinit var switchButton: ImageButton
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var currentZoomRatio: Float = 1.0f

    protected abstract val requiredPermissions: Array<String>
    protected abstract val rationaleMessage: String
    protected abstract fun setupAdditionalUseCases(cameraProvider: ProcessCameraProvider)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            onPermissionsGranted()
        } else {
            DialogUtil.showPermissionDeniedDialog(requireContext(), requireContext().packageName)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAndRequestPermissions()
        setupGestureDetector()
    }

    private fun checkAndRequestPermissions() {
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            onPermissionsGranted()
        } else {
            DialogUtil.showRationaleDialog(requireContext(), rationaleMessage) {
                requestPermissionLauncher.launch(requiredPermissions)
            }
        }
    }

    protected open fun onPermissionsGranted() {
        CameraUtil.getCameraProvider(requireContext()) { provider ->
            setupCamera(provider)
        }
    }

    protected fun setupCamera(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        try {
            cameraProvider.unbindAll()

            // ✅ Привязываем Preview + дополнительные use cases В ОДНОМ ВЫЗОВЕ
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview
            )

            setupAdditionalUseCases(cameraProvider)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupGestureDetector() {
        scaleGestureDetector = ScaleGestureDetector(
            requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    camera?.let { cam ->
                        currentZoomRatio *= detector.scaleFactor
                        val clampedZoomRatio = currentZoomRatio.coerceIn(
                            cam.cameraInfo.zoomState.value?.minZoomRatio ?: 1.0f,
                            cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 4.0f
                        )
                        cam.cameraControl.setZoomRatio(clampedZoomRatio)
                        currentZoomRatio = clampedZoomRatio
                    }
                    return true
                }
            }
        )
    }

    protected fun handleTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector?.onTouchEvent(event)

        if (event.pointerCount == 1 && event.action == MotionEvent.ACTION_UP) {
            handleTapToFocus(event)
        }
        return true
    }

    protected fun handleTapToFocus(event: MotionEvent) {
        val camera = camera ?: return
        val factory = previewView.meteringPointFactory
        val point = factory.createPoint(event.x, event.y)
        val action = FocusMeteringAction.Builder(point)
            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        camera.cameraControl.startFocusAndMetering(action)
        showFocusIndicator(event.x, event.y)
    }

    protected fun showFocusIndicator(x: Float, y: Float) {
        val size = focusView.width.takeIf { it > 0 } ?: 60
        focusView.apply {
            translationX = x - size / 2
            translationY = y - size / 2
            scaleX = 1.5f
            scaleY = 1.5f
            alpha = 1f
            visibility = View.VISIBLE
            animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        }
        focusView.postDelayed({
            focusView.animate().alpha(0f).setDuration(200).withEndAction {
                focusView.visibility = View.GONE
                focusView.alpha = 1f
            }.start()
        }, 800)
    }

    protected fun toggleFlash() {
        isFlashEnabled = !isFlashEnabled
        flashButton.setImageResource(
            if (isFlashEnabled) R.drawable.ic_flash_state_on
            else R.drawable.ic_flash_state_off
        )
        camera?.cameraControl?.enableTorch(isFlashEnabled)
    }

    protected fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        val isFrontCamera = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
        flashButton.visibility = if (isFrontCamera) View.GONE else View.VISIBLE
        onPermissionsGranted()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}