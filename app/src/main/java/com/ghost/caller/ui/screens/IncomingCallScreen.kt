package com.ghost.caller.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghost.caller.R
import com.ghost.caller.viewmodel.CallViewModel

@Composable
fun IncomingCallScreen(
    number: String,
    name: String?,
    viewModel: CallViewModel
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 72.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        // 👤 Caller Info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = name ?: number,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground
            )

            if (name != null) {
                Text(
                    text = number,
                    fontSize = 15.sp,
                    color = colors.onSurfaceVariant
                )
            }

            Text(
                text = "Incoming call…",
                fontSize = 14.sp,
                color = colors.primary.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        // 🔥 Swipe Control
        SwipeToAnswer(
            onAccept = { viewModel.acceptCall() },
            onDecline = { viewModel.endCall() }
        )
    }
}


@Composable
fun SwipeToAnswer(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    rememberInfiniteTransition(label = "")

    val colors = MaterialTheme.colorScheme
    val maxOffset = 600f

    var offsetX by remember { mutableStateOf(0f) }

    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        label = "swipeAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(50))
            .background(colors.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {

        // 🧭 Background labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Decline",
                color = colors.error,
                modifier = Modifier.padding(start = 24.dp)
            )
            Text(
                "Accept",
                color = colors.primary,
                modifier = Modifier.padding(end = 24.dp)
            )
        }

        // 🎯 Draggable circle
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.toInt(), 0) }
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.primary)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            when {
                                offsetX > maxOffset * 0.4f -> {
                                    onAccept()
                                }

                                offsetX < -maxOffset * 0.4f -> {
                                    onDecline()
                                }
                            }
                            offsetX = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x)
                                .coerceIn(-maxOffset, maxOffset)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                ImageVector.vectorResource(R.drawable.call),
                contentDescription = "Swipe",
                tint = colors.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}


