@file:Suppress("D")

package com.ghost.caller.ui.screens.recent

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.ghost.caller.ui.components.CallLogItem
import com.ghost.caller.ui.components.contact.SearchBar
import com.ghost.caller.viewmodel.call.CallLogEntry
import com.ghost.caller.viewmodel.recent.CallLogEvent
import com.ghost.caller.viewmodel.recent.CallLogSideEffect
import com.ghost.caller.viewmodel.recent.CallLogViewMode
import com.ghost.caller.viewmodel.recent.CallLogViewModel
import com.ghost.caller.viewmodel.recent.CallStatistics
import com.ghost.caller.viewmodel.recent.GroupedCallLog
import kotlinx.coroutines.flow.collectLatest

enum class CallAction {
    CALL, MESSAGE, INFO, DELETE, ADD_CONTACT, BLOCK, MARK_READ
}


@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
fun CallLogScreen(
    onNavigateToCall: (String) -> Unit,
    onNavigateToSms: (String) -> Unit,
    onNavigateToContact: (String) -> Unit,
    onNavigateToAddContact: (String, String?) -> Unit,
    onKeypadClick: () -> Unit,
    navigationBar: (@Composable () -> Unit)?,
    viewModel: CallLogViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    rememberCoroutineScope()
    val context = LocalContext.current

    val pagedCalls = viewModel.pagedCallLogs.collectAsLazyPagingItems()

    var isFilterDialogVisible by remember { mutableStateOf(false) }
    var isSortDialogVisible by remember { mutableStateOf(false) }


    // Handle side effects
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collectLatest { effect ->
            when (effect) {
                is CallLogSideEffect.NavigateToCall -> {
                    onNavigateToCall(effect.phoneNumber)
                }

                is CallLogSideEffect.NavigateToSms -> {
                    onNavigateToSms(effect.phoneNumber)
                }

                is CallLogSideEffect.NavigateToContact -> {
                    onNavigateToContact(effect.phoneNumber)
                }

                is CallLogSideEffect.NavigateToAddContact -> {
                    onNavigateToAddContact(effect.phoneNumber, effect.name)
                }

                is CallLogSideEffect.ShowToast -> {
                    Toast.makeText(
                        context,
                        effect.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is CallLogSideEffect.ShowError -> {
                    Toast.makeText(
                        context,
                        effect.message,
                        Toast.LENGTH_LONG
                    ).show()
                }

                is CallLogSideEffect.ShowDeleteConfirmation -> {
                    // Show delete confirmation dialog
                }

                is CallLogSideEffect.ShowClearAllConfirmation -> {
                    // Show clear all confirmation
                }

                is CallLogSideEffect.ShowBlockConfirmation -> {
                    // Show block confirmation dialog
                }

                is CallLogSideEffect.ExportCallLogs -> {
                    // Handle export
                }

                else -> {}
            }
        }
    }


    PullToRefreshBox(
        isRefreshing = pagedCalls.loadState.refresh is LoadState.Loading,
        onRefresh = { pagedCalls.refresh() },
    ) {
        Scaffold(
            topBar = {
                CallLogTopBar(
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = { query ->
                        if (query.isNotEmpty()) {
                            viewModel.sendEvent(CallLogEvent.SearchCallLogs(query))
                        } else {
                            viewModel.sendEvent(CallLogEvent.ClearSearch)
                        }
                    },
                    unreadCount = state.unreadCount,
                    onFilterClick = {
                        isFilterDialogVisible = true
                    },
                    onSortClick = {
                        isSortDialogVisible = true
                    },
                    onClearAllClick = {
                        viewModel.sendEvent(CallLogEvent.ClearCallLogs)
                    }
                )
            },
            bottomBar = {
                if (state.isInSelectionMode) {
                    CallLogSelectionBottomBar(
                        selectedCount = state.selectedCalls.size,
                        onCancel = { viewModel.sendEvent(CallLogEvent.ToggleSelectionMode(false)) },
                        onDelete = {
                            // Gather the selected items currently loaded in memory
                            val itemsToDelete = pagedCalls.itemSnapshotList.items.filter {
                                state.selectedCalls.contains(it.id)
                            }
                            viewModel.sendEvent(CallLogEvent.DeleteCallLogs(itemsToDelete))
                        },
                        onSelectAll = {
                            // Select all items currently loaded in the snapshot
                            pagedCalls.itemSnapshotList.items.forEach { call ->
                                if (!state.selectedCalls.contains(call.id)) {
                                    viewModel.sendEvent(CallLogEvent.ToggleCallSelection(call.id))
                                }
                            }
                        }
                    )
                } else {
                    navigationBar?.invoke()
                }
            },
            floatingActionButton = {
                if (!state.isInSelectionMode) {
                    FloatingActionButton(
                        onClick = onKeypadClick,
                        modifier = Modifier
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Quick Actions")
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    pagedCalls.loadState.refresh is LoadState.Loading && pagedCalls.itemCount == 0 -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text("Loading call logs...")
                            }
                        }
                    }

                    // 2. Error State
                    pagedCalls.loadState.refresh is LoadState.Error && pagedCalls.itemCount == 0 -> {
                        val error = (pagedCalls.loadState.refresh as LoadState.Error).error
                        ErrorView(
                            message = error.localizedMessage ?: "Failed to load call logs",
                            onRetry = { pagedCalls.retry() } // Use Paging's built-in retry!
                        )
                    }

                    // 3. Empty State
                    pagedCalls.itemCount == 0 && pagedCalls.loadState.refresh is LoadState.NotLoading -> {
                        EmptyCallLogsView(
                            onAddContact = {
                                onNavigateToAddContact("", null)
                            }
                        )
                    }

                    else -> {
                        when (state.viewMode) {
                            CallLogViewMode.LIST -> {
                                CallLogList(
                                    pagedCalls = pagedCalls,
                                    isSelectionMode = state.isInSelectionMode,
                                    selectedCalls = state.selectedCalls,
                                    onCallClick = { call ->
                                        viewModel.sendEvent(CallLogEvent.SelectCallLog(call))
                                    },
                                    onCallLongClick = { call ->
                                        if (!state.isInSelectionMode) {
                                            viewModel.sendEvent(CallLogEvent.ToggleSelectionMode(true))
                                            viewModel.sendEvent(CallLogEvent.ToggleCallSelection(call.id))
                                        }
                                    },
                                    onCallAction = { action, call ->
                                        when (action) {
                                            CallAction.CALL -> viewModel.sendEvent(
                                                CallLogEvent.MakeCall(
                                                    call.number
                                                )
                                            )

                                            CallAction.MESSAGE -> viewModel.sendEvent(
                                                CallLogEvent.SendSms(
                                                    call.number
                                                )
                                            )

                                            CallAction.INFO -> viewModel.sendEvent(
                                                CallLogEvent.ViewContact(
                                                    call.number
                                                )
                                            )

                                            CallAction.DELETE -> viewModel.sendEvent(
                                                CallLogEvent.DeleteCallLog(
                                                    call
                                                )
                                            )

                                            CallAction.ADD_CONTACT -> viewModel.sendEvent(
                                                CallLogEvent.AddToContacts(
                                                    call.number,
                                                    call.name
                                                )
                                            )

                                            CallAction.BLOCK -> viewModel.sendEvent(
                                                CallLogEvent.BlockNumber(
                                                    call.number
                                                )
                                            )

                                            CallAction.MARK_READ -> viewModel.sendEvent(
                                                CallLogEvent.MarkAsRead(
                                                    call
                                                )
                                            )
                                        }
                                    },
                                    formatDuration = { viewModel.formatDuration(it) },
                                    formatDate = { viewModel.formatDate(it) }
                                )
                            }

                            CallLogViewMode.GROUPED -> {
                                CallLogGroupedList(
                                    groupedCalls = state.groupedCallLogs,
                                    isSelectionMode = state.isInSelectionMode,
                                    selectedCalls = state.selectedCalls,
                                    onCallClick = { call ->
                                        viewModel.sendEvent(CallLogEvent.SelectCallLog(call))
                                    },
                                    onCallLongClick = { call ->
                                        if (!state.isInSelectionMode) {
                                            viewModel.sendEvent(CallLogEvent.ToggleSelectionMode(true))
                                            viewModel.sendEvent(CallLogEvent.ToggleCallSelection(call.id))
                                        }
                                    },
                                    onCallAction = { action, call ->
                                        when (action) {
                                            CallAction.CALL -> viewModel.sendEvent(
                                                CallLogEvent.MakeCall(
                                                    call.number
                                                )
                                            )

                                            CallAction.MESSAGE -> viewModel.sendEvent(
                                                CallLogEvent.SendSms(
                                                    call.number
                                                )
                                            )

                                            CallAction.INFO -> viewModel.sendEvent(
                                                CallLogEvent.ViewContact(
                                                    call.number
                                                )
                                            )

                                            CallAction.DELETE -> viewModel.sendEvent(
                                                CallLogEvent.DeleteCallLog(
                                                    call
                                                )
                                            )

                                            CallAction.ADD_CONTACT -> viewModel.sendEvent(
                                                CallLogEvent.AddToContacts(
                                                    call.number,
                                                    call.name
                                                )
                                            )

                                            CallAction.BLOCK -> viewModel.sendEvent(
                                                CallLogEvent.BlockNumber(
                                                    call.number
                                                )
                                            )

                                            CallAction.MARK_READ -> viewModel.sendEvent(
                                                CallLogEvent.MarkAsRead(
                                                    call
                                                )
                                            )
                                        }
                                    },
                                    formatDuration = { viewModel.formatDuration(it) },
                                    formatDate = { viewModel.formatDate(it) }
                                )
                            }

                            CallLogViewMode.STATISTICS -> {
                                CallLogStatisticsView(
                                    statistics = state.statistics,
                                    onBack = {
                                        viewModel.sendEvent(
                                            CallLogEvent.ChangeViewMode(
                                                CallLogViewMode.LIST
                                            )
                                        )
                                    }
                                )
                            }
                        }

                        // Loading more indicator
                        if (pagedCalls.loadState.append is LoadState.Loading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .align(Alignment.BottomCenter)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    if (isSortDialogVisible) {
        CallLogSortDialog(
            onDismiss = { isSortDialogVisible = false },
            currentSort = state.sortOrder,
            onApply = {
                viewModel.sendEvent(CallLogEvent.ChangeSortOrder(it))
                isSortDialogVisible = false
            }
        )
    }
    if (isFilterDialogVisible) {
        CallLogFilterDialog(
            onDismiss = { isFilterDialogVisible = false },
            currentDateRange = state.dateRange,
            currentType = state.selectedCallType,
            onApply = { logType, dateRange ->
                viewModel.sendEvent(CallLogEvent.FilterByDate(dateRange))
                viewModel.sendEvent(CallLogEvent.FilterByType(logType))
                isFilterDialogVisible = false
            }
        )
    }

}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CallLogList(
    pagedCalls: LazyPagingItems<CallLogEntry>,
    isSelectionMode: Boolean,
    selectedCalls: Set<String>,
    onCallClick: (CallLogEntry) -> Unit,
    onCallLongClick: (CallLogEntry) -> Unit,
    onCallAction: (CallAction, CallLogEntry) -> Unit, // Assuming CallAction is your enum/class
    formatDuration: (Long) -> String,
    formatDate: (Long) -> String
) {
    val lazyState = rememberLazyListState()

    LazyColumn(
        state = lazyState,
        verticalArrangement = Arrangement.spacedBy(1.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Use the Paging 3 specific items setup
        items(
            count = pagedCalls.itemCount,
            key = pagedCalls.itemKey { it.id }, // Ensures stable keys during pagination
            contentType = pagedCalls.itemContentType { "CallLog" }
        ) { index ->
            val call = pagedCalls[index]

            // 2. Handle potential nulls (if placeholders are enabled in PagingConfig)
            if (call != null) {
                CallLogItem(
                    modifier = Modifier.animateItem(),
                    call = call,
                    isSelected = selectedCalls.contains(call.id),
                    isSelectionMode = isSelectionMode,
                    onClick = { onCallClick(call) },
                    onLongClick = { onCallLongClick(call) },
                    onCallAction = { action -> onCallAction(action, call) },
                    formatDuration = formatDuration,
                    formatDate = formatDate
                )
            }
        }

        // 3. Show a loading spinner at the bottom when fetching the next page
        if (pagedCalls.loadState.append is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CallLogGroupedList(
    groupedCalls: List<GroupedCallLog>,
    isSelectionMode: Boolean,
    selectedCalls: Set<String>,
    onCallClick: (CallLogEntry) -> Unit,
    onCallLongClick: (CallLogEntry) -> Unit,
    onCallAction: (CallAction, CallLogEntry) -> Unit,
    formatDuration: (Long) -> String,
    formatDate: (Long) -> String
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        groupedCalls.forEach { group ->
            stickyHeader {
                GroupHeader(
                    date = group.date,
                    count = group.totalCount,
                    duration = group.totalDuration,
                    formatDuration = formatDuration
                )
            }

            items(
                items = group.calls,
                key = { it.id }
            ) { call ->
                CallLogItem(
                    call = call,
                    isSelected = selectedCalls.contains(call.id),
                    isSelectionMode = isSelectionMode,
                    onClick = { onCallClick(call) },
                    onLongClick = { onCallLongClick(call) },
                    onCallAction = { action -> onCallAction(action, call) },
                    formatDuration = formatDuration,
                    formatDate = formatDate,
//                    showDate = false
                )
            }
        }
    }
}

@Composable
fun GroupHeader(
    date: String,
    count: Int,
    duration: Long,
    formatDuration: (Long) -> String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$count calls",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDuration(duration),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallLogTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    unreadCount: Int,
    onFilterClick: () -> Unit,
    onSortClick: () -> Unit,
    onClearAllClick: () -> Unit
) {
    Column {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Call Log")
                    if (unreadCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            },
            actions = {
                IconButton(onClick = onFilterClick) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
                IconButton(onClick = onSortClick) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort")
                }
                IconButton(onClick = onClearAllClick) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                }
            },
        )
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onSearch = onSearchQueryChange,
            active = searchQuery.isNotEmpty(),
            onActiveChange = {},
            modifier = Modifier
                .fillMaxWidth()

        )
    }
}

@Composable
fun CallLogSelectionBottomBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onSelectAll: () -> Unit
) {
    BottomAppBar(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedCount selected",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onSelectAll) {
                    Text("Select All")
                }
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

    }
}

@Composable
fun CallLogStatisticsView(
    statistics: CallStatistics,
    onBack: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StatisticsHeader(onBack = onBack)
        }

        item {
            OverviewCards(statistics = statistics)
        }

        item {
            CallTypeBreakdown(statistics = statistics)
        }

        item {
            ActivityChart(
                data = statistics.callCountByDay,
                title = "Call Activity",
                formatValue = { "$it calls" }
            )
        }

        item {
            ActivityChart(
                data = statistics.callDurationByDay.mapValues { (_, value) -> value.toInt() },
                title = "Call Duration",
                formatValue = { formatDuration(it.toLong()) }
            )
        }

        if (statistics.mostActiveContact != null) {
            item {
                MostActiveContactCard(
                    name = statistics.mostActiveContact,
                    count = statistics.callCountByDay.values.maxOrNull() ?: 0
                )
            }
        }
    }
}

