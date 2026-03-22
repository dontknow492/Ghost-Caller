package com.ghost.caller.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghost.caller.R


data class CallAction(
    val icon: ImageVector,
    val label: String,
    val isActive: Boolean = false,
    val isEnabled: Boolean = true,
    val hideLabel: Boolean = false,
    val activeColor: Color? = null,
    val onClick: (() -> Unit)? = null
)

data class CallActionRowModel(
    val actions: List<CallAction>,
    val centerContent: (@Composable () -> Unit)? = null
)

@Composable
fun CallerInfoHeader(
    number: String,
    name: String?,
    isDialing: Boolean,
    isRecording: Boolean,
    callDuration: Int,
    displayStatus: String,
    modifier: Modifier = Modifier
) {

    val colors = MaterialTheme.colorScheme

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {

        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Contact",
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

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

        // Status
        if (isDialing) {
            Text(
                text = "Dialing…",
                fontSize = 14.sp,
                color = colors.primary.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(isRecording) {
                Icon(
                    ImageVector.vectorResource(R.drawable.fiber_manual_record),
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(end = 6.dp)
                )
            }

            Text(
                text = displayStatus,
                fontSize = 16.sp,
                color = if (isRecording) colors.error else colors.onBackground.copy(alpha = 0.7f)
            )
        }

    }
}


@Composable
fun CallActionIconButton(
    action: CallAction
) {
    val colors = MaterialTheme.colorScheme

    val containerColor by animateColorAsState(
        targetValue = when {
            !action.isEnabled -> colors.surfaceVariant.copy(alpha = 0.2f)
            action.isActive -> action.activeColor ?: colors.primary
            else -> colors.surfaceVariant.copy(alpha = 0.4f)
        },
        label = ""
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            !action.isEnabled -> colors.onSurface.copy(alpha = 0.3f)
            action.isActive -> colors.onPrimary
            else -> colors.onSurface
        },
        label = ""
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(84.dp)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(containerColor)
                .clickable(
                    enabled = action.isEnabled,
                    onClick = { action.onClick?.invoke() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                action.icon,
                contentDescription = action.label,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
        }

        if (!action.hideLabel) {
            Text(
                text = action.label,
                color = colors.onBackground.copy(
                    alpha = if (action.isEnabled) 0.75f else 0.3f
                ),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}


@Composable
fun CallActionRow(actions: List<CallAction>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        actions.forEach {
            CallActionIconButton(action = it)
        }
    }
}


@Composable
fun CallActionsLayout(
    rows: List<CallActionRowModel>,
    modifier: Modifier = Modifier,
    verticalSpacing: Dp = 28.dp
) {

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        rows.forEach { row ->

            if (row.centerContent != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.actions.getOrNull(0)?.let {
                        CallActionIconButton(it)
                    }

                    row.centerContent.invoke()

                    row.actions.getOrNull(1)?.let {
                        CallActionIconButton(it)
                    }
                }
            } else {
                CallActionRow(actions = row.actions)
            }
        }
    }
}