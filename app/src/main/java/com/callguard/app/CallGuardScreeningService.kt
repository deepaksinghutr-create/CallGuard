package com.callguard.app

import android.database.Cursor
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.CallScreeningService.CallResponse
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log

/**
 * This service is invoked by Android for every incoming call once this app
 * is set as the device's default "Call Screening" app.
 *
 * Logic:
 *  - Figure out which physical SIM slot the call arrived on (0 = SIM1, 1 = SIM2)
 *  - If it's SIM2 -> always allow
 *  - If it's SIM1 -> allow only if the number exists in Contacts, OR if the
 *    user has turned the blocking feature OFF in the app
 */
class CallGuardScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "CallGuardService"
    }

   override fun onScreenCall(callDetails: Call.Details) {
        val prefs = Prefs(applicationContext)
        val number = callDetails.handle?.schemeSpecificPart
        val simSlot = resolveSimSlot(callDetails)
        val mode = prefs.modeForSlot(simSlot)

        Log.d(TAG, "Incoming call from $number on simSlot=$simSlot mode=$mode")

        when (mode) {
            Prefs.ALLOW_ALL -> respondAllow(callDetails)
            Prefs.BLOCK_ALL -> respondBlock(callDetails)
            Prefs.CONTACTS_ONLY -> {
                if (number != null && isNumberInContacts(number)) {
                    respondAllow(callDetails)
                } else {
                    respondBlock(callDetails)
                }
            }
            else -> respondAllow(callDetails)
        }
    }
   
    /** Returns the sim slot index (0 or 1) for the PhoneAccountHandle attached to this call, or -1 if unknown. */
    private fun resolveSimSlot(callDetails: Call.Details): Int {
        return try {
            val handle = callDetails.accountHandle ?: return -1
            val subscriptionManager =
                getSystemService(SubscriptionManager::class.java) ?: return -1
            val telephonyManager =
                getSystemService(TelephonyManager::class.java) ?: return -1

            @Suppress("MissingPermission")
            val subId = telephonyManager.getSubscriptionId(handle)
            @Suppress("MissingPermission")
            val info = subscriptionManager.getActiveSubscriptionInfo(subId)
            info?.simSlotIndex ?: -1
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing permission to resolve SIM slot", e)
            -1
        } catch (e: Exception) {
            Log.w(TAG, "Could not resolve SIM slot", e)
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
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null, null, null
            )
            cursor?.use { it.moveToFirst() } ?: false
        } catch (e: SecurityException) {
            // No contacts permission -> treat as unknown to be safe
            false
        }
    }

    private fun respondAllow(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, response)
    }

    private fun respondBlock(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, response)
    }
}
