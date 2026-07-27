package com.callguard.app

import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
        Manifest.permission.ANSWER_PHONE_CALLS
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (!results.values.all { it }) {
            Toast.makeText(this, "Saari permissions allow karna zaroori hai", Toast.LENGTH_LONG).show()
        }
        updateStatusText()
    }

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { updateStatusText() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)

        setupSpinner(binding.spinnerSim1, prefs.sim1Mode) { prefs.sim1Mode = it }
        setupSpinner(binding.spinnerSim2, prefs.sim2Mode) { prefs.sim2Mode = it }
        setupSpinner(binding.spinnerFallback, prefs.fallbackMode) { prefs.fallbackMode = it }

        binding.btnGrantPermissions.setOnClickListener { requestAllPermissions() }
        binding.btnSetDefaultScreeningApp.setOnClickListener { requestScreeningRole() }

        requestAllPermissions()
        requestScreeningRole()
    }

    override fun onResume() {
        super.onResume()
        updateStatusText()
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
            Toast.makeText(this, "Settings > Apps > Default apps > Caller ID & spam app mein CallGuard select karein", Toast.LENGTH_LONG).show()
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
            append(if (permsGranted) "✔ Permissions: OK\n" else "✘ Permissions: Missing (neeche button dabayein)\n")
            append(if (roleHeld) "✔ Default call screening app: Set" else "✘ Default call screening app: Not set (neeche button dabayein)")
        }
    }
}
