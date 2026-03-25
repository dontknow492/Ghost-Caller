@file:Suppress("D")

package com.ghost.caller.ui.screens.call


import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghost.caller.presentation.call.CallViewModel
import com.ghost.caller.viewmodel.call.CallSideEffect
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallScreen(
    onNavigateBack: () -> Unit,
    viewModel: CallViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    rememberCoroutineScope()
    val context = LocalContext.current

    // Handle side effects
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collectLatest { effect ->
            when (effect) {
                is CallSideEffect.ShowToast -> {
                    android.widget.Toast.makeText(
                        context,
                        effect.message,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }

                is CallSideEffect.ShowError -> {
                    android.widget.Toast.makeText(
                        context,
                        effect.error.message,
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }

                is CallSideEffect.NavigateBack -> {
                    onNavigateBack()
                }

                is CallSideEffect.Vibrate -> {
                    // Vibrate
                }

                is CallSideEffect.PlayRingtone -> {
                    // Play ringtone
                }

                is CallSideEffect.StopRingtone -> {
                    // Stop ringtone
                }
                // Handle other effects
                else -> {}
            }
        }
    }

    // Show different screens based on call status
    when {
        state.isIncomingCallScreenVisible -> {
            IncomingCallScreen(
                number = state.phoneNumber,
                name = state.contactName,
                viewModel = viewModel
            )
        }

        state.isCallScreenVisible -> {
            ActiveCallScreen(
                number = state.phoneNumber,
                name = state.contactName,
                viewModel = viewModel
            )
        }

        else -> {
            // If the call ends or there is no call, safely pop the screen off the stack
            DialerScreen(
                viewModel = viewModel,
                onNavigateBack = onNavigateBack,
            )
        }

    }
}






