package ru.rut.democamera.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

object PermissionsUtil {

    val PHOTO_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA
    )
    val VIDEO_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    fun arePermissionsGranted(context: Context, permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getMissingPermissions(context: Context, permissions: Array<String>): List<String> {
        return permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun showRationaleDialog(
        context: Context,
        rationaleMessage: String,
        onGrant: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle("Permissions Required")
            .setMessage(rationaleMessage)
            .setPositiveButton("Grant") { _, _ -> onGrant() }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
