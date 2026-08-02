package com.callguard.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = Prefs(this)
        if (prefs.hasSeenWelcome()) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_welcome)

        findViewById<android.widget.Button>(R.id.btnGetStarted).setOnClickListener {
            prefs.setWelcomeSeen()
            goToMain()
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
