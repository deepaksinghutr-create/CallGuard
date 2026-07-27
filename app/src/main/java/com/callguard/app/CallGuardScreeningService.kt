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
        val rawHandleId = callDetails.accountHandle?.id
        val extrasDump = dumpExtras(callDetails)
        val simSlot = resolveSimSlot(callDetails)
        val mode = prefs.modeForSlot(simSlot)

        Log.d(TAG, "Incoming call from $number simSlot=$simSlot mode=$mode rawHandleId=$rawHandleId extras=$extrasDump")

        val allowed = when (mode) {
            Prefs.ALLOW_ALL -> true
            Prefs.BLOCK_ALL -> false
            Prefs.CONTACTS_ONLY -> number != null && isNumberInContacts(number)
            else -> true
        }

        prefs.logCall(number, simSlot, mode, allowed, rawHandleId, extrasDump)

        if (allowed) respondAllow(callDetails) else respondBlock(callDetails)
    }

    private fun dumpExtras(callDetails: Call.Details): String {
        return try {
            val extras = callDetails.extras
            if (extras == null || extras.isEmpty) return "none"
            extras.keySet().joinToString(", ") { key ->
                "$key=${extras.get(key)}"
            }
        } catch (e: Exception) {
            "error: ${e.message}"
        }
    }

    private fun resolveSimSlot(callDetails: Call.Details): Int {
        val handle = callDetails.accountHandle
        if (handle != null) {
            val subscriptionManager = getSystemService(SubscriptionManager::class.java)
            val idAsSubId = handle.id.toIntOrNull()
            if (idAsSubId != null && subscriptionManager != null) {
                try {
                    @Suppress("MissingPermission")
                    val info = subscriptionManager.getActiveSubscriptionInfo(idAsSubId)
                    if (info != null) return info.simSlotIndex
                } catch (e: SecurityException) {
                    Log.w(TAG, "No permission reading subscription info via handle.id", e)
                }
            }
        }

        return RingingSimTracker.getRecentSlotOrUnknown()
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
