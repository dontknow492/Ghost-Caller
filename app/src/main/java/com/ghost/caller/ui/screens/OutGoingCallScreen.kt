package com.ghost.caller.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.ghost.caller.R
import com.ghost.caller.ui.components.CallAction
import com.ghost.caller.ui.components.CallActionRowModel
import com.ghost.caller.ui.components.CallActionsLayout
import com.ghost.caller.ui.components.CallerInfoHeader
import com.ghost.caller.viewmodel.CallViewModel

@Composable
fun OutgoingCallScreen(
    number: String,
    name: String?,
    viewModel: CallViewModel
) {
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    if (isWideScreen) {
        // --- WIDESCREEN / LANDSCAPE MODE ---
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Left Side: Caller Info
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CallInfoHeaderSection(number = number, name = name)
            }

            // Right Side: Action Buttons
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CallActionGridSection(viewModel = viewModel)
            }
        }
    } else {
        // --- STANDARD PORTRAIT MODE ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 72.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Caller Info
            CallInfoHeaderSection(number = number, name = name)

            // Bottom: Action Buttons
            CallActionGridSection(viewModel = viewModel)
        }
    }
}

// ---------------------------------------------------------
// EXTRACTED COMPONENTS (Keeps your main screen clean!)
// ---------------------------------------------------------

@Composable
private fun CallInfoHeaderSection(number: String, name: String?) {
    CallerInfoHeader(
        number = number,
        name = name,
        isDialing = true,
        isRecording = false,
        callDuration = 0,
        displayStatus = "Dialing…",
    )
}

@Composable
private fun CallActionGridSection(viewModel: CallViewModel) {
    val colors = MaterialTheme.colorScheme

    CallActionsLayout(
        rows = listOf(
            // Row 1
            CallActionRowModel(
                actions = listOf(
                    CallAction(
                        icon = ImageVector.vectorResource(R.drawable.graphic_eq),
                        label = "Record",
                        isEnabled = false
                    ),
                    CallAction( // Replaced duplicate "Record" with a Video call placeholder
                        icon = ImageVector.vectorResource(R.drawable.voicemail_24px),
                        label = "Video",
                        isEnabled = false
                    ),
                    CallAction(
                        icon = ImageVector.vectorResource(R.drawable.note_alt),
                        label = "Note",
                        isEnabled = false
                    )
                )
            ),

            // Row 2
            CallActionRowModel(
                actions = listOf(
                    CallAction(
                        icon = ImageVector.vectorResource(R.drawable.mic_off),
                        label = "Mute",
                        isEnabled = false
                    ),
                    CallAction(
                        icon = ImageVector.vectorResource(R.drawable.pause),
                        label = "Hold",
                        isEnabled = false
                    ),
                    CallAction(
                        icon = ImageVector.vectorResource(R.drawable.add_call),
                        label = "Add",
                        isEnabled = false
                    )
                )
            ),

            // Row 3 (with End Call)
            CallActionRowModel(
                actions = listOf(
                    CallAction(
                        icon = ImageVector.vectorResource(R.drawable.volume_up),
                        label = "Speaker",
                        hideLabel = true,
                        isEnabled = false
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
                        elevation = FloatingActionButtonDefaults.elevation(6.dp)
                    ) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.call_end),
                            contentDescription = "End Call",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            )
        ),
    )
}