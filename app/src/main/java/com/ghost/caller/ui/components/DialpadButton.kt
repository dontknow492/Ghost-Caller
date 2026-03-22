package com.ghost.caller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DisabledCallActionIconButton(
    icon: ImageVector,
    label: String,
    hideLabel: Boolean = false
) {
    val colors = MaterialTheme.colorScheme

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp) // slightly tighter
    ) {

        Box(
            modifier = Modifier
                .size(60.dp) // reduced from 68
                .clip(CircleShape)
                .background(
                    colors.surfaceVariant.copy(alpha = 0.6f) // subtle, not harsh white overlay
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(26.dp)
            )
        }

        if (!hideLabel) {
            Text(
                text = label,
                fontSize = 12.sp, // slightly smaller
                color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 6.dp),
                maxLines = 1
            )
        }
    }
}