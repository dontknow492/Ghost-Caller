package com.ghost.caller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.ghost.caller.viewmodel.call.CallEvent
import com.ghost.caller.viewmodel.call.CallViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class CallerActivity : ComponentActivity() {

    // Safely inject via Koin so ContactRepository dependency is satisfied
    private val viewModel: CallViewModel by viewModel()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Timber.d("All required call permissions granted via launcher.")
            makeCall()
        } else {
            Timber.w("Required call permissions denied.")
            Toast.makeText(this, "Required permissions denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("CallerActivity created.")

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
            Timber.d("Requesting permissions: %s", requiredPermissions.joinToString())
            requestPermissionLauncher.launch(requiredPermissions)
        } else {
            Timber.d("All required call permissions already granted.")
            makeCall()
        }
    }

    private fun makeCall() {
        val number = intent.getStringExtra("number")
        if (number.isNullOrEmpty()) {
            Timber.e("No valid number found in intent to make call.")
            return
        }

        Timber.d("Initiating direct call to: %s", number)
        viewModel.sendEvent(CallEvent.InitiateCallDirectly(number))
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("CallerActivity destroyed.")
    }
}