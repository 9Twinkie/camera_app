package ru.rut.democamera.utils

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog

object DialogUtil {

    fun showRationaleDialog(context: Context, message: String, onGrant: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Permissions Required")
            .setMessage(message)
            .setPositiveButton("Grant") { dialogInterface, button -> onGrant() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showPermissionDeniedDialog(context: Context, packageName: String) {
        AlertDialog.Builder(context)
            .setTitle("Permission Denied")
            .setMessage("Camera access is required. Enable it in app settings.")
            .setPositiveButton("Settings") { dialogInterface, button ->
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
