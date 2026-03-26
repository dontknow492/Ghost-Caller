@file:Suppress("D")

package com.ghost.caller.ui.screens.call

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghost.caller.viewmodel.call.CallEvent
import com.ghost.caller.viewmodel.call.CallStatus
import com.ghost.caller.viewmodel.call.CallViewModel

@Composable
fun ActiveCallScreen(
    number: String,
    name: String?,
    viewModel: CallViewModel
) {
    val state by viewModel.state.collectAsState()
    var showDialpad by remember { mutableStateOf(false) }

    // Dynamic gradient with Glassmorphism effect
    val dynamicBackgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        )
    )

    // Premium radial gradient overlay for depth
    val premiumRadialGradient = Brush.radialGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
            Color.Transparent
        ),
        center = Offset(0.5f, 0.5f),
        radius = 1.5f,
        tileMode = TileMode.Clamp
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dynamicBackgroundGradient)
    ) {
        // Premium radial gradient overlay for depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(premiumRadialGradient)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 56.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- TOP HEADER: Avatar, Name, Status Pill ---
            CallHeader(
                name = state.contactName ?: name,
                number = state.phoneNumber.ifEmpty { number },
                status = state.callStatus,
                duration = state.callDurationFormatted,
                photoUri = state.contactPhotoUri?.toString()
            )

            Spacer(modifier = Modifier.weight(1f))

            // --- MIDDLE SECTION: Controls Grid OR Dialpad ---
            AnimatedContent(
                targetState = showDialpad,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "ControlsVsDialpad"
            ) { isDialpad ->
                if (isDialpad) {
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
                        onVideoClick = { /* Handle Video */ },
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
                onDialpadToggle = { showDialpad = !showDialpad },
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
    // Glassmorphism effect for status pill
    val glassBackground = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
        )
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Avatar with premium gradient border
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            // Premium gradient border
            Box(
                modifier = Modifier
                    .size(114.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.primary
                            )
                        )
                    )
            )

            Surface(
                modifier = Modifier.size(110.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 8.dp
            ) {
                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Contact Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Name with gradient text effect
        Text(
            text = name ?: number,
            fontSize = 34.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.basicMarquee(),
            style = MaterialTheme.typography.headlineMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        )

        // Number (Only show if we already showed the name above)
        if (name != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = number,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status or Duration Glass Pill with blur effect
        Surface(
            modifier = Modifier.blur(0.5.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            shape = RoundedCornerShape(percent = 50)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Pulsing Primary Dot for Active calls
                if (status == CallStatus.Active) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dotPulse"
                    )

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                val statusText = when (status) {
                    CallStatus.Active -> duration
                    CallStatus.Dialing -> "DIALING..."
                    CallStatus.Connecting -> "CONNECTING..."
                    CallStatus.OnHold -> "ON HOLD"
                    else -> status.name.uppercase()
                }

                Text(
                    text = statusText,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
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
                label = "Video",
                isActive = false,
                enabled = false,
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
                enabled = false,
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
                label = "Add",
                isActive = false,
                enabled = false,
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
        // Speaker Toggle with Glassmorphism
        CallControlButton(
            icon = if (isSpeakerOn) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
            label = null,
            isActive = isSpeakerOn,
            onClick = onSpeakerToggle
        )

        // End Call with Premium Gradient
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.error,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    )
                )
                .clickable { onEndCall() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CallEnd,
                contentDescription = "End Call",
                tint = MaterialTheme.colorScheme.onError,
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
    label: String? = null,
    isActive: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    // Glassmorphism gradient for background
    val glassGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
        )
    )

    // Smooth transition animations adapting to Material Theme colors
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            isActive -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        },
        label = "btnBgColor"
    )

    val iconTint by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            isActive -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "btnIconTint"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .then(
                    if (isActive) Modifier.blur(0.5.dp) else Modifier
                )
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    indication = ripple(bounded = true, radius = 36.dp),
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
        }

        if (!label.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
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
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
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
    // Glassmorphism effect for dialpad keys
    val glassBackground = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
        )
    )

    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(glassBackground)
            .blur(0.5.dp)
            .clickable(
                onClick = onClick,
                indication = ripple(bounded = true, color = MaterialTheme.colorScheme.onSurface),
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = if (letters.isNotEmpty()) (-2).dp else 0.dp)
        ) {
            Text(
                text = number,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}