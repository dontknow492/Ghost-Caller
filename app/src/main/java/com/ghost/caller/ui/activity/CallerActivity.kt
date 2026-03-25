package com.ghost.caller.ui.activity


import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.ghost.caller.presentation.call.CallViewModel
import com.ghost.caller.viewmodel.call.CallEvent

class CallActivity : ComponentActivity() {

    private val viewModel: CallViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Start call handling
            makeCall()
        } else {
            Toast.makeText(this, "Required permissions denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check and request necessary permissions
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        permissions.add(Manifest.permission.CALL_PHONE)
        permissions.add(Manifest.permission.READ_PHONE_STATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissions.add(Manifest.permission.MANAGE_OWN_CALLS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.ANSWER_PHONE_CALLS)
        }

        val requiredPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (requiredPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(requiredPermissions)
        } else {
            makeCall()
        }
    }

    private fun makeCall() {
        val number = intent.getStringExtra("number") ?: return
        viewModel.sendEvent(CallEvent.InitiateCallDirectly(number))
//        viewModel.initiateCall(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up
    }
}