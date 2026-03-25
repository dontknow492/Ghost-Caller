@file:Suppress("D")

package com.ghost.caller.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VoiceChat
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.CallMissed
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Badge
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghost.caller.models.ContactType
import com.ghost.caller.ui.components.contact.ContactAvatar
import com.ghost.caller.ui.screens.recent.CallAction
import com.ghost.caller.viewmodel.call.CallLogEntry
import com.ghost.caller.viewmodel.call.CallType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CallLogItem(
    modifier: Modifier = Modifier,
    call: CallLogEntry,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCallAction: (CallAction) -> Unit,
    formatDuration: (Long) -> String,
    formatDate: (Long) -> String,
) {
    var showActions by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .animateContentSize(),
        tonalElevation = if (isSelected) 2.dp else 0.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                // ✅ Selection
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // ✅ Avatar + Call Type Badge
                CallAvatarWithBadge(call)

                Spacer(modifier = Modifier.width(12.dp))

                // ✅ Main Content
                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = call.name ?: call.number,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 🔹 Call type + meta info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {

                        Text(
                            text = getCallTypeText(call),
                            style = MaterialTheme.typography.bodySmall,
                            color = getCallTypeColor(call)
                        )

                        Text(
                            text = "• ${formatDate(call.timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (call.durationSeconds > 0) {
                            Text(
                                text = "• ${formatDuration(call.durationSeconds)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                // ✅ Quick Call Button (important UX)
                if (!isSelectionMode) {
                    IconButton(onClick = { onCallAction(CallAction.CALL) }) {
                        Icon(
                            Icons.Rounded.Call,
                            contentDescription = "Call",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 🔥 Expandable Actions (cleaner spacing)
            AnimatedVisibility(
                visible = showActions,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ActionButton(Icons.AutoMirrored.Rounded.Message, "SMS") {
                        onCallAction(CallAction.MESSAGE)
                    }
                    ActionButton(Icons.Rounded.Info, "Info") {
                        onCallAction(CallAction.INFO)
                    }
                    ActionButton(Icons.Rounded.Delete, "Delete", tint = MaterialTheme.colorScheme.error) {
                        onCallAction(CallAction.DELETE)
                    }
                }
            }
        }
    }
}

@Composable
fun CallAvatarWithBadge(call: CallLogEntry) {
    Box {
        ContactAvatar(
            photoUri = null, // you can pass real if available
            initials = (call.name ?: call.number).take(2),
            contactType = ContactType.OTHER,
            size = 48.dp
        )

        // 🔥 Call type badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (call.type) {
                    CallType.INCOMING -> Icons.AutoMirrored.Rounded.CallReceived
                    CallType.OUTGOING -> Icons.AutoMirrored.Rounded.CallMade
                    CallType.MISSED -> Icons.AutoMirrored.Rounded.CallMissed
                    else -> Icons.Rounded.Phone
                },
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = getCallTypeColor(call)
            )
        }
    }
}

fun getCallTypeText(call: CallLogEntry): String {
    return when (call.type) {
        CallType.INCOMING -> "Incoming"
        CallType.OUTGOING -> "Outgoing"
        CallType.MISSED -> "Missed"
        CallType.REJECTED -> "Rejected"
        CallType.BLOCKED -> "Blocked"
        CallType.VOICEMAIL -> "Voicemail"
        else -> "Call"
    }
}


@Composable
fun getCallTypeColor(call: CallLogEntry): Color {
    return when (call.type) {
        CallType.MISSED -> MaterialTheme.colorScheme.error
        CallType.INCOMING -> MaterialTheme.colorScheme.primary
        CallType.OUTGOING -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}


@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,

) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = tint
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint
            )
        }
    }
}