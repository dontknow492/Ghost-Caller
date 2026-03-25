@file:Suppress("D")

package com.ghost.caller.ui.screens.call

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghost.caller.presentation.call.CallViewModel
import com.ghost.caller.viewmodel.call.CallEvent
import com.ghost.caller.viewmodel.call.ContactSuggestion
import com.ghost.caller.viewmodel.recent.CallLogViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Enhanced color palette
private val ButtonDarkGray = Color(0xFF2A2D34)
private val ButtonLightGray = Color(0xFF3A3D44)
private val CallGreen = Color(0xFF34C759)
private val CallRed = Color(0xFFFF3B30)
private val GradientStart = Color(0xFF1A1C22)
private val GradientEnd = Color(0xFF0F1117)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DialerScreen(
    viewModel: CallViewModel,
    onNavigateBack: () -> Unit,
    onCallInitiated: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var isCalling by remember { mutableStateOf(false) }

    // Animation states
    val buttonScale by animateFloatAsState(
        targetValue = if (isCalling) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = "CallButtonScale"
    )

    // Handle call button animation
    LaunchedEffect(state.isLoading) {
        if (state.isLoading) {
            isCalling = true
            delay(500)
            isCalling = false
        }
    }

    // Full screen background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientEnd),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp, top = 48.dp), // Adjust top padding for status bar
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Header: Back Button ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Navigate Back",
                        tint = Color.White
                    )
                }
            }

            // --- Upper Area: Suggestions ---
            // Using a Column with weight(1f) here pushes the dialpad perfectly to the bottom
            // and provides the required ColumnScope for AnimatedVisibility!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AnimatedVisibility(
                    visible = state.dialedNumber.isNotEmpty(),
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { 50 })
                ) {
                    ContactSuggestionsList(
                        suggestions = state.suggestions,
                        onContactSelected = { contact ->
                            viewModel.sendEvent(CallEvent.SelectContactSuggestion(contact))
                            haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Bottom Area: Number Display & Dialpad ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Number display with enhanced styling
                NumberDisplay(
                    number = state.dialedNumberFormatted,
                    onDelete = {
                        viewModel.sendEvent(CallEvent.DeleteDigit)
                        haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                    },
                    onClear = {
                        viewModel.sendEvent(CallEvent.ClearNumber)
                        haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Dialpad grid
                MainDialpadGrid(
                    onDigitClick = { digit ->
                        viewModel.sendEvent(CallEvent.AppendDigit(digit))
                        haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                    },
                    onLongPress = { digit ->
                        when (digit) {
                            "0" -> {
                                viewModel.sendEvent(CallEvent.AppendDigit("+"))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Call button with animations
                CallButton(
                    isEnabled = state.dialedNumber.isNotEmpty() && !state.isLoading,
                    isLoading = state.isLoading,
                    scale = buttonScale,
                    onClick = {
                        coroutineScope.launch {
                            isCalling = true
                            viewModel.sendEvent(CallEvent.InitiateCall)
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            delay(300)
                            onCallInitiated()
                            isCalling = false
                        }
                    }
                )
            }
        }

        // Error message overlay
        AnimatedVisibility(
            visible = state.error != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp)
        ) {
            ErrorMessage(
                message = state.error?.message ?: "",
                onDismiss = { viewModel.sendEvent(CallEvent.DismissError) }
            )
        }
    }
}

@Composable
private fun ContactSuggestionsList(
    suggestions: List<ContactSuggestion>,
    onContactSelected: (ContactSuggestion) -> Unit
) {
    if (suggestions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SearchOff,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "No contacts found",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 16.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(
                items = suggestions,
                key = { "${it.name}_${it.number}" }
            ) { contact ->
                ContactSuggestionRow(
                    contact = contact,
                    onClick = { onContactSelected(contact) }
                )
            }
        }
    }
}

@Composable
private fun NumberDisplay(
    number: String,
    onDelete: () -> Unit,
    onClear: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.ifEmpty { "Enter number" },
            color = if (number.isEmpty()) Color.White.copy(alpha = 0.3f) else Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        if (number.isNotEmpty()) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Backspace,
                        contentDescription = "Delete",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MainDialpadGrid(
    onDigitClick: (String) -> Unit,
    onLongPress: (String) -> Unit
) {
    val keys = listOf(
        listOf(DialKey("1", ""), DialKey("2", "ABC"), DialKey("3", "DEF")),
        listOf(DialKey("4", "GHI"), DialKey("5", "JKL"), DialKey("6", "MNO")),
        listOf(DialKey("7", "PQRS"), DialKey("8", "TUV"), DialKey("9", "WXYZ")),
        listOf(DialKey("*", ""), DialKey("0", "+"), DialKey("#", ""))
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        for (row in keys) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (key in row) {
                    DialpadButton(
                        number = key.number,
                        letters = key.letters,
                        onClick = { onDigitClick(key.number) },
                        onLongPress = { onLongPress(key.number) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DialpadButton(
    number: String,
    letters: String,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ), label = "DialpadScale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) ButtonLightGray else ButtonDarkGray,
        animationSpec = tween(100), label = "DialpadColor"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    isPressed = true
                    onClick()
                    isPressed = false
                }
            )
            .pointerInput(Unit) {
                // Wait for long press
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.pressed }) {
                            isPressed = true
                        } else {
                            isPressed = false
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = number,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = if (number == "0") 2.sp else 0.sp
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun CallButton(
    isEnabled: Boolean,
    isLoading: Boolean,
    scale: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(if (isEnabled) 80.dp else 76.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isEnabled) CallGreen else Color(0xFF2A2D34),
                shape = CircleShape
            )
            .clickable(enabled = isEnabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = Color.White,
                strokeWidth = 3.dp
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = "Call",
                tint = if (isEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun ContactSuggestionRow(
    contact: ContactSuggestion,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) ButtonLightGray else Color.Transparent,
        animationSpec = tween(100), label = "SuggestionBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    isPressed = true
                    onClick()
                    isPressed = false
                }
            )
            .padding(vertical = 12.dp, horizontal = 24.dp), // Increased horizontal padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with gradient
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(CallGreen, CallGreen.copy(alpha = 0.7f)),
                        start = Offset(0f, 0f),
                        end = Offset(0f, Float.POSITIVE_INFINITY)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (contact.photoUri != null) {
                AsyncImage(
                    model = contact.photoUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = contact.name.take(2).uppercase(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Contact info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contact.contactType ?: "Mobile",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
                Text(
                    text = "•",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 12.sp
                )
                Text(
                    text = formatPhoneNumber(contact.number),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Call icon
        Icon(
            imageVector = Icons.Rounded.Call,
            contentDescription = "Call",
            tint = CallGreen,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = CallRed.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Error,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 13.sp,
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// Helper data classes
private data class DialKey(
    val number: String,
    val letters: String
)

// Helper functions
private fun formatPhoneNumber(number: String): String {
    val cleaned = number.replace(Regex("[^\\d+]"), "")
    return when {
        cleaned.length <= 7 -> cleaned
        cleaned.length == 10 -> "${cleaned.substring(0, 3)}-${cleaned.substring(3, 6)}-${cleaned.substring(6)}"
        cleaned.length == 11 && cleaned.startsWith("1") ->
            "+1 ${cleaned.substring(1, 4)}-${cleaned.substring(4, 7)}-${cleaned.substring(7)}"
        else -> cleaned
    }
}