package ru.rut.democamera.utils

import android.content.Context
import android.view.OrientationEventListener
import android.view.Surface

object OrientationUtil {

    private var currentOrientation = Surface.ROTATION_0
    private var orientationEventListener: OrientationEventListener? = null

    fun getSurfaceRotation(orientationDegrees: Int): Int {
        return when (orientationDegrees) {
            in 45..134 -> Surface.ROTATION_270
            in 135..224 -> Surface.ROTATION_180
            in 225..314 -> Surface.ROTATION_90
            else -> Surface.ROTATION_0
        }
    }

    fun initOrientationListener(context: Context, onOrientationChanged: (Int) -> Unit) {
        orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return

                val rotation = getSurfaceRotation(orientation)
                if (rotation != currentOrientation) {
                    currentOrientation = rotation
                    onOrientationChanged(rotation)
                }
            }
        }
        orientationEventListener?.enable()
    }

    fun disableOrientationListener() {
        orientationEventListener?.disable()
        orientationEventListener = null
    }

    fun getCurrentRotation(): Int {
        return currentOrientation
    }
}