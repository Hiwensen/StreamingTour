package com.shaowei.streaming

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Helper class for handling dangerous permissions for Android API level >= 23 which
 * requires user consent at runtime to access the camera.
 */
const val RC_PERMISSION_REQUEST = 9222

fun hasReadStoragePermission(context: Context) = ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.READ_EXTERNAL_STORAGE
) == PackageManager.PERMISSION_GRANTED

fun hasCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
}

fun hasWriteStoragePermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
}

fun requestCameraPermission(activity: Activity, requestWritePermission: Boolean) {
    val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        Manifest.permission.CAMERA
    ) || requestWritePermission &&
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
    if (showRationale) {
        Toast.makeText(
            activity,
            "Camera permission is needed to run this application", Toast.LENGTH_LONG
        ).show()
    } else {

        // No explanation needed, we can request the permission.
        val permissions =
            if (requestWritePermission) arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) else arrayOf(Manifest.permission.CAMERA)
        ActivityCompat.requestPermissions(
            activity,
            permissions,
            RC_PERMISSION_REQUEST
        )
    }
}

fun requestWriteStoragePermission(activity: Activity) {
    val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    if (showRationale) {
        Toast.makeText(
            activity,
            "Writing to external storage permission is needed to run this application",
            Toast.LENGTH_LONG
        ).show()
    } else {

        // No explanation needed, we can request the permission.
        val permissions =
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ActivityCompat.requestPermissions(
            activity,
            permissions,
            RC_PERMISSION_REQUEST
        )
    }
}

fun requestReadWriteStoragePermission(activity: Activity) {
    val permissions =
        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)

    val showWriteRationale =
        ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    val showReadRationale =
        ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE)

    if (showWriteRationale || showReadRationale) {
        Toast.makeText(
            activity,
            "Reading and Writing to external storage permission is needed to run this application",
            Toast.LENGTH_LONG
        ).show()
    } else {
        // No explanation needed, we can request the permission.
        ActivityCompat.requestPermissions(
            activity,
            permissions,
            RC_PERMISSION_REQUEST
        )
    }
}


/**
 * Launch Application Setting to grant permission.
 */
fun launchPermissionSettings(activity: Activity) {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    intent.data = Uri.fromParts("package", activity.packageName, null)
    activity.startActivity(intent)
}
