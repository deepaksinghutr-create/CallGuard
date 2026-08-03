package com.callguard.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        private const val KEY_SERVICE_STATUS = "service_status"
        private const val KEY_NOTIF_TONE = "notification_tone_uri"

        private const val KEY_STATS_DATE = "stats_date"
        private const val KEY_BLOCKED_TODAY = "blocked_today"
        private const val KEY_ALLOWED_TODAY = "allowed_today"
        private const val KEY_SPAM_BLOCKED_TOTAL = "spam_blocked_total"
        private const val KEY_LAST_UPDATE_TIME = "last_update_time"

        private const val KEY_BLOCKED_CALLS_LOG = "blocked_calls_log"
        private const val KEY_WHITELIST = "whitelist_numbers"
    }

    // ---- Modes ----

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
        ALLOW_ALL -> "Allow all calls"
        CONTACTS_ONLY -> "Allow contacts only"
        BLOCK_ALL -> "Block all calls"
        else -> "Unknown"
    }

    fun modeSubtitle(mode: Int): String = when (mode) {
        ALLOW_ALL -> "Everyone can call through"
        CONTACTS_ONLY -> "Only calls from your contacts will be allowed"
        BLOCK_ALL -> "All incoming calls will be blocked"
        else -> ""
    }

    // ---- Ring detector ----

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

    // ---- Debug log ----

    fun logCall(number: String?, slot: Int, mode: Int, allowed: Boolean, diagnostics: String = "") {
        val time = timeNow()
        val slotLabel = slotLabel(slot)
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

    // ---- Crash reporting ----

    fun saveCrash(stackTrace: String) {
        sp.edit().putString(KEY_LAST_CRASH, stackTrace).apply()
    }

    fun getLastCrash(): String? = sp.getString(KEY_LAST_CRASH, null)

    fun clearCrash() {
        sp.edit().remove(KEY_LAST_CRASH).apply()
    }

    // ---- Service status ----

    fun setServiceStatus(status: String) {
        sp.edit().putString(KEY_SERVICE_STATUS, status).apply()
    }

    fun getServiceStatus(): String = sp.getString(KEY_SERVICE_STATUS, "Not started yet") ?: "Not started yet"

    // ---- Notification tone ----

    fun getNotificationToneUri(): String? = sp.getString(KEY_NOTIF_TONE, null)

    fun setNotificationToneUri(uri: String?) {
        sp.edit().putString(KEY_NOTIF_TONE, uri).apply()
    }

    // ---- Stats (Blocked Today / Allowed Today / Spam Blocked total / Last Update) ----

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun resetStatsIfNewDay() {
        val storedDate = sp.getString(KEY_STATS_DATE, "")
        if (storedDate != today()) {
            sp.edit()
                .putString(KEY_STATS_DATE, today())
                .putInt(KEY_BLOCKED_TODAY, 0)
                .putInt(KEY_ALLOWED_TODAY, 0)
                .apply()
        }
    }

    fun recordCallResult(allowed: Boolean) {
        resetStatsIfNewDay()
        if (allowed) {
            sp.edit().putInt(KEY_ALLOWED_TODAY, getAllowedToday() + 1).apply()
        } else {
            sp.edit()
                .putInt(KEY_BLOCKED_TODAY, getBlockedToday() + 1)
                .putInt(KEY_SPAM_BLOCKED_TOTAL, getSpamBlockedTotal() + 1)
                .apply()
        }
        sp.edit().putString(KEY_LAST_UPDATE_TIME, timeNow()).apply()
    }

    fun getBlockedToday(): Int {
        resetStatsIfNewDay()
        return sp.getInt(KEY_BLOCKED_TODAY, 0)
    }

    fun getAllowedToday(): Int {
        resetStatsIfNewDay()
        return sp.getInt(KEY_ALLOWED_TODAY, 0)
    }

    fun getSpamBlockedTotal(): Int = sp.getInt(KEY_SPAM_BLOCKED_TOTAL, 0)

    fun getLastUpdateTime(): String = sp.getString(KEY_LAST_UPDATE_TIME, "--:--") ?: "--:--"

    // ---- Blocked calls list (for "View Blocked Calls" screen) ----

    private fun slotLabel(slot: Int): String = when (slot) {
        0 -> "SIM 1"
        1 -> "SIM 2"
        else -> "Unknown"
    }

    fun recordBlockedCall(number: String?, slot: Int) {
        val entry = "${timeNow()}|${slotLabel(slot)}|${number ?: "Unknown number"}"
        val existing = sp.getString(KEY_BLOCKED_CALLS_LOG, "") ?: ""
        val lines = (listOf(entry) + existing.split("\n").filter { it.isNotBlank() }).take(50)
        sp.edit().putString(KEY_BLOCKED_CALLS_LOG, lines.joinToString("\n")).apply()
    }

    fun getBlockedCalls(slotFilter: Int? = null): List<Triple<String, String, String>> {
        val raw = sp.getString(KEY_BLOCKED_CALLS_LOG, "") ?: ""
        val all = raw.split("\n").filter { it.isNotBlank() }.mapNotNull {
            val parts = it.split("|")
            if (parts.size == 3) Triple(parts[0], parts[1], parts[2]) else null
        }
        return if (slotFilter == null) all else all.filter { it.second == slotLabel(slotFilter) }
    }

    // ---- Whitelist (extra allowed numbers beyond Contacts) ----

    fun getWhitelist(): List<String> {
        val raw = sp.getString(KEY_WHITELIST, "") ?: ""
        return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    fun addToWhitelist(number: String) {
        val current = getWhitelist().toMutableList()
        val cleaned = number.trim()
        if (cleaned.isNotBlank() && !current.contains(cleaned)) {
            current.add(cleaned)
            sp.edit().putString(KEY_WHITELIST, current.joinToString(",")).apply()
        }
    }

    fun removeFromWhitelist(number: String) {
        val current = getWhitelist().toMutableList()
        current.remove(number)
        sp.edit().putString(KEY_WHITELIST, current.joinToString(",")).apply()
    }

    fun isWhitelisted(number: String): Boolean {
        return getWhitelist().any { number.endsWith(it) || it.endsWith(number) }
    }

    // ---- Welcome screen ----

    fun hasSeenWelcome(): Boolean = sp.getBoolean("has_seen_welcome", false)

    fun setWelcomeSeen() {
        sp.edit().putBoolean("has_seen_welcome", true).apply()
    }

    private fun timeNow(): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
}
