package com.callguard.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

class RingDetectorService : Service() {

    private val listeners = mutableListOf<Pair<TelephonyManager, PhoneStateListener>>()

    override fun onCreate() {
        super.onCreate()
        try {
            startForegroundWithNotification()
            Prefs(applicationContext).setServiceStatus("Started OK at ${nowTime()}")
            registerListeners()
        } catch (e: Exception) {
            Log.w("RingDetector", "Failed to start service", e)
            Prefs(applicationContext).setServiceStatus("FAILED to start: ${e.javaClass.simpleName}: ${e.message}")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (listeners.isEmpty()) {
            try {
                registerListeners()
            } catch (e: Exception) {
                Prefs(applicationContext).setServiceStatus("Listener registration failed: ${e.message}")
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Prefs(applicationContext).setServiceStatus("Stopped at ${nowTime()}")
        unregisterListeners()
    }

    private fun nowTime(): String =
        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

    private fun startForegroundWithNotification() {
        val channelId = "callguard_ring_detector"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "CallGuard monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("CallGuard is active")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(Notification.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(101, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(101, notification)
        }
    }

    @Suppress("DEPRECATION", "MissingPermission")
    private fun registerListeners() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Prefs(applicationContext).setServiceStatus("Missing READ_PHONE_STATE permission")
            return
        }

        try {
            unregisterListeners()
            val subscriptionManager = getSystemService(SubscriptionManager::class.java)
            if (subscriptionManager == null) {
                Prefs(applicationContext).setServiceStatus("SubscriptionManager unavailable")
                return
            }
            val activeSubs = subscriptionManager.activeSubscriptionInfoList
            if (activeSubs == null) {
                Prefs(applicationContext).setServiceStatus("activeSubscriptionInfoList is null (permission or no SIMs)")
                return
            }
            val baseTelephonyManager = getSystemService(TelephonyManager::class.java)
            if (baseTelephonyManager == null) {
                Prefs(applicationContext).setServiceStatus("TelephonyManager unavailable")
                return
            }

            for (sub in activeSubs) {
                val subId = sub.subscriptionId
                val slot = sub.simSlotIndex
                val tmForSub = baseTelephonyManager.createForSubscriptionId(subId)

                val listener = object : PhoneStateListener() {
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        if (state == TelephonyManager.CALL_STATE_RINGING) {
                            Log.d("RingDetector", "Ringing on slot=$slot subId=$subId")
                            Prefs(applicationContext).recordRinging(slot, phoneNumber)
                            Prefs(applicationContext).setServiceStatus("Last ring detected: slot=$slot at ${nowTime()}")
                        }
                    }
                }
                tmForSub.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                listeners.add(tmForSub to listener)
            }
            Prefs(applicationContext).setServiceStatus("Listening on ${activeSubs.size} SIM(s), registered at ${nowTime()}")
        } catch (e: Exception) {
            Prefs(applicationContext).setServiceStatus("Listener error: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    private fun unregisterListeners() {
        listeners.forEach { (tm, listener) ->
            try { tm.listen(listener, PhoneStateListener.LISTEN_NONE) } catch (_: Exception) {}
        }
        listeners.clear()
    }
}
