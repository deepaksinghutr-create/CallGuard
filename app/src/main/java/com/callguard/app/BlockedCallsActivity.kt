package com.callguard.app

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BlockedCallsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked_calls)

        val slotFilter = intent.getIntExtra("slot", -2).let { if (it == -2) null else it }
        val prefs = Prefs(this)
        val calls = prefs.getBlockedCalls(slotFilter)
        val container = findViewById<LinearLayout>(R.id.containerBlockedCalls)

        if (calls.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No blocked calls yet."
                setTextColor(0xFFAAAAAA.toInt())
                textSize = 14f
            }
            container.addView(empty)
            return
        }

        for ((time, simLabel, number) in calls) {
            val row = TextView(this).apply {
                text = "$time  •  $simLabel\n$number"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 14f
                setPadding(20, 20, 20, 20)
                setBackgroundResource(R.drawable.bg_card)
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = 12
            container.addView(row, params)
        }
    }
}
