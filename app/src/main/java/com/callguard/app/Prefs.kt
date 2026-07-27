package com.callguard.app

import android.content.Context

class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("callguard_prefs", Context.MODE_PRIVATE)

    companion object {
        const val ALLOW_ALL = 0
        const val CONTACTS_ONLY = 1
        const val BLOCK_ALL = 2

        private const val KEY_SIM1_MODE = "sim1_mode"
        private const val KEY_SIM2_MODE = "sim2_mode"
        private const val KEY_FALLBACK_MODE = "fallback_mode"
        private const val KEY_DEBUG_LOG = "debug_log"
    }

    var sim1Mode: Int
        get() = sp.getInt(KEY_SIM1_MODE, CONTACTS_ONLY)
        set(value) = sp.edit().putInt(KEY_SIM1_MODE, value).apply()

    var sim2Mode: Int
        get() = sp.getInt(KEY_SIM2_MODE, ALLOW_ALL)
        set(value) = sp.edit().putInt(KEY_SIM2_MODE, value).apply()

    var fallbackMode: Int
        get() = sp.getInt(KEY_FALLBACK_MODE, ALLOW_ALL)
        set(value) = sp.edit().putInt(KEY_FALLBACK_MODE, value).apply()

    fun modeForSlot(slot: Int): Int = when (slot) {
        0 -> sim1Mode
        1 -> sim2Mode
        else -> fallbackMode
    }

    fun modeLabel(mode: Int): String = when (mode) {
        ALLOW_ALL -> "Allow all"
        CONTACTS_ONLY -> "Contacts only"
        BLOCK_ALL -> "Block all"
        else -> "Unknown"
    }

    fun logCall(number: String?, slot: Int, mode: Int, allowed: Boolean, rawHandleId: String?) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val slotLabel = when (slot) {
            0 -> "SIM 1"
            1 -> "SIM 2"
            else -> "Unknown slot"
        }
        val entry = "$time | ${number ?: "unknown"} | $slotLabel | rawId: ${rawHandleId ?: "null"} | rule: ${modeLabel(mode)} | " +
                if (allowed) "ALLOWED" else "BLOCKED"

        val existing = sp.getString(KEY_DEBUG_LOG, "") ?: ""
        val lines = (listOf(entry) + existing.split("\n").filter { it.isNotBlank() }).take(15)
        sp.edit().putString(KEY_DEBUG_LOG, lines.joinToString("\n")).apply()
    }

    fun getDebugLog(): String {
        val log = sp.getString(KEY_DEBUG_LOG, "") ?: ""
        return log.ifBlank { "No calls screened yet." }
    }
}
