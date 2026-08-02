package com.callguard.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log

class RingDetectorService : Service() {

    private val listeners = mutableListOf<Pair<TelephonyManager, PhoneStateListener>>()

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        registerListeners()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (listeners.isEmpty()) registerListeners()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        unregisterListeners()
    }

    private fun startForegroundWithNotification() {
        val channelId = "callguard_ring_detector"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "CallGuard monitoring",
                NotificationManager.IMPORTANCE_MIN
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("CallGuard is active")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(Notification.PRIORITY_MIN)
            .build()
        startForeground(101, notification)
    }

    @Suppress("DEPRECATION", "MissingPermission")
    private fun registerListeners() {
        try {
            unregisterListeners()
            val subscriptionManager = getSystemService(SubscriptionManager::class.java) ?: return
            val activeSubs = subscriptionManager.activeSubscriptionInfoList ?: return
            val baseTelephonyManager = getSystemService(TelephonyManager::class.java) ?: return

            for (sub in activeSubs) {
                val subId = sub.subscriptionId
                val slot = sub.simSlotIndex
                val tmForSub = baseTelephonyManager.createForSubscriptionId(subId)

                val listener = object : PhoneStateListener() {
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        if (state == TelephonyManager.CALL_STATE_RINGING) {
                            Log.d("RingDetector", "Ringing on slot=$slot subId=$subId")
                            Prefs(applicationContext).recordRinging(slot, phoneNumber)
                        }
                    }
                }
                tmForSub.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                listeners.add(tmForSub to listener)
            }
        } catch (e: Exception) {
            Log.w("RingDetector", "Failed to register listeners", e)
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
