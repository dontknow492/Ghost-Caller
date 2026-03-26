package com.ghost.caller.ui.screens.call

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
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



@Composable
fun IncomingCallScreen(
    number: String,
    name: String?,
    onRemindMe: (phoneNumber: String) -> Unit,
    onMessage: (phoneNumber: String) -> Unit,
    viewModel: CallViewModel
) {
    val state by viewModel.state.collectAsState()


    val DeepBackgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        ),
    )



    val PremiumRadialGradient = Brush.radialGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
            Color.Transparent
        ),
        center = Offset(0.5f, 0.5f),
        radius = 1.5f,
        tileMode = TileMode.Clamp
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackgroundGradient)
    ) {
        // Premium radial gradient overlay for depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PremiumRadialGradient)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 64.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            IncomingCallHeader(
                name = state.contactName ?: name,
                number = state.phoneNumber.ifEmpty { number },
                photoUri = state.contactPhotoUri?.toString()
            )

            Spacer(modifier = Modifier.weight(1f))

            // --- Quick Actions with Glassmorphism ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionButton(
                    icon = Icons.Rounded.Alarm,
                    label = "Remind Me",
                    colors = MaterialTheme.colorScheme
                ) {
                    onRemindMe(state.phoneNumber)
                }
                QuickActionButton(
                    icon = Icons.AutoMirrored.Rounded.Message,
                    label = "Message",
                    colors = MaterialTheme.colorScheme
                ) {
                    onMessage(state.phoneNumber)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // --- Modern Spring-physics Slider with Material colors ---
            TwoWaySwipeToAnswer(
                onAccept = { viewModel.sendEvent(CallEvent.AcceptCall) },
                onReject = { viewModel.sendEvent(CallEvent.RejectCall) },
                colors = MaterialTheme.colorScheme
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun IncomingCallHeader(
    name: String?,
    number: String,
    photoUri: String?
) {

    val GlassWhite = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        // Premium Sonar Ripple Avatar with Material colors
        SonarRippleAvatar(
            photoUri = photoUri,
            colors = MaterialTheme.colorScheme
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Contact Name with Material styling
        Text(
            text = name ?: number,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 38.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .basicMarquee()
        )

        // Contact Number
        if (name != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = number,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status Text with Glassmorphism
        Surface(
            color = GlassWhite,
            shape = RoundedCornerShape(percent = 50),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = "INCOMING CALL",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun SonarRippleAvatar(
    photoUri: String?,
    colors: androidx.compose.material3.ColorScheme
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sonar")

    @Composable
    fun createPulseSpec(delay: Int): Float {
        return infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(delay, StartOffsetType.Delay)
            ),
            label = "pulse_$delay"
        ).value
    }

    val pulse1 = createPulseSpec(0)
    val pulse2 = createPulseSpec(600)
    val pulse3 = createPulseSpec(1200)

    Box(contentAlignment = Alignment.Center) {
        // Draw the 3 expanding sonar rings with Material primary colors
        listOf(pulse1, pulse2, pulse3).forEach { progress ->
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(1f + (progress * 1.5f))
                    .alpha(1f - progress)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.3f))
            )
        }

        // Actual Avatar with Material colors
        Surface(
            modifier = Modifier.size(150.dp),
            shape = CircleShape,
            color = colors.surfaceVariant
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
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    colors: androidx.compose.material3.ColorScheme,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        // Glassmorphism effect with blur
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.surface.copy(alpha = 0.3f),
                            colors.surface.copy(alpha = 0.1f)
                        )
                    )
                )
                .blur(0.5.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colors.primary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = label,
            color = colors.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TwoWaySwipeToAnswer(
    onAccept: () -> Unit,
    onReject: () -> Unit,
    colors: androidx.compose.material3.ColorScheme
) {
    // Glassmorphism effect with Material colors
    val GlassBackground = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.05f)
        )
    )



    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    val maxDrag = with(LocalDensity.current) { 120.dp.toPx() }
    val triggerThreshold = maxDrag * 0.75f

    val infiniteTransition = rememberInfiniteTransition(label = "arrows")
    val arrowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .clip(CircleShape)
            .background(GlassBackground)
            .border(width = 2.dp, color = colors.primary, shape = CircleShape)
            .blur(0.5.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background Track with Material colors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Decline
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CallEnd,
                    contentDescription = "Decline",
                    tint = colors.error,
                    modifier = Modifier
                        .size(32.dp)
                        .alpha(1f - (offsetX.value / maxDrag).coerceIn(0f, 1f))
                )
                Spacer(modifier = Modifier.size(8.dp))
                Icon(
                    imageVector = Icons.Rounded.KeyboardDoubleArrowLeft,
                    contentDescription = null,
                    tint = colors.error.copy(alpha = arrowAlpha),
                    modifier = Modifier.alpha(1f - (offsetX.value / maxDrag).coerceIn(0f, 1f))
                )
            }

            // Right Side: Accept
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardDoubleArrowRight,
                    contentDescription = null,
                    tint = colors.tertiary.copy(alpha = arrowAlpha),
                    modifier = Modifier.alpha(1f - (-offsetX.value / maxDrag).coerceIn(0f, 1f))
                )
                Spacer(modifier = Modifier.size(8.dp))
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = "Accept",
                    tint = colors.tertiary,
                    modifier = Modifier
                        .size(32.dp)
                        .alpha(1f - (-offsetX.value / maxDrag).coerceIn(0f, 1f))
                )
            }
        }

        // Center Draggable Handle with Material colors
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(72.dp)
                .border(width = 2.dp, color = colors.outlineVariant, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            colors.surface,
                            colors.primaryContainer,
                            colors.surface
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetX.value > triggerThreshold) {
                                    offsetX.animateTo(maxDrag, tween(200))
                                    onAccept()
                                } else if (offsetX.value < -triggerThreshold) {
                                    offsetX.animateTo(-maxDrag, tween(200))
                                    onReject()
                                } else {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val resistance =
                                    if (Math.abs(offsetX.value) > maxDrag * 0.9f) 0.5f else 1f
                                val newOffset = (offsetX.value + (dragAmount * resistance))
                                    .coerceIn(-maxDrag, maxDrag)
                                offsetX.snapTo(newOffset)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val handleIconTint = when {
                offsetX.value > 30f -> colors.tertiary
                offsetX.value < -30f -> colors.error
                else -> colors.onSurface
            }

            val handleIcon = when {
                offsetX.value < -30f -> Icons.Rounded.CallEnd
                else -> Icons.Rounded.Call
            }

            Icon(
                imageVector = handleIcon,
                contentDescription = "Drag to answer or decline",
                tint = handleIconTint,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}