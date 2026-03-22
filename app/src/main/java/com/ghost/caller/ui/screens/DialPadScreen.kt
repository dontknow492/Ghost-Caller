@file:Suppress("D")

package com.ghost.caller.ui.screens

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghost.caller.R
import com.ghost.caller.ui.components.DialPadButtonWithHaptics
import com.ghost.caller.ui.components.DialPadKey
import com.ghost.caller.ui.components.ModernSearchTextField
import com.ghost.caller.viewmodel.CallLogEntry
import com.ghost.caller.viewmodel.CallType
import com.ghost.caller.viewmodel.CallViewModel
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialPadScreen(viewModel: CallViewModel, permissionsGranted: Boolean) {
    val configuration = LocalConfiguration.current
    // 1. Detect if we have a wide screen (Landscape phone or Tablet)
    val isWideScreen = configuration.screenWidthDp >= 600

    // State for dial pad visibility
    var isDialPadExpanded by remember { mutableStateOf(true) }

    // Force the dial pad to always be expanded on wide screens
    val actualDialPadState = if (isWideScreen) true else isDialPadExpanded

    val materialTheme = MaterialTheme

    Scaffold(
        floatingActionButton = {
            // 2. Hide the FAB entirely on wide screens since the dialpad is permanently visible
            if (!isWideScreen) {
                FloatingActionButton(
                    onClick = { isDialPadExpanded = !isDialPadExpanded },
                    containerColor = materialTheme.colorScheme.primaryContainer,
                    contentColor = materialTheme.colorScheme.onPrimaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        if (isDialPadExpanded) Icons.Default.KeyboardArrowDown
                        else Icons.Default.Phone,
                        contentDescription = if (isDialPadExpanded) "Hide dial pad" else "Show dial pad",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->

        // 3. THE RESPONSIVE LAYOUT SWITCH
        if (isWideScreen) {
            // --- WIDESCREEN / LANDSCAPE MODE ---
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Dialpad on the Left (Fixed width so it doesn't stretch weirdly on huge tablets)
                Box(
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Dialpad(
                        viewModel = viewModel,
                        isDialPadExpanded = actualDialPadState
                    )
                }

                // A subtle divider line between the dial pad and the call list
                VerticalDivider(color = materialTheme.colorScheme.outlineVariant)

                // Recent Calls on the Right (Takes up all remaining space)
                RecentCallsSection(
                    viewModel = viewModel,
                    permissionsGranted = permissionsGranted,
                    modifier = Modifier.weight(1f),
                    onDialpadRequest = {
                        isDialPadExpanded = true
                    } // Ignored on widescreen, but needed for the callback
                )
            }
        } else {
            // --- STANDARD PORTRAIT MODE ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Recent Calls takes the top space
                RecentCallsSection(
                    viewModel = viewModel,
                    permissionsGranted = permissionsGranted,
                    modifier = Modifier.weight(1f),
                    onDialpadRequest = { isDialPadExpanded = true }
                )

                // Dialpad on the bottom
                Dialpad(
                    viewModel = viewModel,
                    isDialPadExpanded = actualDialPadState,
                    modifier = Modifier
                    // Note: Ensure your Dialpad's internal Column handles its own background/clip as discussed previously!
                )
            }
        }
    }
}

// 4. EXTRACTED RECENT CALLS CONTENT
// By extracting this, we don't have to duplicate the LazyColumn code for both the Row and the Column!
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentCallsSection(
    viewModel: CallViewModel,
    permissionsGranted: Boolean,
    modifier: Modifier = Modifier,
    onDialpadRequest: () -> Unit,

) {
    val context = LocalContext.current
    val recentCalls by viewModel.recentCallsFiltered.collectAsState()
    val materialTheme = MaterialTheme
    val searchQuery by viewModel.searchQuery.collectAsState()


    val groupedCalls = remember(recentCalls) {
        groupCallsByDate(recentCalls) // Ensure this helper function is available in your scope
    }


    Column(
        modifier = modifier
    ) {
        CenterAlignedTopAppBar(
            title = { Text("Recent Calls") }
        )
        ModernSearchTextField(
            value = searchQuery,
            onValueChange = { viewModel.filterValueChange(it) },
            onSearch = { viewModel.filterValueChange(searchQuery) },
            placeholder = "Search calls...",
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Box(modifier = Modifier) {
            if (permissionsGranted && recentCalls.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    groupedCalls.forEach { (dateHeader, calls) ->
                        item {
                            DateHeader(dateHeader)
                        }
                        items(calls, key = { it.timestamp }) { call ->
                            RecentCallItem(
                                modifier = Modifier.animateItem(), // Note: animateItem() requires Compose Foundation 1.7+
                                call = call,
                                onClick = {
                                    viewModel.setNumber(call.number)
                                    onDialpadRequest()
                                },
                                onCallClick = {
                                    viewModel.setNumber(call.number)
                                    viewModel.initiateCall(context, makeRealCall = true)
                                },
                                onDeleteClick = {
                                    viewModel.deleteCallLog(call)
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            } else if (permissionsGranted && recentCalls.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        tint = materialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No recent calls",
                        color = materialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else if (!permissionsGranted) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = materialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Allow call log permission to see recent calls",
                        color = materialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun Dialpad(
    viewModel: CallViewModel,
    isDialPadExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dialedNumber by viewModel.dialedNumber.collectAsState()

    val keys = remember {
        listOf(
            listOf(
                DialPadKey("1", listOf("=")),
                DialPadKey("2", listOf("A", "B", "C")),
                DialPadKey("3", listOf("D", "E", "F"))
            ),
            listOf(
                DialPadKey("4", listOf("G", "H", "I")),
                DialPadKey("5", listOf("J", "K", "L")),
                DialPadKey("6", listOf("M", "N", "O"))
            ),
            listOf(
                DialPadKey("7", listOf("P", "Q", "R", "S")),
                DialPadKey("8", listOf("T", "U", "V")),
                DialPadKey("9", listOf("W", "X", "Y", "Z"))
            ),
            listOf(
                DialPadKey("*", listOf(",")),
                DialPadKey("0", listOf("+")),
                DialPadKey("#", listOf("-w"))
            )
        )
    }

    AnimatedVisibility(
        visible = isDialPadExpanded,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),   // from bottom
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),    // to bottom
        modifier = modifier
            .clip(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                )
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(vertical = 6.dp)
        ) {
            HorizontalDivider()

            Spacer(Modifier.height(12.dp))

            // Top display (more compact)
            AnimatedVisibility(
                visible = dialedNumber.isNotEmpty(),
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 })
            ) {
                val contactName = viewModel.getContactName(dialedNumber)

                Column(
                    modifier = Modifier
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Dialed number (main focus)
                    Text(
                        text = dialedNumber,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace
                    )

                    // Contact name (secondary)
                    if (!contactName.isNullOrBlank()) {
                        Text(
                            text = contactName,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            // Compact grid
            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { key ->
                        DialPadButtonWithHaptics(
                            key = key,
                            onClick = { viewModel.appendDigit(key.number) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // --- IMPROVED BOTTOM ACTIONS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // 1. Simulated Call (Left side - Replacing the empty Box)
                FloatingActionButton(
                    onClick = { viewModel.initiateCall(context, makeRealCall = false) },
                    modifier = Modifier.size(48.dp), // Slightly smaller to indicate secondary action
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp), // Flatter look
                    shape = CircleShape
                ) {
                    // Outlined icon helps differentiate it from the real call button
                    Icon(
                        Icons.Outlined.Call,
                        contentDescription = "Simulated Call",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 2. Real Call (Center - Main focus)
                FloatingActionButton(
                    onClick = { viewModel.initiateCall(context, makeRealCall = true) },
                    modifier = Modifier.size(64.dp), // Larger to draw the eye
                    containerColor = Color(0xFF4CAF50), // Classic dialer green
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = "Real Call",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // 3. Delete / Backspace (Right side)
                Box(
                    modifier = Modifier.size(48.dp), // Kept at 48dp to perfectly balance the left button!
                    contentAlignment = Alignment.Center
                ) {
                    if (dialedNumber.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.deleteDigit() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                ImageVector.vectorResource(R.drawable.backspace),
                                contentDescription = "Delete",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


// Date header composable
@Composable
fun DateHeader(date: String) {
    val materialTheme = MaterialTheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date,
            color = materialTheme.colorScheme.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.width(8.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.5.dp,
            color = materialTheme.colorScheme.outlineVariant
        )
    }
}


// Helper function to group calls by date
private fun groupCallsByDate(calls: List<CallLogEntry>): List<Pair<String, List<CallLogEntry>>> {
    Calendar.getInstance()
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return calls.groupBy { call ->
        val callDate = Calendar.getInstance().apply { timeInMillis = call.timestamp }
        when {
            isSameDay(callDate, today) -> "Today"
            isSameDay(callDate, yesterday) -> "Yesterday"
            else -> {
                val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
                dateFormat.format(Date(call.timestamp))
            }
        }
    }.toList()
        .sortedByDescending { (_, calls) -> calls.maxOfOrNull { it.timestamp } ?: 0L }
}

// Helper function to check if two dates are the same day
private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

// Optional: Add these to your theme if not already present
@Composable
fun CustomDialPadTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
//        typography = Typography(),
        content = content
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentCallItem(
    call: CallLogEntry,
    onClick: () -> Unit,
    onCallClick: () -> Unit = {},
    onDeleteClick: () -> Unit, // 1. Added Delete Action,
    modifier: Modifier = Modifier
) {
    val materialTheme = MaterialTheme

    // --- (Your existing data formatting logic remains exactly the same) ---
    val baseName = if (call.name.isNullOrBlank()) call.number else call.name
    val displayName = if (call.groupedCount > 1) "$baseName (${call.groupedCount})" else baseName
    val initial = baseName.trim().firstOrNull()?.uppercase() ?: "#"

    val isUnreadMissed = call.type == CallType.MISSED && !call.isRead
    val nameColor =
        if (isUnreadMissed) materialTheme.colorScheme.error else materialTheme.colorScheme.onSurface
    val nameWeight = if (isUnreadMissed) FontWeight.Bold else FontWeight.Medium

    val timeText = remember(call.timestamp) {
        val diff = System.currentTimeMillis() - call.timestamp
        when {
            diff < 60_000 -> "Just now"
            diff < 60 * 60_000 -> "${diff / 60_000} min ago"
            diff < 24 * 60 * 60_000 -> SimpleDateFormat("hh:mm a", Locale.getDefault()).format(
                Date(
                    call.timestamp
                )
            )

            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(call.timestamp))
        }
    }

    val subText = remember(call.durationSeconds, call.location, call.type, call.name) {
        val durationStr =
            if (call.durationSeconds > 0) DateUtils.formatElapsedTime(call.durationSeconds) else ""
        when {
            !call.name.isNullOrBlank() && durationStr.isNotEmpty() -> durationStr
            call.name.isNullOrBlank() && !call.location.isNullOrBlank() && durationStr.isNotEmpty() -> "${call.location} • $durationStr"
            call.name.isNullOrBlank() && !call.location.isNullOrBlank() -> call.location
            call.type == CallType.REJECTED -> "Declined"
            call.type == CallType.BLOCKED -> "Blocked"
            durationStr.isNotEmpty() -> durationStr
            else -> "Ringing"
        }
    }

    val callIcon = when (call.type) {
        CallType.INCOMING -> ImageVector.vectorResource(R.drawable.call_received)
        CallType.OUTGOING -> ImageVector.vectorResource(R.drawable.call_made)
        CallType.MISSED -> ImageVector.vectorResource(R.drawable.call_missed)
        CallType.REJECTED -> Icons.Default.Close
        CallType.BLOCKED -> ImageVector.vectorResource(R.drawable.block_24px)
        CallType.VOICEMAIL -> ImageVector.vectorResource(R.drawable.voicemail_24px)
        else -> Icons.Default.Call
    }

    val iconColor =
        if (call.type == CallType.MISSED || call.type == CallType.REJECTED || call.type == CallType.BLOCKED) {
            materialTheme.colorScheme.error
        } else {
            materialTheme.colorScheme.onSurfaceVariant
        }

    // 2. THE SWIPE STATE LOGIC
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swiped Right -> Call
                    onCallClick()
                    false // Return false so it bounces back to center instead of disappearing
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    // Swiped Left -> Delete
                    onDeleteClick()
                    true // Return true so it visually slides off the screen
                }

                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    // 3. THE SWIPE WRAPPER
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        // This is what shows up BEHIND the item when you swipe
        backgroundContent = {
            val backgroundColor by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50) // Green for Call
                    SwipeToDismissBoxValue.EndToStart -> materialTheme.colorScheme.error // Red for Delete
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                label = "swipe_color"
            )

            val iconAlignment = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.Settled -> Alignment.Center

            }

            val actionIcon = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Call
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                SwipeToDismissBoxValue.Settled -> null
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = iconAlignment
            ) {
                if (actionIcon != null) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        // This is your actual Call Item UI
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // IMPORTANT: You must give the row a solid background so the swipe colors underneath don't bleed through
                    .background(materialTheme.colorScheme.background)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(materialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = materialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Info Column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = displayName,
                        color = nameColor,
                        fontSize = 16.sp,
                        fontWeight = nameWeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.alpha(0.8f)
                    ) {
                        Icon(
                            imageVector = callIcon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(14.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "$timeText • $subText",
                            color = materialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!call.name.isNullOrBlank()) {
                        Text(
                            text = call.number,
                            color = materialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Action Button (Still keeping the quick-tap button on the right!)
                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call Back",
                        tint = materialTheme.colorScheme.primary
                    )
                }
            }
        }
    )
}
