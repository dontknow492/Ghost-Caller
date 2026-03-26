package com.ghost.caller.ui.screens.call


import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghost.caller.viewmodel.call.CallEvent
import com.ghost.caller.viewmodel.call.CallViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Modern Dark Theme Colors
private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF2A2D34), // Slate dark
        Color(0xFF121212)  // Deep black
    )
)
private val AcceptGreen = Color(0xFF34C759)
private val DeclineRed = Color(0xFFFF3B30)
private val SliderBackground = Color(0xFF333333)

@Composable
fun IncomingCallScreen(
    number: String,
    name: String?,
    viewModel: CallViewModel
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 64.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- TOP: Ringing Avatar & Info ---
            Spacer(modifier = Modifier.height(32.dp))
            IncomingCallHeader(
                name = state.contactName ?: name,
                number = state.phoneNumber.ifEmpty { number },
                photoUri = state.contactPhotoUri?.toString()
            )

            Spacer(modifier = Modifier.weight(1f))

            // --- MIDDLE: Quick Actions (Remind / Message) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickActionButton(icon = Icons.Rounded.Alarm, label = "Remind Me") {}
                QuickActionButton(icon = Icons.Rounded.Message, label = "Message") {}
            }

            Spacer(modifier = Modifier.height(48.dp))

            // --- BOTTOM: Modern Two-Way Swipe Slider ---
            TwoWaySwipeToAnswer(
                onAccept = { viewModel.sendEvent(CallEvent.AcceptCall) },
                onReject = { viewModel.sendEvent(CallEvent.RejectCall) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IncomingCallHeader(
    name: String?,
    number: String,
    photoUri: String?
) {
    // Pulse Animation for the avatar background to indicate ringing
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Avatar with pulse effect
        Box(contentAlignment = Alignment.Center) {
            // Pulsing rings
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale)
                    .alpha(pulseAlpha)
                    .clip(CircleShape)
                    .background(Color.White)
            )

            // Actual Avatar
            Surface(
                modifier = Modifier.size(140.dp),
                shape = CircleShape,
                color = SliderBackground
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
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier
                            .padding(32.dp)
                            .fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Contact Name (Large)
        Text(
            text = name ?: number,
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )

        // Contact Number
        if (name != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = number,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status Text
        Text(
            text = "INCOMING CALL",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Light
        )
    }
}

@Composable
private fun TwoWaySwipeToAnswer(
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    // The maximum distance the user can drag the button (left or right)
    val maxDrag = with(LocalDensity.current) { 110.dp.toPx() }
    // The threshold distance to trigger the action (70% of max drag)
    val triggerThreshold = maxDrag * 0.7f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(CircleShape)
            .background(SliderBackground.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        // --- Background Icons (Decline / Accept) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Decline Icon (Fades out if dragging right)
            Icon(
                imageVector = Icons.Rounded.CallEnd,
                contentDescription = "Decline",
                tint = DeclineRed,
                modifier = Modifier
                    .size(32.dp)
                    .alpha(1f - (offsetX.value / maxDrag).coerceIn(0f, 1f))
            )

            // Right: Accept Icon (Fades out if dragging left)
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = "Accept",
                tint = AcceptGreen,
                modifier = Modifier
                    .size(32.dp)
                    .alpha(1f - (-offsetX.value / maxDrag).coerceIn(0f, 1f))
            )
        }

        // --- Center Draggable Handle ---
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                // Check if dragged far enough to trigger
                                if (offsetX.value > triggerThreshold) {
                                    offsetX.animateTo(maxDrag, tween(200))
                                    onAccept()
                                } else if (offsetX.value < -triggerThreshold) {
                                    offsetX.animateTo(-maxDrag, tween(200))
                                    onReject()
                                } else {
                                    // Snap back to center if not dragged enough
                                    offsetX.animateTo(0f, tween(300))
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                // Restrict dragging within maxDrag bounds
                                val newOffset =
                                    (offsetX.value + dragAmount).coerceIn(-maxDrag, maxDrag)
                                offsetX.snapTo(newOffset)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Change the handle icon color dynamically based on drag direction
            val handleIconTint = when {
                offsetX.value > 20f -> AcceptGreen
                offsetX.value < -20f -> DeclineRed
                else -> Color.DarkGray
            }

            val handleIcon = when {
                offsetX.value < -20f -> Icons.Rounded.CallEnd
                else -> Icons.Rounded.Call
            }

            Icon(
                imageVector = handleIcon,
                contentDescription = "Drag to answer or decline",
                tint = handleIconTint,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}