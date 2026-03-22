@file:Suppress("D")

package com.ghost.caller.ui.screens

// ---------------------------
// Call Screens
// ---------------------------
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.ghost.caller.R
import com.ghost.caller.ui.components.CallAction
import com.ghost.caller.ui.components.CallActionRowModel
import com.ghost.caller.ui.components.CallActionsLayout
import com.ghost.caller.ui.components.CallerInfoHeader
import com.ghost.caller.viewmodel.CallViewModel
import kotlinx.coroutines.delay

@Composable
fun ActiveCallScreen(
    number: String,
    name: String?,
    viewModel: CallViewModel
) {
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    // --- Timer Logic ---
    LaunchedEffect(Unit) {
        viewModel.resetDuration()
        while (true) {
            delay(1000)
            viewModel.incrementDuration()
        }
    }

    // --- State Collection ---
    val callDuration by viewModel.callDuration.collectAsState()
    val isOnHold by viewModel.isOnHold.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()

    // Formatter for the timer
    val minutes = callDuration / 60
    val seconds = callDuration % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)
    val displayStatus = if (isOnHold) "On Hold" else timeString

    // --- RESPONSIVE LAYOUT SWITCH ---
    if (isWideScreen) {
        // WIDESCREEN / LANDSCAPE MODE
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Left Side: Caller Info & Timer
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                ActiveCallInfoHeaderSection(
                    number = number,
                    name = name,
                    isRecording = isRecording,
                    callDuration = callDuration,
                    displayStatus = displayStatus
                )
            }

            // Right Side: Action Buttons
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                ActiveCallActionGridSection(
                    viewModel = viewModel,
                    isRecording = isRecording,
                    isMuted = isMuted,
                    isOnHold = isOnHold,
                    isSpeakerOn = isSpeakerOn
                )
            }
        }
    } else {
        // STANDARD PORTRAIT MODE
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 72.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Caller Info & Timer
            ActiveCallInfoHeaderSection(
                number = number,
                name = name,
                isRecording = isRecording,
                callDuration = callDuration,
                displayStatus = displayStatus
            )

            // Bottom: Action Buttons
            ActiveCallActionGridSection(
                viewModel = viewModel,
                isRecording = isRecording,
                isMuted = isMuted,
                isOnHold = isOnHold,
                isSpeakerOn = isSpeakerOn
            )
        }
    }
}

// ---------------------------------------------------------
// EXTRACTED COMPONENTS (Keeps your main screen clean!)
// ---------------------------------------------------------

@Composable
private fun ActiveCallInfoHeaderSection(
    number: String,
    name: String?,
    isRecording: Boolean,
    callDuration: Int,
    displayStatus: String
) {
    CallerInfoHeader(
        number = number,
        name = name,
        isDialing = false, // It's an active call now
        isRecording = isRecording,
        callDuration = callDuration,
        displayStatus = displayStatus,
    )
}

@Composable
private fun ActiveCallActionGridSection(
    viewModel: CallViewModel,
    isRecording: Boolean,
    isMuted: Boolean,
    isOnHold: Boolean,
    isSpeakerOn: Boolean
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    CallActionsLayout(
        rows = listOf(
            // Row 1
            CallActionRowModel(
                actions = listOf(
                    CallAction(
                        icon = ImageVector.vectorResource(R.drawable.graphic_eq),
                        label = "Record",
                        isActive = isRecording,
                        activeColor = colors.error,
                        onClick = { viewModel.toggleRecording() }
                    ),
                    CallAction( // Replaced duplicate "Record" with "Video"
                        icon = ImageVector.vectorResource(R.drawable.voicemail_24px),
                        label = "Video",
                        isEnabled = false
                    ),
                    CallAction(
                        icon = ImageVector.vectorResource(R.drawable.note_alt),
                        label = "Note",
                        onClick = {
                            val intent = Intent(Intent.ACTION_CREATE_NOTE)
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context,
                                    "No Notes app found",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                )
            ),

            // Row 2
            CallActionRowModel(
                actions = listOf(
                    CallAction(
                        icon = ImageVector.vectorResource(R.drawable.mic_off),
                        label = "Mute",
                        isActive = isMuted,
                        onClick = { viewModel.toggleMute() }
                    ),
                    CallAction(
                        icon = ImageVector.vectorResource(R.drawable.pause),
                        label = "Hold",
                        isActive = isOnHold,
                        onClick = { viewModel.toggleHold() }
                    ),
                    CallAction(
                        icon = ImageVector.vectorResource(R.drawable.add_call),
                        label = "Add",
                        isEnabled = false // disabled state
                    )
                )
            ),

            // Row 3 (Speaker - End Call - Dialpad)
            CallActionRowModel(
                actions = listOf(
                    CallAction(
                        icon = ImageVector.vectorResource(R.drawable.volume_up),
                        label = "Speaker",
                        isActive = isSpeakerOn,
                        hideLabel = true,
                        onClick = { viewModel.toggleSpeaker() }
                    ),
                    CallAction(
                        icon = ImageVector.vectorResource(R.drawable.dialpad),
                        label = "Dialpad",
                        hideLabel = true,
                        isEnabled = false
                    )
                ),
                centerContent = {
                    FloatingActionButton(
                        onClick = { viewModel.endCall() },
                        containerColor = colors.error,
                        contentColor = colors.onError,
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(2.dp)
                    ) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.call_end),
                            contentDescription = "End Call",
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            )
        ),
    )
}