@Composable
fun StatisticsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            text = "Call Statistics",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun OverviewCards(statistics: CallStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Overview",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = statistics.totalCalls.toString(),
                    label = "Total Calls",
                    color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    value = formatDuration(statistics.totalDuration),
                    label = "Total Duration",
                    color = MaterialTheme.colorScheme.secondary
                )
                StatItem(
                    value = formatDuration(statistics.averageDuration),
                    label = "Average Duration",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun StatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CallTypeBreakdown(statistics: CallStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Call Type Breakdown",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            BreakdownItem(
                label = "Incoming",
                count = statistics.incomingCount,
                total = statistics.totalCalls,
                color = Color(0xFF4CAF50)
            )
            BreakdownItem(
                label = "Outgoing",
                count = statistics.outgoingCount,
                total = statistics.totalCalls,
                color = Color(0xFF2196F3)
            )
            BreakdownItem(
                label = "Missed",
                count = statistics.missedCount,
                total = statistics.totalCalls,
                color = Color(0xFFF44336)
            )
            BreakdownItem(
                label = "Rejected",
                count = statistics.rejectedCount,
                total = statistics.totalCalls,
                color = Color(0xFFFF9800)
            )
            BreakdownItem(
                label = "Blocked",
                count = statistics.blockedCount,
                total = statistics.totalCalls,
                color = Color(0xFF9C27B0)
            )
            BreakdownItem(
                label = "Voicemail",
                count = statistics.voicemailCount,
                total = statistics.totalCalls,
                color = Color(0xFF00BCD4)
            )
        }
    }
}

@Composable
fun BreakdownItem(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val percentage = if (total > 0) (count.toFloat() / total) * 100 else 0f

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = color
            )
            Text(
                text = "$count (${
                    String.format("%.1f", percentage)
                }%)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = percentage / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun ActivityChart(
    data: Map<String, Int>,
    title: String,
    formatValue: (Int) -> String
) {
    if (data.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            val maxValue = data.values.maxOrNull() ?: 1
            val sortedData = data.toList().sortedByDescending { it.first }.take(7)

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sortedData.forEach { (date, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = date,
                            fontSize = 12.sp,
                            modifier = Modifier.width(80.dp)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((value.toFloat() / maxValue).coerceAtMost(1f))
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }

                        Text(
                            text = formatValue(value),
                            fontSize = 12.sp,
                            modifier = Modifier.width(60.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MostActiveContactCard(
    name: String,
    count: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Most Active",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Most Active Contact",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$count calls",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun EmptyCallLogsView(
    onAddContact: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No Call Logs",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Your call history will appear here",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAddContact) {
                Text("Add Contact")
            }
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, secs)
        minutes > 0 -> String.format("%02d:%02d", minutes, secs)
        else -> String.format("%02ds", secs)
    }
}