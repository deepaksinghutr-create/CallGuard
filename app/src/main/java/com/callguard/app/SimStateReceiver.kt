package com.callguard.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log

class SimStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (state != TelephonyManager.EXTRA_STATE_RINGING) return

            // On many dual-SIM phones, this extra tells us which subscription is ringing
            val subId = intent.getIntExtra("subscription", -1)
                .takeIf { it != -1 }
                ?: intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", -1)

            if (subId == -1) {
                Log.d("SimStateReceiver", "Ringing but no subscription extra found")
                return
            }

            val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
            @Suppress("MissingPermission")
            val info = subscriptionManager?.getActiveSubscriptionInfo(subId)
            val slot = info?.simSlotIndex ?: -1

            Log.d("SimStateReceiver", "Ringing on subId=$subId slot=$slot")
            RingingSimTracker.markRinging(slot)

        } catch (e: Exception) {
            Log.w("SimStateReceiver", "Error reading ringing SIM", e)
        }
    }
}
