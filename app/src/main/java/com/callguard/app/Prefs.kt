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
}
