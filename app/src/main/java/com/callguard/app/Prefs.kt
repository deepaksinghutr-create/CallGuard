package com.callguard.app

import android.content.Context

/** Simple SharedPreferences wrapper storing the user's toggle state. */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("callguard_prefs", Context.MODE_PRIVATE)

    var isBlockingEnabled: Boolean
        get() = sp.getBoolean(KEY_BLOCKING_ENABLED, false)
        set(value) = sp.edit().putBoolean(KEY_BLOCKING_ENABLED, value).apply()

    companion object {
        private const val KEY_BLOCKING_ENABLED = "blocking_enabled"
    }
}
