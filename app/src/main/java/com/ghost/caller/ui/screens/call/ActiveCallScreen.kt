@file:Suppress("D")

package com.ghost.caller.ui.screens.call

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghost.caller.viewmodel.call.CallEvent
import com.ghost.caller.viewmodel.call.CallStatus
import com.ghost.caller.viewmodel.call.CallViewModel

// Modern Dark Theme Colors for the Call Screen
private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF2A2D34), // Slate dark
        Color(0xFF121212)  // Deep black
    )
)
private val ButtonDarkGray = Color(0xFF333333)
private val ButtonActiveWhite = Color(0xFFE0E0E0)
private val EndCallRed = Color(0xFFFF3B30)

@Composable
fun ActiveCallScreen(
    number: String,
    name: String?,
    viewModel: CallViewModel
) {
    val state by viewModel.state.collectAsState()

    var showDialpad by remember { mutableStateOf(false) }

    // Main Container with modern static gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 48.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- TOP HEADER: Avatar, Name, Number, Status ---
            CallHeader(
                name = state.contactName ?: name,
                number = state.phoneNumber.ifEmpty { number },
                status = state.callStatus,
                duration = state.callDurationFormatted,
                photoUri = state.contactPhotoUri?.toString()
            )

            Spacer(modifier = Modifier.weight(1f))

            // --- MIDDLE SECTION: Controls Grid OR Dialpad ---
            Crossfade(
                targetState = showDialpad,
//                animationSpec = tween(300),
                label = "ControlsVsDialpad"
            ) { showDialpad ->
                if (showDialpad) {
                    InCallDialpad(
                        onDigitClick = { viewModel.sendEvent(CallEvent.AppendDigit(it)) }
                    )
                } else {
                    CallControlsGrid(
                        isMuted = state.isMuted,
                        isOnHold = state.isOnHold,
                        isRecording = state.isRecording,
                        onMuteToggle = { viewModel.sendEvent(CallEvent.ToggleMute) },
                        onHoldToggle = { viewModel.sendEvent(CallEvent.ToggleHold) },
                        onRecordToggle = { viewModel.sendEvent(CallEvent.ToggleRecording) },
                        onVideoClick = { /* Handle Video (Disabled/Future) */ },
                        onNoteClick = { /* Handle Note */ },
                        onAddCallClick = { /* Handle Add Call */ }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // --- BOTTOM ROW: Speaker, End Call, Dialpad Toggle ---
            BottomCallBar(
                isSpeakerOn = state.isSpeakerOn,
                isDialpadOpen = showDialpad,
                onSpeakerToggle = { viewModel.sendEvent(CallEvent.ToggleSpeaker) },
                onDialpadToggle = {
                    showDialpad = !showDialpad
                },
                onEndCall = { viewModel.sendEvent(CallEvent.EndCall) }
            )
        }
    }
}

@Composable
private fun CallHeader(
    name: String?,
    number: String,
    status: CallStatus,
    duration: String,
    photoUri: String?
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Avatar
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = ButtonDarkGray
        ) {
            if (photoUri != null) {
                // If you have Coil installed, this loads the contact image
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Contact Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fallback icon
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Name (Large)
        Text(
            text = name ?: number,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )

        // Number (Only show if we already showed the name above)
        if (name != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = number,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status or Duration
        val statusText = when (status) {
            CallStatus.Active -> duration
            CallStatus.Dialing -> "DIALING..."
            CallStatus.Connecting -> "CONNECTING..."
            CallStatus.OnHold -> "ON HOLD"
            else -> status.name.uppercase()
        }

        Text(
            text = statusText,
            color = if (status == CallStatus.Active) Color.White else Color.White.copy(alpha = 0.5f),
            fontSize = 16.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun CallControlsGrid(
    isMuted: Boolean,
    isOnHold: Boolean,
    isRecording: Boolean,
    onMuteToggle: () -> Unit,
    onHoldToggle: () -> Unit,
    onRecordToggle: () -> Unit,
    onVideoClick: () -> Unit,
    onNoteClick: () -> Unit,
    onAddCallClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CallControlButton(
                icon = Icons.Rounded.Videocam,
                label = "Video call",
                isActive = false, // Usually disabled until implemented
                onClick = onVideoClick
            )
            CallControlButton(
                icon = Icons.Rounded.GraphicEq,
                label = "Record",
                isActive = isRecording,
                onClick = onRecordToggle
            )
            CallControlButton(
                icon = Icons.AutoMirrored.Rounded.NoteAdd,
                label = "Note",
                isActive = false,
                onClick = onNoteClick
            )
        }

        // Bottom Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CallControlButton(
                icon = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                label = "Mute",
                isActive = isMuted,
                onClick = onMuteToggle
            )
            CallControlButton(
                icon = if (isOnHold) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                label = "Hold",
                isActive = isOnHold,
                onClick = onHoldToggle
            )
            CallControlButton(
                icon = Icons.Rounded.PersonAdd,
                label = "Add call",
                isActive = false,
                onClick = onAddCallClick
            )
        }
    }
}

@Composable
private fun BottomCallBar(
    isSpeakerOn: Boolean,
    isDialpadOpen: Boolean,
    onSpeakerToggle: () -> Unit,
    onDialpadToggle: () -> Unit,
    onEndCall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Speaker Toggle
        CallControlButton(
            icon = if (isSpeakerOn) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
            label = null, // No label on bottom row
            isActive = isSpeakerOn,
            onClick = onSpeakerToggle
        )

        // End Call (Large Red Button)
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(EndCallRed)
                .clickable { onEndCall() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CallEnd,
                contentDescription = "End Call",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        // Dialpad Toggle
        CallControlButton(
            icon = Icons.Rounded.Dialpad,
            label = null,
            isActive = isDialpadOpen,
            onClick = onDialpadToggle
        )
    }
}

@Composable
private fun CallControlButton(
    icon: ImageVector,
    label: String?,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (isActive) ButtonActiveWhite else ButtonDarkGray)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.Black else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        if (label != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun InCallDialpad(onDigitClick: (String) -> Unit) {
    val keys = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("*" to "", "0" to "+", "#" to "")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in keys) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (key in row) {
                    DialpadKey(
                        number = key.first,
                        letters = key.second,
                        onClick = { onDigitClick(key.first) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DialpadKey(number: String, letters: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(ButtonDarkGray)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = number,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}



