@file:Suppress("D")

package com.ghost.caller.ui.screens.call

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Face2
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.ghost.caller.models.ContactQuickInfo
import com.ghost.caller.ui.components.ContactListItem
import com.ghost.caller.viewmodel.call.CallEvent
import com.ghost.caller.viewmodel.call.CallViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Enhanced color palette


@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
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

    val suggestions = viewModel.suggestions.collectAsLazyPagingItems()

    var isDialpadExpanded by remember { mutableStateOf(true) }

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Calls") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Navigate Back",
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isDialpadExpanded,
            ) {
                FloatingActionButton(
                    onClick = { isDialpadExpanded = true }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Dialpad,
                        contentDescription = "Open Dialpad"
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), // Adjust top padding for status bar
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Upper Area: Suggestions ---
            // Using a Column with weight(1f) here pushes the dialpad perfectly to the bottom
            // and provides the required ColumnScope for AnimatedVisibility!
            ContactSuggestionsList(
                modifier = Modifier.weight(1f),
                dialedPhoneNumber = state.dialedNumber,
                suggestions = suggestions,
                onContactSelected = { contact ->
                    viewModel.sendEvent(CallEvent.SelectContactSuggestion(contact))
                    haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = isDialpadExpanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { 50 })
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider()
                    AnimatedVisibility(
                        visible = state.dialedNumberFormatted.isNotEmpty()
                    ) {
                        // Number display with enhanced styling
                        NumberDisplay(
                            number = state.dialedNumberFormatted,
                            onDelete = {
                                viewModel.sendEvent(CallEvent.DeleteDigit)
                                haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                            },
                        )
                    }


                    Spacer(modifier = Modifier.height(12.dp))

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



                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { viewModel.sendEvent(CallEvent.ClearNumber) }
                        ) {
                            Text(
                                text = "C",
                                style = MaterialTheme.typography.headlineLarge
                            )
                        }
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
                        IconButton(
                            onClick = { isDialpadExpanded = false },
                            modifier = Modifier
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Dialpad,
                                contentDescription = "Hide Dialpad",
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                }
            }

        }

        // Error message overlay
        AnimatedVisibility(
            visible = state.error != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .padding(paddingValues)
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
    modifier: Modifier = Modifier,
    dialedPhoneNumber: String,
    suggestions: LazyPagingItems<ContactQuickInfo>,
    onContactSelected: (ContactQuickInfo) -> Unit
) {
    if (dialedPhoneNumber.isEmpty() && suggestions.itemCount == 0) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Face2,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Start typing to get Suggestion",
                    fontSize = 16.sp
                )
            }
        }
    } else if (suggestions.itemCount == 0) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "No contacts found",
                    fontSize = 16.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(
                count = suggestions.itemCount,
                key = { index -> suggestions[index]?.id ?: index }
            ) { index ->
                suggestions[index]?.let { contactQuickInfo ->
                    ContactListItem(
                        modifier = Modifier,
                        contact = contactQuickInfo,
                        onClick = {
                            onContactSelected(contactQuickInfo)
                        },
                        isSelectionMode = false,
                        onLongClick = {},
                        onFavoriteClick = {},
                        onCallClick = {
                            onContactSelected(contactQuickInfo)
                        },
                        isSelected = false
                    )
                }
            }
        }

    }
}

@Composable
private fun NumberDisplay(
    number: String,
    onDelete: () -> Unit,
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
            color = MaterialTheme.colorScheme.primary,
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
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Backspace,
                        contentDescription = "Delete",
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
        DialKey("1", ""), DialKey("2", "ABC"), DialKey("3", "DEF"),
        DialKey("4", "GHI"), DialKey("5", "JKL"), DialKey("6", "MNO"),
        DialKey("7", "PQRS"), DialKey("8", "TUV"), DialKey("9", "WXYZ"),
        DialKey("*", ""), DialKey("0", "+"), DialKey("#", "")
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        items(keys) { key ->
            DialpadButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.7f),
                number = key.number,
                letters = key.letters,
                onClick = { onDigitClick(key.number) },
                onLongPress = { onLongPress(key.number) }
            )
        }
    }
}

@Composable
private fun DialpadButton(
    modifier: Modifier = Modifier,
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


    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
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
                        isPressed = event.changes.any { it.pressed }
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
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = if (number == "0") 2.sp else 0.sp
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
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
                if (isEnabled) MaterialTheme.colorScheme.primaryContainer else Color(0xFF2A2D34),
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
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp)
            )
        }
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
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
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
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = message,
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
        cleaned.length == 10 -> "${cleaned.substring(0, 3)}-${
            cleaned.substring(
                3,
                6
            )
        }-${cleaned.substring(6)}"

        cleaned.length == 11 && cleaned.startsWith("1") ->
            "+1 ${cleaned.substring(1, 4)}-${cleaned.substring(4, 7)}-${cleaned.substring(7)}"

        else -> cleaned
    }
}