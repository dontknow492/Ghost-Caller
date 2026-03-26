package com.ghost.caller

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import com.ghost.caller.ui.navigation.AppNavigation
import com.ghost.caller.ui.screens.PermissionsWrapper
import com.ghost.caller.ui.theme.CallerTheme
import timber.log.Timber

// --- ENTRY POINT ---
// ---------------------------
// Main Activity
// ---------------------------
class MainActivity : ComponentActivity() {

    private val defaultDialerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Check the status after the user interacts with the system prompt
        if (checkIfDefaultDialer()) {
            Timber.d("Successfully set as the default dialer.")
            Toast.makeText(this, "Success! App is now the default dialer.", Toast.LENGTH_SHORT)
                .show()
        } else {
            Timber.w("User declined to set app as the default dialer.")
            Toast.makeText(
                this,
                "App must be the default dialer to receive calls.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.CALL_PHONE,
                android.Manifest.permission.READ_CALL_LOG,
                android.Manifest.permission.POST_NOTIFICATIONS, // Android 13+ only
                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.WRITE_CALL_LOG,
                android.Manifest.permission.MANAGE_OWN_CALLS,
                android.Manifest.permission.ANSWER_PHONE_CALLS
            )
        } else {
            listOf(
                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.CALL_PHONE,
                android.Manifest.permission.READ_CALL_LOG,
                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.WRITE_CALL_LOG,
                android.Manifest.permission.MANAGE_OWN_CALLS,
                android.Manifest.permission.ANSWER_PHONE_CALLS
            )
        }

        setContent {
            CallerTheme(
                isDarkTheme = isSystemInDarkTheme(),
                dynamicColor = false
            ) {
                PermissionsWrapper(permissions = requiredPermissions) {
                    // This ONLY shows up if all permissions are true
                    AppNavigation()
                }
            }
        }

        requestDefaultDialerRole()
    }


    // 3. A helper function to check if we are ALREADY the default dialer
    private fun checkIfDefaultDialer(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10+
            val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
            roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
        } else { // Android 9 and below
            val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
            telecomManager.defaultDialerPackage == packageName
        }
    }

    // 4. The function that actually triggers the system popup
    private fun requestDefaultDialerRole() {
        // If we are already the default dialer, do nothing!
        if (checkIfDefaultDialer()) {
            Timber.d("App is already set as the default dialer.")
            return
        }

        Timber.d("Requesting default dialer role...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Approach for Android 10 and above
            val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
            if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                defaultDialerLauncher.launch(intent)
            }
        } else {
            // Approach for Android 9 and below
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            }
            defaultDialerLauncher.launch(intent)
        }
    }
}