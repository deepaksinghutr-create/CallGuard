package com.callguard.app

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.callguard.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs

    private val requiredPermissions = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.ANSWER_PHONE_CALLS
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (!allGranted) {
            Toast.makeText(
                this,
                "App ko sahi se kaam karne ke liye saari permissions allow karna zaroori hai",
                Toast.LENGTH_LONG
            ).show()
        }
        updateStatusText()
    }

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updateStatusText()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)

        binding.switchBlocking.isChecked = prefs.isBlockingEnabled
        binding.switchBlocking.setOnCheckedChangeListener { _, isChecked ->
            prefs.isBlockingEnabled = isChecked
            Toast.makeText(
                this,
                if (isChecked) "SIM 1 par unknown calls block honge"
                else "Sabhi calls allow rahengi",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnGrantPermissions.setOnClickListener { requestAllPermissions() }
        binding.btnSetDefaultScreeningApp.setOnClickListener { requestScreeningRole() }

        requestAllPermissions()
        requestScreeningRole()
    }

    override fun onResume() {
        super.onResume()
        updateStatusText()
    }

    private fun requestAllPermissions() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        }
    }

    private fun requestScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    roleLauncher.launch(intent)
                }
            }
        } else {
            Toast.makeText(
                this,
                "Settings > Apps > Default apps > Caller ID & spam app mein CallGuard select karein",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updateStatusText() {
        val permsGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        val roleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) ?: false
        } else {
            true
        }

        binding.textStatus.text = buildString {
            append(if (permsGranted) "✔ Permissions: OK\n" else "✘ Permissions: Missing (tap button below)\n")
            append(if (roleHeld) "✔ Default call screening app: Set" else "✘ Default call screening app: Not set (tap button below)")
        }
    }
}
