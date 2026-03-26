@file:Suppress("D")

package com.ghost.caller.viewmodel.recent

import android.app.Application
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ghost.caller.repository.CallLogRepository
import com.ghost.caller.viewmodel.call.CallLogEntry
import com.ghost.caller.viewmodel.call.CallType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import java.util.Locale

class CallLogViewModel(
    application: Application,
    private val callLogRepository: CallLogRepository
) : AndroidViewModel(application) {

    // UI State Flow
    private val _state = MutableStateFlow(CallLogState())
    val state: StateFlow<CallLogState> = _state.asStateFlow()

    // Side effects channel
    private val _sideEffect = MutableSharedFlow<CallLogSideEffect>()
    val sideEffect: SharedFlow<CallLogSideEffect> = _sideEffect.asSharedFlow()

    // Event channel
    private val _event = MutableSharedFlow<CallLogEvent>()

    // Filter flows for Paging
    private val _callTypeFilter = MutableStateFlow(CallTypeFilter.ALL)
    private val _dateRangeFilter = MutableStateFlow(DateRange.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)

    // --- PAGING DATA FLOW ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedCallLogs: Flow<PagingData<CallLogEntry>> = combine(
        _callTypeFilter,
        _dateRangeFilter,
        _searchQuery,
        _sortOrder
    ) { type, range, query, sort ->
        // Package filters together
        FilterParams(type, range, query, sort)
    }.flatMapLatest { params ->
        Timber.d("Refreshing Paged Calls with params: $params")
        callLogRepository.getPagedRecentCalls(
            filterType = mapCallTypeFilter(params.type),
            dateRange = params.dateRange,
            searchQuery = params.query.ifBlank { null },
            sortOrder = params.sortOrder
        )
    }.cachedIn(viewModelScope)

    // Internal data class for combine
    private data class FilterParams(
        val type: CallTypeFilter,
        val dateRange: DateRange,
        val query: String,
        val sortOrder: SortOrder
    )

    init {
        viewModelScope.launch {
            _event.collect { event -> handleEvent(event) }
        }

        // Load initial non-paged data
        loadStatistics()
        checkUnreadCount()
    }

    fun sendEvent(event: CallLogEvent) {
        viewModelScope.launch { _event.emit(event) }
    }

    private fun handleEvent(event: CallLogEvent) {
        when (event) {
            is CallLogEvent.LoadCallLogs,
            is CallLogEvent.LoadMoreCallLogs -> Timber.d("Manual load events are handled automatically by Paging.")

            is CallLogEvent.SearchCallLogs -> searchCallLogs(event.query)
            is CallLogEvent.FilterByType -> filterByType(event.type)
            is CallLogEvent.FilterByDate -> filterByDate(event.dateRange)
            is CallLogEvent.SelectCallLog -> selectCallLog(event.callLog)
            is CallLogEvent.DeleteCallLog -> deleteCallLog(event.callLog)
            is CallLogEvent.DeleteCallLogs -> deleteCallLogs(event.callLogs)
            is CallLogEvent.ClearCallLogs -> clearCallLogs()
            is CallLogEvent.ToggleSelectionMode -> toggleSelectionMode(event.enabled)
            is CallLogEvent.ToggleCallSelection -> toggleCallSelection(event.callId)
            is CallLogEvent.ClearSearch -> clearSearch()
            is CallLogEvent.ClearError -> clearError()
            is CallLogEvent.ClearSuccessMessage -> clearSuccessMessage()
            is CallLogEvent.ChangeSortOrder -> changeSortOrder(event.sortOrder)
            is CallLogEvent.ChangeViewMode -> changeViewMode(event.viewMode)
            is CallLogEvent.MakeCall -> makeCall(event.phoneNumber)
            is CallLogEvent.SendSms -> sendSms(event.phoneNumber)
            is CallLogEvent.ViewContact -> viewContact(event.phoneNumber)
            is CallLogEvent.AddToContacts -> addToContacts(event.phoneNumber, event.name)
            is CallLogEvent.BlockNumber -> blockNumber(event.phoneNumber)
            is CallLogEvent.MarkAsRead -> markAsRead(event.callLog)
            is CallLogEvent.MarkAllAsRead -> markAllAsRead()
            is CallLogEvent.ExportCallLogs -> exportCallLogs()
            is CallLogEvent.Refresh -> refresh()
            is CallLogEvent.Retry -> Timber.d("Retry should be called on the LazyPagingItems object in UI.")
        }
    }

    // --- FILTER METHODS ---

    private fun searchCallLogs(query: String) {
        _state.update { it.copy(searchQuery = query) }
        _searchQuery.value = query // Triggers Paging refresh
    }

    private fun filterByType(type: CallTypeFilter) {
        _state.update { it.copy(selectedCallType = type) }
        _callTypeFilter.value = type
    }

    private fun filterByDate(dateRange: DateRange) {
        _state.update { it.copy(dateRange = dateRange) }
        _dateRangeFilter.value = dateRange
    }

    private fun changeSortOrder(sortOrder: SortOrder) {
        _state.update { it.copy(sortOrder = sortOrder) }
        _sortOrder.value = sortOrder
    }

    private fun clearSearch() {
        _state.update { it.copy(searchQuery = "") }
        _searchQuery.value = ""
    }

    private fun mapCallTypeFilter(filter: CallTypeFilter): CallType? {
        return when (filter) {
            CallTypeFilter.ALL -> null
            CallTypeFilter.INCOMING -> CallType.INCOMING
            CallTypeFilter.OUTGOING -> CallType.OUTGOING
            CallTypeFilter.MISSED -> CallType.MISSED
            CallTypeFilter.REJECTED -> CallType.REJECTED
            CallTypeFilter.BLOCKED -> CallType.BLOCKED
            CallTypeFilter.VOICEMAIL -> CallType.VOICEMAIL
        }
    }

    // --- DATA MUTATION METHODS ---

    private fun deleteCallLog(callLog: CallLogEntry) {
        viewModelScope.launch {
            try {
                val success = callLogRepository.deleteCallLog(callLog)
                if (success) {
                    sendSideEffect(CallLogSideEffect.CallLogDeleted(callLog))
                    loadStatistics()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete call log")
                sendSideEffect(CallLogSideEffect.ShowError("Failed to delete call log"))
            }
        }
    }

    private fun deleteCallLogs(callLogs: List<CallLogEntry>) {
        viewModelScope.launch {
            try {
                val deletedCount = callLogRepository.deleteCallLogs(callLogs)
                if (deletedCount > 0) {
                    _state.update { it.copy(isInSelectionMode = false, selectedCalls = emptySet()) }
                    sendSideEffect(CallLogSideEffect.ShowToast("$deletedCount call logs deleted"))
                    sendSideEffect(CallLogSideEffect.CallLogsDeleted(deletedCount))
                    loadStatistics()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete call logs")
                sendSideEffect(CallLogSideEffect.ShowError("Failed to delete call logs"))
            }
        }
    }

    private fun clearCallLogs() {
        sendSideEffect(CallLogSideEffect.ShowClearAllConfirmation)
    }

    fun confirmClearAllCallLogs() {
        viewModelScope.launch {
            try {
                val deletedCount = callLogRepository.clearAllCallLogs()
                if (deletedCount > 0) {
                    _state.update { it.copy(isInSelectionMode = false, selectedCalls = emptySet()) }
                    sendSideEffect(CallLogSideEffect.ShowToast("All call logs cleared"))
                    sendSideEffect(CallLogSideEffect.CallLogsCleared(deletedCount))
                    loadStatistics()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear call logs")
                sendSideEffect(CallLogSideEffect.ShowError("Failed to clear call logs"))
            }
        }
    }


    private fun markAsRead(callLog: CallLogEntry) {
        viewModelScope.launch {
            try {
                if (callLogRepository.markAsRead(callLog)) {
                    checkUnreadCount()
                    // UI needs to call refresh() on paging items if visual update is strictly required
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to mark as read")
                sendSideEffect(CallLogSideEffect.ShowError("Failed to mark as read"))
            }
        }
    }

    private fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val updatedCount = callLogRepository.markAllAsRead()
                if (updatedCount > 0) {
                    sendSideEffect(CallLogSideEffect.ShowToast("All calls marked as read"))
                    checkUnreadCount()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to mark all as read")
                sendSideEffect(CallLogSideEffect.ShowError("Failed to mark all as read"))
            }
        }
    }

    // --- VIEW MODE & STATISTICS ---

    private fun changeViewMode(viewMode: CallLogViewMode) {
        if (viewMode == CallLogViewMode.STATISTICS) {
            loadStatistics()
        } else if (viewMode == CallLogViewMode.GROUPED) {
            loadGroupedCallLogs()
        }
        _state.update { it.copy(viewMode = viewMode) }
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                val statistics = callLogRepository.getCallStatistics()
                _state.update { it.copy(statistics = statistics) }
                sendSideEffect(CallLogSideEffect.ShowStatistics(statistics))
            } catch (e: Exception) {
                Timber.e(e, "Failed to load statistics")
            }
        }
    }

    private fun loadGroupedCallLogs() {
        viewModelScope.launch {
            try {
                val filterType = mapCallTypeFilter(_state.value.selectedCallType)
                val groupedLogs = callLogRepository.getGroupedCallLogs(
                    filterType = filterType,
                    dateRange = _state.value.dateRange
                )
                _state.update { it.copy(groupedCallLogs = groupedLogs) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load grouped logs")
            }
        }
    }

    private fun checkUnreadCount() {
        viewModelScope.launch {
            val unreadCount = callLogRepository.getUnreadCount()
            _state.update { it.copy(unreadCount = unreadCount) }
        }
    }

    private fun refresh() {
        // Just trigger a re-emission of current params to force a DB fetch
        _searchQuery.value = _state.value.searchQuery
        loadStatistics()
        checkUnreadCount()
    }

    // --- SELECTION & NAVIGATION STATE ---

    private fun toggleSelectionMode(enabled: Boolean) {
        _state.update {
            it.copy(
                isInSelectionMode = enabled,
                selectedCalls = if (!enabled) emptySet() else it.selectedCalls
            )
        }
    }

    private fun toggleCallSelection(callId: String) {
        _state.update { currentState ->
            val selectedCalls = currentState.selectedCalls.toMutableSet()
            if (selectedCalls.contains(callId)) {
                selectedCalls.remove(callId)
            } else {
                selectedCalls.add(callId)
            }
            currentState.copy(selectedCalls = selectedCalls)
        }
    }

    private fun selectCallLog(callLog: CallLogEntry) {
        if (!_state.value.isInSelectionMode) {
            sendSideEffect(CallLogSideEffect.NavigateToCall(callLog.number))
        } else {
            toggleCallSelection(callLog.id)
        }
    }

    private fun makeCall(phoneNumber: String) {
        sendSideEffect(CallLogSideEffect.NavigateToCall(phoneNumber))
    }

    private fun sendSms(phoneNumber: String) {
        sendSideEffect(CallLogSideEffect.NavigateToSms(phoneNumber))
    }

    private fun viewContact(phoneNumber: String) {
        sendSideEffect(CallLogSideEffect.NavigateToContact(phoneNumber))
    }

    private fun addToContacts(phoneNumber: String, name: String?) {
        sendSideEffect(CallLogSideEffect.NavigateToAddContact(phoneNumber, name))
    }

    private fun blockNumber(phoneNumber: String) {
        sendSideEffect(CallLogSideEffect.ShowBlockConfirmation(phoneNumber))
    }

    private fun exportCallLogs() {
        viewModelScope.launch {
            try {
                val filePath = callLogRepository.exportCallLogs()
                if (filePath != null) {
                    sendSideEffect(CallLogSideEffect.ExportCallLogs(filePath))
                } else {
                    sendSideEffect(CallLogSideEffect.ShowError("No call logs to export"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to export call logs")
                sendSideEffect(CallLogSideEffect.ShowError("Failed to export call logs"))
            }
        }
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun clearSuccessMessage() {
        _state.update { it.copy(successMessage = null) }
    }

    private fun sendSideEffect(effect: CallLogSideEffect) {
        viewModelScope.launch {
            _sideEffect.emit(effect)
        }
    }

    // --- UI FORMATTING HELPERS ---

    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, secs)
            minutes > 0 -> String.format("%02d:%02d", minutes, secs)
            else -> String.format("%02ds", secs)
        }
    }

    fun formatDate(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp

        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            isSameDay(calendar, today) -> "Today, ${formatTime(timestamp)}"
            isSameDay(calendar, yesterday) -> "Yesterday, ${formatTime(timestamp)}"
            else -> {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                "${dateFormat.format(Date(timestamp))}, ${formatTime(timestamp)}"
            }
        }
    }

    private fun formatTime(timestamp: Long): String {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return timeFormat.format(Date(timestamp))
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}

// ... Factory remains unchanged ...