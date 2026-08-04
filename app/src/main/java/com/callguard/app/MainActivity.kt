package com.callguard.app

import android.Manifest
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.callguard.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs

    private val requiredPermissions: Array<String> by lazy {
        val base = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.READ_PHONE_NUMBERS
        )
        if (Build.VERSION.SDK_INT >= 33) base + Manifest.permission.POST_NOTIFICATIONS else base
    }

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

    private val toneLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (uri != null) {
            prefs.setNotificationToneUri(uri.toString())
            NotificationHelper.updateChannelSound(this, uri)
            Toast.makeText(this, "Notification tone updated", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)
        NotificationHelper.ensureChannelExists(this)

        val lastCrash = prefs.getLastCrash()
        if (lastCrash != null) {
            AlertDialog.Builder(this)
                .setTitle("Last crash details")
                .setMessage(lastCrash)
                .setPositiveButton("OK") { _, _ -> prefs.clearCrash() }
                .setCancelable(false)
                .show()
        }

        setupFallbackSpinner()

        binding.cardBlockingRuleSim1.setOnClickListener { showModePicker(0) }
        binding.cardBlockingRuleSim2.setOnClickListener { showModePicker(1) }

        binding.rowViewBlockedSim1.setOnClickListener {
            startActivity(Intent(this, BlockedCallsActivity::class.java).putExtra("slot", 0))
        }
        binding.rowViewBlockedSim2.setOnClickListener {
            startActivity(Intent(this, BlockedCallsActivity::class.java).putExtra("slot", 1))
        }
        binding.rowWhitelistSim1.setOnClickListener {
            startActivity(Intent(this, WhitelistActivity::class.java))
        }
        binding.rowWhitelistSim2.setOnClickListener {
            startActivity(Intent(this, WhitelistActivity::class.java))
        }

        binding.btnGrantPermissions.setOnClickListener { requestAllPermissions() }
        binding.btnSetDefaultScreeningApp.setOnClickListener { requestScreeningRole() }
        binding.btnSetNotificationTone.setOnClickListener { openTonePicker() }

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
        updateProtectionCards()
        updateBlockingRuleCards()
        updateStats()
        binding.textDebugLog.text = prefs.getDebugLog()
        binding.textServiceStatus.text = prefs.getServiceStatus()
    }

    private fun showPage(index: Int) {
        val pages = listOf(binding.pageSim1, binding.pageSim2, binding.pageSettings)
        val target = pages[index]

        pages.forEach { page ->
            if (page != target && page.visibility == View.VISIBLE) {
                page.animate()
                    .alpha(0f)
                    .setDuration(120)
                    .withEndAction { page.visibility = View.GONE }
                    .start()
            }
        }

        target.alpha = 0f
        target.visibility = View.VISIBLE
        target.animate()
            .alpha(1f)
            .setDuration(200)
            .start()

        highlightTab(binding.tabSim1, index == 0)
        highlightTab(binding.tabSim2, index == 1)
        highlightTab(binding.tabSettings, index == 2)
    }

    private fun highlightTab(tab: LinearLayout, selected: Boolean) {
        tab.setBackgroundResource(if (selected) R.drawable.bg_tab_selected else 0)
    }

    private fun setupFallbackSpinner() {
        val adapter = android.widget.ArrayAdapter.createFromResource(
            this, R.array.call_mode_options, android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFallback.adapter = adapter
        binding.spinnerFallback.setSelection(prefs.fallbackMode)
        binding.spinnerFallback.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.fallbackMode = position
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun showModePicker(slot: Int) {
        val options = arrayOf("Allow all calls", "Allow contacts only", "Block all calls")
        val current = if (slot == 0) prefs.sim1Mode else prefs.sim2Mode

        AlertDialog.Builder(this)
            .setTitle(if (slot == 0) "SIM 1 blocking rule" else "SIM 2 blocking rule")
            .setSingleChoiceItems(options, current) { dialog, which ->
                if (slot == 0) prefs.sim1Mode = which else prefs.sim2Mode = which
                updateBlockingRuleCards()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateBlockingRuleCards() {
        binding.textBlockingRuleTitleSim1.text = prefs.modeLabel(prefs.sim1Mode)
        binding.textBlockingRuleSubtitleSim1.text = prefs.modeSubtitle(prefs.sim1Mode)
        binding.textBlockingRuleTitleSim2.text = prefs.modeLabel(prefs.sim2Mode)
        binding.textBlockingRuleSubtitleSim2.text = prefs.modeSubtitle(prefs.sim2Mode)
    }

    private fun updateProtectionCards() {
        val permsGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        val roleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) ?: false
        } else true

        val isProtected = permsGranted && roleHeld
        val statusText = if (isProtected) "Active" else "Setup needed"
        val statusColor = if (isProtected) 0xFF4CAF50.toInt() else 0xFFFF9800.toInt()
        val subtitle = if (isProtected) "Both SIMs are protected" else "Complete setup in Settings tab"

        binding.textProtectionStatusSim1.text = statusText
        binding.textProtectionStatusSim1.setTextColor(statusColor)
        binding.textProtectionSubtitleSim1.text = subtitle

        binding.textProtectionStatusSim2.text = statusText
        binding.textProtectionStatusSim2.setTextColor(statusColor)
        binding.textProtectionSubtitleSim2.text = subtitle
    }

    private fun updateStats() {
        val blocked = prefs.getBlockedToday().toString()
        val allowed = prefs.getAllowedToday().toString()
        val total = prefs.getSpamBlockedTotal().toString()
        val lastUpdate = prefs.getLastUpdateTime()

        binding.textBlockedTodaySim1.text = blocked
        binding.textAllowedTodaySim1.text = allowed
        binding.textSpamBlockedSim1.text = total
        binding.textLastUpdateSim1.text = lastUpdate

        binding.textBlockedTodaySim2.text = blocked
        binding.textAllowedTodaySim2.text = allowed
        binding.textSpamBlockedSim2.text = total
        binding.textLastUpdateSim2.text = lastUpdate
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

    private fun openTonePicker() {
        val currentUriString = prefs.getNotificationToneUri()
        val currentUri = if (!currentUriString.isNullOrBlank()) {
            Uri.parse(currentUriString)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose blocked-call notification tone")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
        }
        toneLauncher.launch(intent)
    }

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
