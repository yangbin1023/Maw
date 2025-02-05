package com.magic.maw.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.magic.maw.R
import com.magic.maw.data.PostData
import com.magic.maw.data.Quality
import kotlinx.atomicfu.atomic

private var id = atomic(1)

fun newNotificationId(): Int {
    return id.getAndAdd(1)
}

fun getNotificationChannelId(context: Context, postData: PostData, quality: Quality): String {
    return context.packageName + "@" + postData.source + ":" + quality.name
}

@SuppressLint("MissingPermission")
class ProgressNotification(
    private val context: Context,
    channelId: String,
    priority: Int = NotificationCompat.PRIORITY_HIGH
) {
    private val id = newNotificationId()
    private val managerCompat = NotificationManagerCompat.from(context)
    private val builder: NotificationCompat.Builder

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.download_progress),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            managerCompat.createNotificationChannel(channel)
        }
        builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(priority)
            .setProgress(100, 0, true)
    }

    fun update(progress: Int) {
        builder.setProgress(100, progress, false)
        builder.setContentTitle(context.getString(R.string.download_progress))
        builder.setContentText("$progress%")
        if (context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            managerCompat.notify(id, builder.build())
        }
    }

    fun finish(
        title: String? = context.getString(R.string.app_name),
        text: String? = null,
        iconUri: Uri? = null,
        uri: Uri? = null,
    ) {
        builder.setProgress(0, 0, false)
        builder.setContentTitle(title)
        builder.setContentText(text)
        builder.setAutoCancel(true)
        iconUri?.let {
            val iconCompat = IconCompat.createWithContentUri(it)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                builder.setSmallIcon(iconCompat)
            }
        }
        uri?.let {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setData(uri)
            val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_ONE_SHOT
            }
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, flag)
            builder.setContentIntent(pendingIntent)
        }
        if (context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            managerCompat.notify(id, builder.build())
        }
    }
}