package com.callguard.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WhitelistActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whitelist)

        prefs = Prefs(this)
        container = findViewById(R.id.containerWhitelist)

        val editNumber = findViewById<EditText>(R.id.editWhitelistNumber)
        findViewById<Button>(R.id.btnAddWhitelist).setOnClickListener {
            val number = editNumber.text.toString().trim()
            if (number.isNotBlank()) {
                prefs.addToWhitelist(number)
                editNumber.setText("")
                refreshList()
            }
        }

        refreshList()
    }

    private fun refreshList() {
        container.removeAllViews()
        val numbers = prefs.getWhitelist()

        if (numbers.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "No whitelisted numbers yet."
                setTextColor(0xFFAAAAAA.toInt())
                textSize = 14f
            }
            container.addView(emptyView)
            return
        }

        for (number in numbers) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundResource(R.drawable.bg_card)
                setPadding(20, 20, 20, 20)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            val numberView = TextView(this).apply {
                text = number
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val removeBtn = Button(this).apply {
                text = "Remove"
                setOnClickListener {
                    prefs.removeFromWhitelist(number)
                    refreshList()
                }
            }
            row.addView(numberView)
            row.addView(removeBtn)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = 12
            container.addView(row, params)
        }
    }
}
