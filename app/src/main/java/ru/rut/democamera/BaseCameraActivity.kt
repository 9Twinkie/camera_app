package ru.rut.democamera

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import ru.rut.democamera.utils.CameraUtil
import ru.rut.democamera.utils.DialogUtil
import ru.rut.democamera.utils.PermissionsUtil
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class BaseCameraActivity : AppCompatActivity(), NavBarFragment.NavBarListener {

    protected lateinit var cameraProvider: ProcessCameraProvider
    protected lateinit var cameraExecutor: ExecutorService
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>
    protected var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    protected var camera: Camera? = null
    protected var isFlashEnabled = false

    abstract fun requiredPermissions(): Array<String>
    abstract fun rationaleMessage(): String
    abstract fun onPermissionsGranted()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        initPermissionLauncher()
    }

    private fun initPermissionLauncher() {
        permissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (PermissionsUtil.arePermissionsGranted(this, requiredPermissions())) {
                onPermissionsGranted()
            } else {
                DialogUtil.showPermissionDeniedDialog(this, packageName)
            }
        }
    }

    protected fun checkAndRequestPermissions() {
        if (PermissionsUtil.arePermissionsGranted(this, requiredPermissions())) {
            onPermissionsGranted()
        } else {
            DialogUtil.showRationaleDialog(this, rationaleMessage()) {
                permissionsLauncher.launch(requiredPermissions())
            }
        }
    }


        protected fun attemptActionOrRequestPermissions(action: () -> Unit) {
            if (PermissionsUtil.arePermissionsGranted(this, requiredPermissions())) {
                action()
            } else {
                DialogUtil.showRationaleDialog(this, rationaleMessage()) {
                    permissionsLauncher.launch(requiredPermissions())
                }
            }
        }



    protected fun setupNavBar(defaultButtonId: Int) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.navbarContainer, NavBarFragment(this, defaultButtonId))
            .commit()
    }

    protected fun toggleFlash(flashButton: View) {
        isFlashEnabled = !isFlashEnabled
        (flashButton as? ImageButton)?.setImageResource(
            if (isFlashEnabled) R.drawable.ic_flash_state_on else R.drawable.ic_flash_state_off
        )
    }

    protected fun switchCamera(flashButton: ImageButton) {
        cameraSelector = CameraUtil.toggleCameraSelector(cameraSelector) { isFrontCamera ->
            flashButton.visibility = if (isFrontCamera) View.GONE else View.VISIBLE
        }
        onPermissionsGranted()
    }

    protected fun handleTouchEvent(view: View, event: MotionEvent): Boolean {
        camera?.let { CameraUtil.handleTouchEvent(event) }
        if (event.action == MotionEvent.ACTION_UP) {
            view.performClick()
        }
        return true
    }

    protected fun setupCamera(
        previewSurfaceProvider: Preview.SurfaceProvider,
        additionalUseCases: (ProcessCameraProvider) -> Unit
    ) {
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewSurfaceProvider
        }

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            CameraUtil.initPinchToZoom(this, camera!!)
            additionalUseCases(cameraProvider)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onGallerySelected() {
        navigateToActivity(GalleryActivity::class.java)
    }

    override fun onPhotoModeSelected() {
        navigateToActivity(MainActivity::class.java)
    }

    override fun onVideoModeSelected() {
        navigateToActivity(VideoActivity::class.java)
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        if (this::class.java != activityClass) {
            startActivity(Intent(this, activityClass))
            finish()
        }
    }
}
