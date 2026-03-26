@file:Suppress("D")

package com.ghost.caller.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ghost.caller.models.ContactType
import com.ghost.caller.ui.screens.recent.CallAction
import com.ghost.caller.viewmodel.call.CallLogEntry
import com.ghost.caller.viewmodel.call.getCallTypeColor
import com.ghost.caller.viewmodel.call.getCallTypeContainerColor
import com.ghost.caller.viewmodel.call.getCallTypeIcon
import com.ghost.caller.viewmodel.call.getCallTypeText


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val haptic = LocalHapticFeedback.current

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {

                // 👉 Swipe Right → Call
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCallAction(CallAction.CALL)
                    false // ❗ don't dismiss
                }

                // 👉 Swipe Left → Delete
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCallAction(CallAction.DELETE)
                    true // dismiss item
                }

                else -> false
            }
        },
        positionalThreshold = { totalDistance ->
            totalDistance * 0.3f   // ✅ 40% swipe required
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !isSelectionMode,
        enableDismissFromEndToStart = !isSelectionMode,
        backgroundContent = {
            DualSwipeBackground(dismissState)
        }
    ) {

        CallLogItemContent(
            modifier = modifier,
            call = call,
            isSelected = isSelected,
            isSelectionMode = isSelectionMode,
            onClick = onClick,
            onLongClick = onLongClick,
            onCallAction = onCallAction,
            formatDuration = formatDuration,
            formatDate = formatDate
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallLogItemContent(
    modifier: Modifier,
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

    val title = rememberSaveable(call.name, call.number) {
        if(call.name.isNullOrEmpty() or call.name.isNullOrBlank()) call.number
        else call.name ?: "Unknown"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
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

            Row(verticalAlignment = Alignment.CenterVertically) {

                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                CallAvatarWithBadge(call)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        // Optional: You can add basicMarquee() here too if long names get cut off!
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                            initialDelayMillis = 3000, // Wait 3 seconds before starting the first scroll
                            repeatDelayMillis = 2000, // Wait 2 seconds between loops
                            velocity = 40.dp, // Smooth, slow speed
                            spacing = MarqueeSpacing(24.dp) // Clear gap between loops
                        ),
                    )

                    // 🔥 1. Combine all the info into a single formatted string
                    val callTypeColor = getCallTypeColor(call)
                    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant

                    val subtitleText = buildAnnotatedString {
                        withStyle(SpanStyle(color = callTypeColor)) {
                            append(getCallTypeText(call))
                        }
                        withStyle(SpanStyle(color = subtitleColor)) {
                            append(" • ${formatDate(call.timestamp)}")
                            if (call.durationSeconds > 0) {
                                append(" • ${formatDuration(call.durationSeconds)}")
                            }
                        }
                    }

                    // 🔥 2. Use a single Text with basicMarquee()
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        // basicMarquee automatically scrolls the text horizontally ONLY if it doesn't fit!
                        modifier = Modifier.basicMarquee()
                    )
                }

                if (!isSelectionMode) {
                    IconButton(onClick = { onCallAction(CallAction.CALL) }) {
                        Icon(
                            Icons.Rounded.Call,
                            contentDescription = "Call",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = { showActions = !showActions }) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "More"
                    )
                }
            }

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
                    ActionButton(
                        Icons.Rounded.Delete,
                        "Delete",
                        tint = MaterialTheme.colorScheme.error
                    ) {
                        onCallAction(CallAction.DELETE)
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualSwipeBackground(state: SwipeToDismissBoxState) {

    val direction = state.dismissDirection
    val progress = state.progress

    val isCall = direction == SwipeToDismissBoxValue.StartToEnd
    val isDelete = direction == SwipeToDismissBoxValue.EndToStart

    val color by animateColorAsState(
        when {
            isCall -> MaterialTheme.colorScheme.primary
            isDelete -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 20.dp),
        contentAlignment = when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
            else -> Alignment.Center
        }
    ) {

        val icon = when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> Icons.Rounded.Call
            SwipeToDismissBoxValue.EndToStart -> Icons.Rounded.Delete
            else -> Icons.Rounded.MoreHoriz
        }

        val tint = when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onPrimary
            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onError
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(24.dp)
                .scale(0.8f + (progress * 0.5f)) // 🔥 smooth grow
        )
    }
}


@Composable
fun CallAvatarWithBadge(call: CallLogEntry) {

    val bgColor = getCallTypeContainerColor(call.type)

    Box {

        // 🔥 Avatar with subtle tint
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                )
                .size(48.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            ContactAvatar(
                photoUri = null,
                initials = (call.name ?: call.number).take(2),
                contactType = ContactType.OTHER,
                size = 48.dp
            )
        }

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
                imageVector = getCallTypeIcon(call.type),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = getCallTypeColor(call)
            )
        }
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