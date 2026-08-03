package com.callguard.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    const val CHANNEL_ID = "callguard_blocked_calls"
    private var notificationCounter = 2000

    private fun resolveSoundUri(context: Context): Uri {
        val stored = Prefs(context).getNotificationToneUri()
        return if (!stored.isNullOrBlank()) {
            Uri.parse(stored)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    fun ensureChannelExists(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            createChannel(context, resolveSoundUri(context))
        }
    }

    fun updateChannelSound(context: Context, soundUri: Uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.deleteNotificationChannel(CHANNEL_ID)
        createChannel(context, soundUri)
    }

    private fun createChannel(context: Context, soundUri: Uri) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID, "Blocked calls", NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifies you whenever CallGuard blocks an incoming call"
            setSound(soundUri, audioAttributes)
            enableVibration(true)
        }
        nm.createNotificationChannel(channel)
    }

    fun showBlockedCallNotification(context: Context, number: String?, simLabel: String) {
        ensureChannelExists(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Call blocked")
            .setContentText("${number ?: "Unknown number"} was blocked on $simLabel")
            .setSmallIcon(android.R.drawable.stat_notify_call_mute)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationCounter++, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted, skip silently
        }
    }
}
