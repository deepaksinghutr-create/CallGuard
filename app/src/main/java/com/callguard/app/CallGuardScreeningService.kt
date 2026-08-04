package com.callguard.app

import android.database.Cursor
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.CallScreeningService.CallResponse
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log

class CallGuardScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "CallGuardService"
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val prefs = Prefs(applicationContext)
        val number = callDetails.handle?.schemeSpecificPart
        val diagnostics = StringBuilder()

        var simSlot = prefs.getRecentRingSlot() ?: -1
        if (simSlot >= 0) {
            diagnostics.append("source=ringDetector slot=$simSlot")
        } else {
            simSlot = resolveSimSlotFallback(callDetails, diagnostics)
        }

        val mode = prefs.modeForSlot(simSlot)
        val allowed = when (mode) {
            Prefs.ALLOW_ALL -> true
            Prefs.BLOCK_ALL -> false
            Prefs.CONTACTS_ONLY -> number != null &&
                    (isNumberInContacts(number) || prefs.isWhitelisted(number))
            else -> true
        }

        prefs.logCall(number, simSlot, mode, allowed, diagnostics.toString())
        prefs.recordCallResult(allowed)

        if (allowed) {
            respondAllow(callDetails)
        } else {
            respondBlock(callDetails)
            prefs.recordBlockedCall(number, simSlot)
            val simLabel = when (simSlot) {
                0 -> "SIM 1"
                1 -> "SIM 2"
                else -> "Unknown SIM"
            }
            NotificationHelper.showBlockedCallNotification(applicationContext, number, simLabel)
        }
    }

    private fun resolveSimSlotFallback(callDetails: Call.Details, diag: StringBuilder): Int {
        return try {
            val handle = callDetails.accountHandle ?: run {
                diag.append("source=accountHandle handle=NULL")
                return -1
            }
            val telephonyManager = getSystemService(TelephonyManager::class.java)
            val subscriptionManager = getSystemService(SubscriptionManager::class.java)

            if (telephonyManager != null && subscriptionManager != null) {
                @Suppress("MissingPermission")
                val subId = telephonyManager.getSubscriptionId(handle)
                if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    @Suppress("MissingPermission")
                    val info = subscriptionManager.getActiveSubscriptionInfo(subId)
                    if (info != null) {
                        diag.append("source=accountHandle slot=${info.simSlotIndex}")
                        return info.simSlotIndex
                    }
                }
            }
            diag.append("source=accountHandle -> UNRESOLVED")
            -1
        } catch (e: Exception) {
            diag.append("source=accountHandle EXC=${e.javaClass.simpleName}")
            -1
        }
    }

    private fun isNumberInContacts(number: String): Boolean {
        return try {
            val uri = android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(number)
            )
            val cursor: Cursor? = contentResolver.query(
                uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null
            )
            cursor?.use { it.moveToFirst() } ?: false
        } catch (e: SecurityException) {
            false
        }
    }

    private fun respondAllow(callDetails: Call.Details) {
        respondToCall(
            callDetails,
            CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
        )
    }

    private fun respondBlock(callDetails: Call.Details) {
        respondToCall(
            callDetails,
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
        )
    }
}
