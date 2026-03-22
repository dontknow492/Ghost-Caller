package com.ghost.caller.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- REUSABLE UI COMPONENTS ---

@Composable
fun DialPadButton(
    key: DialPadKey,
    onClick: () -> Unit
) {
    val materialTheme = MaterialTheme

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
//            .background(
//                materialTheme.colorScheme.surfaceVariant,
//                CircleShape
//            )
            .clickable(
                onClick = onClick,
//                indication = rememberRipple(
//                    bounded = true,
//                    radius = 40.dp,
//                    color = materialTheme.colorScheme.primary
//                )
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Main number/symbol
            Text(
                text = key.number,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = materialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            )

            // Letters (if any)
            if (key.letters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = key.letters.joinToString(""),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = materialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 0.5.sp
                )
            } else if (key.number == "0") {
                // Special handling for 0 key
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "+",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = materialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// Data class for dial pad keys
data class DialPadKey(
    val number: String,
    val letters: List<String>
)

// Optional: Add haptic feedback and long press for special characters
@Composable
fun DialPadButtonWithHaptics(
    key: DialPadKey,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(64.dp) // reduced from 80
//            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
                    onLongPress = {
                        onLongPress?.let {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            it()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = key.number,
                fontSize = 28.sp, // reduced
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )

            if (key.letters.isNotEmpty()) {
                Text(
                    text = key.letters.joinToString(""),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 0.4.sp
                )
            }
        }
    }
}

@Composable
fun CallActionIconButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    hideLabel: Boolean = false,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val containerColor by animateColorAsState(
        targetValue = if (isActive)
            activeColor
        else
            colors.surfaceVariant.copy(alpha = 0.4f),
        label = "bg_anim"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isActive)
            colors.onPrimary
        else
            colors.onSurface,
        label = "icon_anim"
    )

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        label = "scale_anim"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(84.dp)
    ) {

        Box(
            modifier = Modifier
                .size(68.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(containerColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = colors.primary)
                ) {
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
        }

        if (!hideLabel) {
            Text(
                text = label,
                color = colors.onBackground.copy(alpha = 0.75f),
                fontSize = 13.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}