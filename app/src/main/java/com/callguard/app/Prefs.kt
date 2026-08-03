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
        private const val KEY_LAST_RING_SLOT = "last_ring_slot"
        private const val KEY_LAST_RING_TIME = "last_ring_time"
        private const val KEY_LAST_CRASH = "last_crash"
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

    fun recordRinging(slot: Int, number: String?) {
        sp.edit()
            .putInt(KEY_LAST_RING_SLOT, slot)
            .putLong(KEY_LAST_RING_TIME, System.currentTimeMillis())
            .apply()
    }

    fun getRecentRingSlot(maxAgeMs: Long = 6000): Int? {
        val time = sp.getLong(KEY_LAST_RING_TIME, 0)
        if (System.currentTimeMillis() - time > maxAgeMs) return null
        val slot = sp.getInt(KEY_LAST_RING_SLOT, -1)
        return if (slot >= 0) slot else null
    }

    fun logCall(number: String?, slot: Int, mode: Int, allowed: Boolean, diagnostics: String = "") {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val slotLabel = when (slot) {
            0 -> "SIM 1"
            1 -> "SIM 2"
            else -> "Unknown slot"
        }
        val entry = "$time | ${number ?: "unknown"} | $slotLabel | rule: ${modeLabel(mode)} | " +
                (if (allowed) "ALLOWED" else "BLOCKED") + "\n  diag: $diagnostics"

        val existing = sp.getString(KEY_DEBUG_LOG, "") ?: ""
        val lines = (listOf(entry) + existing.split("\n\n").filter { it.isNotBlank() }).take(10)
        sp.edit().putString(KEY_DEBUG_LOG, lines.joinToString("\n\n")).apply()
    }

    fun getDebugLog(): String {
        val log = sp.getString(KEY_DEBUG_LOG, "") ?: ""
        return log.ifBlank { "No calls screened yet." }
    }

    fun saveCrash(stackTrace: String) {
        sp.edit().putString(KEY_LAST_CRASH, stackTrace).apply()
    }

    fun getLastCrash(): String? = sp.getString(KEY_LAST_CRASH, null)

    fun clearCrash() {
        sp.edit().remove(KEY_LAST_CRASH).apply()
    }
    fun setServiceStatus(status: String) {
        sp.edit().putString("service_status", status).apply()
    }

    fun getServiceStatus(): String = sp.getString("service_status", "Not started yet") ?: "Not started yet"
    fun hasSeenWelcome(): Boolean = sp.getBoolean("has_seen_welcome", false)

    fun setWelcomeSeen() {
        sp.edit().putBoolean("has_seen_welcome", true).apply()
    }
    fun getNotificationToneUri(): String? = sp.getString("notification_tone_uri", null)

    fun setNotificationToneUri(uri: String?) {
        sp.edit().putString("notification_tone_uri", uri).apply()
    }
}
