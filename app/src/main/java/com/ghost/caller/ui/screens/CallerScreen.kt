@file:Suppress("D")

package com.ghost.caller.ui.screens

//import androidx.compose.ui.graphics.RenderEffect
//import androidx.compose.ui.graphics.Shader

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ghost.caller.viewmodel.CallState
import com.ghost.caller.viewmodel.CallViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState


// --- MAIN UI COMOSABLE ---
// --- MAIN UI COMOSABLE ---
// ---------------------------
// Main Composable
// ---------------------------
@OptIn(ExperimentalAnimationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun CallingApp(viewModel: CallViewModel) {
    // Define required permissions
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_CALL_LOG,

//            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    } else {
        listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_CALL_LOG,
        )
    }

    // Track permission states using Accompanist
    val permissionsState = rememberMultiplePermissionsState(requiredPermissions)
    val allPermissionsGranted = permissionsState.allPermissionsGranted

    // Show rationale dialog if needed
    var showRationale by remember { mutableStateOf(false) }

    LaunchedEffect(permissionsState.revokedPermissions) {
        if (permissionsState.revokedPermissions.isNotEmpty() && !allPermissionsGranted) {
            showRationale = true
        }
    }

    // Load recent calls when permissions are granted
    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted) {
            viewModel.loadRecentCalls()
        }
    }

    // Request permissions on first launch


    // Permission rationale dialog
    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Permissions Required") },
            text = { Text("This app needs phone, contacts, and call log permissions to function properly.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationale = false
//                        permissionsState.launchMultiplePermissionRequest()
                    }
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val callState by viewModel.callState.collectAsState()

    // Main UI based on permission state
    if (allPermissionsGranted) {
        AnimatedContent(
            targetState = callState,
            transitionSpec = {
                fadeIn(tween(300)).togetherWith(fadeOut(tween(300)))
            },
            label = "CallStateTransition"
        ) { state ->
            when (state) {
                is CallState.Idle -> DialPadScreen(viewModel, true)
                is CallState.Outgoing -> {
                    WallpaperCallContainer {
                        OutgoingCallScreen(state.number, state.name, viewModel)
                    }
                }

                is CallState.Incoming -> {
                    WallpaperCallContainer {
                        IncomingCallScreen(state.number, state.name, viewModel)
                    }
                }

                is CallState.Active -> {
                    WallpaperCallContainer {
                        ActiveCallScreen(state.number, state.name, viewModel)
                    }
                }
            }
        }
    } else {
        PermissionRequestScreen(
            onRequestPermissions = {
                permissionsState.launchMultiplePermissionRequest()
            }
        )
    }
}
// --- SCREENS ---


@Composable
fun WallpaperCallContainer(
    content: @Composable () -> Unit
) {
//    val context = LocalContext.current
//    val window = (context as? Activity)?.window

    // Apply the blur effect to the Activity window
//    DisposableEffect(Unit) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            // Tell the window we want to blur what's behind it
//            window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
//            // Set the blur radius (e.g., 50 pixels). Higher = blurrier.
//            window?.attributes?.blurBehindRadius = 50
//            window?.attributes = window?.attributes
//        }
//
//        // Cleanup when the composable leaves the screen
//        onDispose {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                window?.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
//                window?.attributes?.blurBehindRadius = 0
//                window?.attributes = window?.attributes
//            }
//        }
//    }

    // Combine the true blur with a slight dark tint for maximum readability
    Surface(
        modifier = Modifier.fillMaxSize(),
//        color = if(isSystemInDarkTheme())Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.6f)
    ) {
        content()
    }
}

