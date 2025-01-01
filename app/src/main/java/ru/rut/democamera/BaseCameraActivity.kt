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
        PermissionsUtil.handlePermissions(
            this,
            requiredPermissions(),
            permissionsLauncher,
            rationaleMessage(),
            ::onPermissionsGranted
        ) { message, onConfirm ->
            DialogUtil.showRationaleDialog(this, message, onConfirm)
        }
    }

    protected fun setupNavBar(defaultButtonId: Int) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.navbarContainer, NavBarFragment(this, defaultButtonId))
            .commit()
    }

    protected fun toggleFlash(flashButton: View) {
        val flashOnIcon = R.drawable.ic_flash_state_on
        val flashOffIcon = R.drawable.ic_flash_state_off

        isFlashEnabled = !isFlashEnabled
        (flashButton as? ImageButton)?.setImageResource(
            if (isFlashEnabled) flashOnIcon else flashOffIcon
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

    override fun onGallerySelected() {
        startActivity(Intent(this, GalleryActivity::class.java))
        finish()
    }

    override fun onPhotoModeSelected() {
        if (this !is MainActivity) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onVideoModeSelected() {
        if (this !is VideoActivity) {
            startActivity(Intent(this, VideoActivity::class.java))
            finish()
        }
    }
}
