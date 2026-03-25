@file:Suppress("D")

package com.ghost.caller.ui.screens.call


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghost.caller.presentation.call.CallViewModel
import com.ghost.caller.viewmodel.call.CallEvent
import com.ghost.caller.viewmodel.call.CallQuality
import com.ghost.caller.viewmodel.call.CallSideEffect
import com.ghost.caller.viewmodel.call.CallState
import com.ghost.caller.viewmodel.call.ContactSuggestion
import com.ghost.caller.viewmodel.recent.CallLogViewModel
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






