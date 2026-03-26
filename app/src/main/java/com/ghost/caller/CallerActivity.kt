package com.ghost.caller

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
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
            makeCall(intent)
        } else {
            Timber.w("Required call permissions denied.")
            Toast.makeText(this, "Required permissions denied", Toast.LENGTH_SHORT).show()
            finish() // Close the activity since we can't proceed
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("CallerActivity created.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes.blurBehindRadius = 80 // Adjust intensity (0 to 150)
        } else {
            // Fallback for older devices: Increase the background dim
            // since true wallpaper blur isn't natively supported before API 31
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.attributes.dimAmount = 0.45f
        }

        // Check and request necessary permissions
        checkAndRequestPermissions()
    }

    // Handle new intents when launchMode is singleTop
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the activity's intent
        Timber.d("New intent received in CallerActivity.")
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
            makeCall(intent)
        }
    }

    private fun makeCall(callIntent: Intent) {
        // 1. First check for our custom string extra
        var number = callIntent.getStringExtra("number")

        // 2. If null, fallback to checking standard Android Intent data (tel: scheme)
        if (number.isNullOrEmpty() && callIntent.data?.scheme == "tel") {
            number = callIntent.data?.schemeSpecificPart
        }

        if (number.isNullOrEmpty()) {
            Timber.e("No valid number found in intent to make call.")
            finish() // Close the activity if we can't extract a valid number
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