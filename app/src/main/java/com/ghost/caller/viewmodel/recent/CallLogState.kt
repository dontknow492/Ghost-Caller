package com.ghost.caller.viewmodel.recent

// CallLogMVI.kt
import com.ghost.caller.viewmodel.call.CallLogEntry

/**
 * Call Log UI State
 */

/**
 * Call Log UI State
 */
data class CallLogState(
    // Removed all manual list, page, and count tracking variables.
    val isLoading: Boolean = false, // Still useful for initial global loading if desired
    val searchQuery: String = "",
    val selectedCallType: CallTypeFilter = CallTypeFilter.ALL,
    val dateRange: DateRange = DateRange.ALL,
    val isInSelectionMode: Boolean = false,
    val selectedCalls: Set<String> = emptySet(),
    val error: String? = null,
    val successMessage: String? = null,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val viewMode: CallLogViewMode = CallLogViewMode.LIST,
    val statistics: CallStatistics = CallStatistics(),
    val groupedCallLogs: List<GroupedCallLog> = emptyList(), // Can remain static if loaded rarely
    val unreadCount: Int = 0
)

// ... Enums and other data classes remain the same ...

enum class CallTypeFilter {
    ALL, INCOMING, OUTGOING, MISSED, REJECTED, BLOCKED, VOICEMAIL
}

enum class DateRange {
    ALL, TODAY, WEEK, MONTH, YEAR, CUSTOM
}

enum class SortOrder {
    DATE_DESC, DATE_ASC, DURATION_DESC, DURATION_ASC
}

enum class CallLogViewMode {
    LIST, GROUPED, STATISTICS
}

data class CallStatistics(
    val totalCalls: Int = 0,
    val totalDuration: Long = 0,
    val incomingCount: Int = 0,
    val outgoingCount: Int = 0,
    val missedCount: Int = 0,
    val rejectedCount: Int = 0,
    val blockedCount: Int = 0,
    val voicemailCount: Int = 0,
    val averageDuration: Long = 0,
    val mostActiveContact: String? = null,
    val mostActiveNumber: String? = null,
    val callCountByDay: Map<String, Int> = emptyMap(),
    val callDurationByDay: Map<String, Long> = emptyMap()
)

data class GroupedCallLog(
    val date: String,
    val calls: List<CallLogEntry>,
    val totalCount: Int,
    val totalDuration: Long
)

/**
 * Call Log UI Events
 */
sealed class CallLogEvent {
    data class LoadCallLogs(val forceRefresh: Boolean = false, val page: Int = 0) : CallLogEvent()
    data object LoadMoreCallLogs : CallLogEvent()
    data class SearchCallLogs(val query: String) : CallLogEvent()
    data class FilterByType(val type: CallTypeFilter) : CallLogEvent()
    data class FilterByDate(val dateRange: DateRange) : CallLogEvent()
    data class SelectCallLog(val callLog: CallLogEntry) : CallLogEvent()
    data class DeleteCallLog(val callLog: CallLogEntry) : CallLogEvent()
    data class DeleteCallLogs(val callLogs: List<CallLogEntry>) : CallLogEvent()
    data object ClearCallLogs : CallLogEvent()
    data class ToggleSelectionMode(val enabled: Boolean) : CallLogEvent()
    data class ToggleCallSelection(val callId: String) : CallLogEvent()
    data object ClearSearch : CallLogEvent()
    data object ClearError : CallLogEvent()
    data object ClearSuccessMessage : CallLogEvent()
    data class ChangeSortOrder(val sortOrder: SortOrder) : CallLogEvent()
    data class ChangeViewMode(val viewMode: CallLogViewMode) : CallLogEvent()
    data class MakeCall(val phoneNumber: String) : CallLogEvent()
    data class SendSms(val phoneNumber: String) : CallLogEvent()
    data class ViewContact(val phoneNumber: String) : CallLogEvent()
    data class AddToContacts(val phoneNumber: String, val name: String?) : CallLogEvent()
    data class BlockNumber(val phoneNumber: String) : CallLogEvent()
    data class MarkAsRead(val callLog: CallLogEntry) : CallLogEvent()
    data object MarkAllAsRead : CallLogEvent()
    data object ExportCallLogs : CallLogEvent()
    object Refresh : CallLogEvent()
    object Retry : CallLogEvent()
}

/**
 * Call Log Side Effects
 */
sealed class CallLogSideEffect {
    data class ShowToast(val message: String) : CallLogSideEffect()
    data class ShowError(val message: String) : CallLogSideEffect()
    data class NavigateToCall(val phoneNumber: String) : CallLogSideEffect()
    data class NavigateToSms(val phoneNumber: String) : CallLogSideEffect()
    data class NavigateToContact(val phoneNumber: String) : CallLogSideEffect()
    data class NavigateToAddContact(val phoneNumber: String, val name: String?) :
        CallLogSideEffect()

    data class ShowBlockConfirmation(val phoneNumber: String) : CallLogSideEffect()
    data class ShowDeleteConfirmation(val callLogs: List<CallLogEntry>) : CallLogSideEffect()
    data object ShowClearAllConfirmation : CallLogSideEffect()
    data class ExportCallLogs(val filePath: String) : CallLogSideEffect()
    data class CallLogDeleted(val callLog: CallLogEntry) : CallLogSideEffect()
    data class CallLogsDeleted(val count: Int) : CallLogSideEffect()
    data class CallLogsCleared(val count: Int) : CallLogSideEffect()
    data class BlockNumber(val phoneNumber: String) : CallLogSideEffect()
    data class ShowStatistics(val statistics: CallStatistics) : CallLogSideEffect()
    object NavigateBack : CallLogSideEffect()
    object ShowShareDialog : CallLogSideEffect()
}