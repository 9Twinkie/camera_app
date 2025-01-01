package ru.rut.democamera.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat

object CameraUtil {

    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var currentZoomRatio: Float = 1.0f

    fun getCameraProvider(context: Context, callback: (ProcessCameraProvider) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            callback(cameraProviderFuture.get())
        }, ContextCompat.getMainExecutor(context))
    }

    fun enableTorchOnRecording(camera: Camera?, isRecording: Boolean, flashEnabled: Boolean) {
        if (flashEnabled && isRecording) {
            camera?.cameraControl?.enableTorch(true)
        } else if (flashEnabled) {
            camera?.cameraControl?.enableTorch(false)
        }
    }

    fun controlFlashDuringAction(
        camera: Camera?,
        flashEnabled: Boolean,
        onActionCompleted: () -> Unit
    ) {
        if (flashEnabled) {
            camera?.cameraControl?.enableTorch(true)
        }
        onActionCompleted()
        if (flashEnabled) {
            camera?.cameraControl?.enableTorch(false)
        }
    }

    fun showToast(context: Context, message: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun toggleCameraSelector(
        currentSelector: CameraSelector,
        onCameraChanged: (Boolean) -> Unit
    ): CameraSelector {
        val newSelector = if (currentSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        onCameraChanged(isFrontCamera(newSelector))
        return newSelector
    }

    private fun isFrontCamera(cameraSelector: CameraSelector): Boolean {
        return cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
    }

    fun initPinchToZoom(context: Context, camera: Camera) {
        scaleGestureDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    currentZoomRatio *= detector.scaleFactor
                    val clampedZoomRatio = currentZoomRatio.coerceIn(
                        camera.cameraInfo.zoomState.value?.minZoomRatio ?: 1.0f,
                        camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 4.0f
                    )
                    camera.cameraControl.setZoomRatio(clampedZoomRatio)
                    currentZoomRatio = clampedZoomRatio
                    return true
                }
            })
    }

    fun handleTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector?.onTouchEvent(event)
        return true
    }
}
