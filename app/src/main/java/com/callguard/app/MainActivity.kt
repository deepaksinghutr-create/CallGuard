package com.callguard.app

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
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
        Manifest.permission.ANSWER_PHONE_CALLS,
        Manifest.permission.READ_PHONE_NUMBERS
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (!results.values.all { it }) {
            Toast.makeText(this, "All permissions must be allowed for this app to work", Toast.LENGTH_LONG).show()
        }
        startRingDetectorIfReady()
        refresh()
    }

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)

        val lastCrash = prefs.getLastCrash()
        if (lastCrash != null) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Last crash details")
                .setMessage(lastCrash)
                .setPositiveButton("OK") { _, _ -> prefs.clearCrash() }
                .setCancelable(false)
                .show()
        }

        setupSpinner(binding.spinnerSim1, prefs.sim1Mode) { prefs.sim1Mode = it }
        setupSpinner(binding.spinnerSim2, prefs.sim2Mode) { prefs.sim2Mode = it }
        setupSpinner(binding.spinnerFallback, prefs.fallbackMode) { prefs.fallbackMode = it }

        binding.btnGrantPermissions.setOnClickListener { requestAllPermissions() }
        binding.btnSetDefaultScreeningApp.setOnClickListener { requestScreeningRole() }

        binding.tabSim1.setOnClickListener { showPage(0) }
        binding.tabSim2.setOnClickListener { showPage(1) }
        binding.tabSettings.setOnClickListener { showPage(2) }

        requestAllPermissions()
        requestScreeningRole()
        startRingDetectorIfReady()
        showPage(0)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        updateStatusText()
        updateSimNumbers()
        binding.textDebugLog.text = prefs.getDebugLog()
    }

    private fun showPage(index: Int) {
        binding.pageSim1.visibility = if (index == 0) View.VISIBLE else View.GONE
        binding.pageSim2.visibility = if (index == 1) View.VISIBLE else View.GONE
        binding.pageSettings.visibility = if (index == 2) View.VISIBLE else View.GONE

        highlightTab(binding.tabSim1, index == 0)
        highlightTab(binding.tabSim2, index == 1)
        highlightTab(binding.tabSettings, index == 2)
    }

    private fun highlightTab(tab: LinearLayout, selected: Boolean) {
        tab.setBackgroundResource(if (selected) R.drawable.bg_tab_selected else 0)
    }

    private fun setupSpinner(spinner: Spinner, currentMode: Int, onSelected: (Int) -> Unit) {
        val adapter = ArrayAdapter.createFromResource(
            this, R.array.call_mode_options, android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(currentMode)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onSelected(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
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
                    roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
                }
            }
        } else {
            Toast.makeText(this, "Go to Settings > Apps > Default apps > Caller ID & spam app, and select CallGuard", Toast.LENGTH_LONG).show()
        }
    }

    /** Only starts the background ring-detector service once the required permissions are actually granted. */
    private fun startRingDetectorIfReady() {
        try {
            val hasPhoneState = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPhoneState) return

            val intent = Intent(this, RingDetectorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Could not start ring detector", e)
        }
    }

    private fun updateStatusText() {
        val permsGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        val roleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) ?: false
        } else true

        binding.textStatus.text = buildString {
            append(if (permsGranted) "✔ Permissions: OK\n" else "✘ Permissions: Missing (tap button below)\n")
            append(if (roleHeld) "✔ Default call screening app: Set" else "✘ Default call screening app: Not set (tap button below)")
        }
    }

    private fun updateSimNumbers() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
                binding.textSim1Number.text = "Permission required"
                binding.textSim2Number.text = "Permission required"
                return
            }
            val subscriptionManager = getSystemService(SubscriptionManager::class.java)
            @Suppress("MissingPermission")
            val subs = subscriptionManager?.activeSubscriptionInfoList

            val sim1 = subs?.find { it.simSlotIndex == 0 }
            val sim2 = subs?.find { it.simSlotIndex == 1 }

            binding.textSim1Number.text = sim1?.let {
                val num = it.number
                if (!num.isNullOrBlank()) num else "SIM detected (number not shared by carrier)"
            } ?: "No SIM found"

            binding.textSim2Number.text = sim2?.let {
                val num = it.number
                if (!num.isNullOrBlank()) num else "SIM detected (number not shared by carrier)"
            } ?: "No SIM found"

        } catch (e: SecurityException) {
            binding.textSim1Number.text = "Permission required"
            binding.textSim2Number.text = "Permission required"
        }
    }
}
