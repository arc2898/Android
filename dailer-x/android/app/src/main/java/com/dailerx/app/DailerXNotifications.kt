package com.dailerx.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

internal object DailerXNotifications {
    const val CHANNEL_ID = "dailerx_caller_alerts"
    private const val CALLER_NOTIFICATION_ID = 4701
    private const val STATUS_NOTIFICATION_ID = 4702

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Caller ID and spam alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Caller identification, spam warnings, and Dailer X activity alerts"
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        manager.createNotificationChannel(channel)
    }

    fun postStatus(context: Context, title: String, message: String) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        createChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, STATUS_NOTIFICATION_ID, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setNumber(1)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .build()
        runCatching { manager.notify(STATUS_NOTIFICATION_ID, notification) }
    }

    fun postCallerAlert(context: Context, number: String, callerName: String?, spamReports: Int, blocked: Boolean) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        createChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("caller_number", number)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, number.hashCode(), launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = when {
            blocked -> "Dailer X blocked a reported spam call"
            spamReports >= 5 -> "Potential spam caller"
            !callerName.isNullOrBlank() -> callerName
            else -> "Unknown caller"
        }
        val detail = buildString {
            append(number)
            if (spamReports > 0) append(" · $spamReports community reports")
            if (blocked) append(" · blocked by your protection rules")
        }
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(detail)
            .setStyle(Notification.BigTextStyle().bigText(detail))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setNumber(1)
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .build()
        runCatching { manager.notify(CALLER_NOTIFICATION_ID, notification) }
    }
}
