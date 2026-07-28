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

        val simSlot = resolveSimSlot(callDetails, diagnostics)
        val mode = prefs.modeForSlot(simSlot)

        val allowed = when (mode) {
            Prefs.ALLOW_ALL -> true
            Prefs.BLOCK_ALL -> false
            Prefs.CONTACTS_ONLY -> number != null && isNumberInContacts(number)
            else -> true
        }

        prefs.logCall(number, simSlot, mode, allowed, diagnostics.toString())

        if (allowed) respondAllow(callDetails) else respondBlock(callDetails)
    }

    private fun resolveSimSlot(callDetails: Call.Details, diag: StringBuilder): Int {
        return try {
            val handle = callDetails.accountHandle
            if (handle == null) {
                diag.append("handle=NULL")
                return -1
            }
            diag.append("component=${handle.componentName?.shortClassName} id=${handle.id}")

            val telephonyManager = getSystemService(TelephonyManager::class.java)
            val subscriptionManager = getSystemService(SubscriptionManager::class.java)

            if (telephonyManager != null) {
                @Suppress("MissingPermission")
                val subId = try {
                    telephonyManager.getSubscriptionId(handle)
                } catch (e: Exception) {
                    diag.append(" subIdErr=${e.javaClass.simpleName}")
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID
                }
                diag.append(" subId=$subId")

                if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID && subscriptionManager != null) {
                    @Suppress("MissingPermission")
                    val info = subscriptionManager.getActiveSubscriptionInfo(subId)
                    if (info != null) {
                        diag.append(" -> slot=${info.simSlotIndex}")
                        return info.simSlotIndex
                    }
                }
            }

            if (subscriptionManager != null) {
                @Suppress("MissingPermission")
                val activeSubs = subscriptionManager.activeSubscriptionInfoList
                diag.append(" activeSubsCount=${activeSubs?.size ?: 0}")
                activeSubs?.forEach {
                    diag.append(" [slot${it.simSlotIndex}:iccTail=${it.iccId?.takeLast(6)}]")
                }
            }

            diag.append(" -> UNRESOLVED")
            -1
        } catch (e: Exception) {
            diag.append(" EXC=${e.javaClass.simpleName}:${e.message}")
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
