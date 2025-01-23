package com.magic.maw.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

val needStoragePermission = Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
val needNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

fun Context.hasPermission(permission: String): Boolean {
    return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}