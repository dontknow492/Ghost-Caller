@file:Suppress("D")

package com.ghost.caller.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.CallLog
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.ghost.caller.repository.paging.CallLogPagingSource
import com.ghost.caller.viewmodel.call.CallLogEntry
import com.ghost.caller.viewmodel.call.CallType
import com.ghost.caller.viewmodel.recent.CallStatistics
import com.ghost.caller.viewmodel.recent.DateRange
import com.ghost.caller.viewmodel.recent.GroupedCallLog
import com.ghost.caller.viewmodel.recent.SortOrder
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CallLogRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val phoneUtil: PhoneNumberUtil? = PhoneNumberUtil.getInstance()
    private val geocoder: PhoneNumberOfflineGeocoder? = PhoneNumberOfflineGeocoder.getInstance()
    private val userCountryCode = Locale.getDefault().country

    companion object {
        private const val PAGE_SIZE = 50
    }

    /**
     * Get recent calls using Paging 3.
     * Replaces the old getRecentCalls(page, pageSize...) list-based method.
     */
    fun getPagedRecentCalls(
        filterType: CallType? = null,
        dateRange: DateRange = DateRange.ALL,
        searchQuery: String? = null,
        sortOrder: SortOrder = SortOrder.DATE_DESC
    ): Flow<PagingData<CallLogEntry>> {
        Timber.d("Creating Pager for Recent Calls. Filter: $filterType, Search: $searchQuery")
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE * 2
            ),
            pagingSourceFactory = {
                CallLogPagingSource(
                    context = context,
                    repository = this,
                    filterType = filterType,
                    dateRange = dateRange,
                    searchQuery = searchQuery,
                    sortOrder = sortOrder
                )
            }
        ).flow
    }

    /**
     * Maps a single Cursor row to a CallLogEntry.
     * Extracted from your original logic for reuse in PagingSource.
     */
    fun mapCursorToCallEntry(cursor: Cursor): CallLogEntry? {
        try {
            val id =
                cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls._ID)) ?: return null
            val number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)) ?: ""

            // Resolve Location Logic
            var resolvedLocation: String? = null
            try {
                val parsedNumber = phoneUtil?.parse(number, userCountryCode)
                val geoDesc = if (parsedNumber != null) {
                    geocoder?.getDescriptionForNumber(parsedNumber, Locale.getDefault())
                } else ""

                if (!geoDesc.isNullOrBlank()) {
                    resolvedLocation = geoDesc
                } else {
                    val regionCode = phoneUtil?.getRegionCodeForNumber(parsedNumber)
                    if (regionCode != null) {
                        resolvedLocation = Locale.Builder()
                            .setRegion(regionCode)
                            .build()
                            .displayCountry
                    }
                }
            } catch (_: Exception) {
                val locIdx = cursor.getColumnIndex(CallLog.Calls.GEOCODED_LOCATION)
                resolvedLocation = if (locIdx >= 0) cursor.getString(locIdx) else null
            }

            val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
            val type = if (typeIdx >= 0) {
                when (cursor.getInt(typeIdx)) {
                    CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                    CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                    CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                    CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
                    CallLog.Calls.BLOCKED_TYPE -> CallType.BLOCKED
                    CallLog.Calls.VOICEMAIL_TYPE -> CallType.VOICEMAIL
                    else -> CallType.UNKNOWN
                }
            } else CallType.UNKNOWN

            val featuresIdx = cursor.getColumnIndex(CallLog.Calls.FEATURES)
            val features = if (featuresIdx >= 0) cursor.getInt(featuresIdx) else 0
            val isVideo =
                (features and CallLog.Calls.FEATURES_VIDEO) == CallLog.Calls.FEATURES_VIDEO

            return CallLogEntry(
                id = id,
                number = number,
                // getColumnIndexOrThrow ensures the index is valid or throws an exception
                name = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)),
                timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)),
                type = type,
                durationSeconds = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)),
                isRead = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.IS_READ)) == 1,
                location = resolvedLocation,
                isVideoCall = isVideo,
                groupedCount = 1
            )
        } catch (e: Exception) {
            Timber.e(e, "Error mapping cursor to CallLogEntry")
            return null
        }
    }

    // --- SELECTION BUILDERS (Kept from original) ---

    fun buildSelection(
        filterType: CallType?,
        dateRange: DateRange,
        searchQuery: String?
    ): String? {
        val conditions = mutableListOf<String>()

        filterType?.let { type ->
            val typeValue = when (type) {
                CallType.INCOMING -> CallLog.Calls.INCOMING_TYPE
                CallType.OUTGOING -> CallLog.Calls.OUTGOING_TYPE
                CallType.MISSED -> CallLog.Calls.MISSED_TYPE
                CallType.REJECTED -> CallLog.Calls.REJECTED_TYPE
                CallType.BLOCKED -> CallLog.Calls.BLOCKED_TYPE
                CallType.VOICEMAIL -> CallLog.Calls.VOICEMAIL_TYPE
                else -> null
            }
            typeValue?.let { conditions.add("${CallLog.Calls.TYPE} = $it") }
        }

        when (dateRange) {
            DateRange.TODAY -> conditions.add("${CallLog.Calls.DATE} >= ${getDayStart(0)}")
            DateRange.WEEK -> conditions.add("${CallLog.Calls.DATE} >= ${getDayStart(-7)}")
            DateRange.MONTH -> conditions.add("${CallLog.Calls.DATE} >= ${getMonthStart()}")
            DateRange.YEAR -> conditions.add("${CallLog.Calls.DATE} >= ${getYearStart()}")
            else -> {}
        }

        searchQuery?.takeIf { it.isNotBlank() }?.let { query ->
            conditions.add("(${CallLog.Calls.NUMBER} LIKE '%$query%' OR ${CallLog.Calls.CACHED_NAME} LIKE '%$query%')")
        }

        return if (conditions.isEmpty()) null else conditions.joinToString(" AND ")
    }

    // --- MODIFICATION METHODS (Kept from original) ---

    suspend fun deleteCallLog(call: CallLogEntry): Boolean = withContext(ioDispatcher) {
        try {
            val deletedRows = context.contentResolver.delete(
                CallLog.Calls.CONTENT_URI, "${CallLog.Calls._ID} = ?", arrayOf(call.id)
            )
            return@withContext deletedRows > 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete call log")
            return@withContext false
        }
    }

    suspend fun markAllAsRead(): Int = withContext(ioDispatcher) {
        try {
            val values = ContentValues().apply { put(CallLog.Calls.IS_READ, 1) }
            context.contentResolver.update(CallLog.Calls.CONTENT_URI, values, null, null)
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark all as read")
            0
        }
    }


    /**
     * Mark call log as read
     */
    suspend fun markAsRead(call: CallLogEntry): Boolean = withContext(ioDispatcher) {
        try {
            val values = ContentValues().apply {
                put(CallLog.Calls.IS_READ, 1)
            }

            val selection = "${CallLog.Calls._ID} = ?"
            val selectionArgs = arrayOf(call.id)

            val updatedRows = context.contentResolver.update(
                CallLog.Calls.CONTENT_URI,
                values,
                selection,
                selectionArgs
            )
            return@withContext updatedRows > 0
        } catch (e: Exception) {
            Timber.tag("CallLogRepository").e(e, "Failed to mark as read")
            return@withContext false
        }
    }


    /**
     * Delete multiple call logs
     */
    suspend fun deleteCallLogs(calls: List<CallLogEntry>): Int = withContext(ioDispatcher) {
        var deletedCount = 0
        calls.forEach { call ->
            if (deleteCallLog(call)) deletedCount++
        }
        return@withContext deletedCount
    }

    /**
     * Clear all call logs
     */
    suspend fun clearAllCallLogs(): Int = withContext(ioDispatcher) {
        try {
            val deletedRows = context.contentResolver.delete(
                CallLog.Calls.CONTENT_URI,
                null,
                null
            )
            return@withContext deletedRows
        } catch (e: Exception) {
            Log.e("CallLogRepository", "Failed to clear call logs", e)
            return@withContext 0
        }
    }

    /**
     * Get grouped call logs by date
     */


    /**
     * Get unread missed call count
     */
    suspend fun getUnreadCount(): Int = withContext(ioDispatcher) {
        val projection = arrayOf(CallLog.Calls._ID)
        val selection = "${CallLog.Calls.IS_READ} = ? AND ${CallLog.Calls.TYPE} = ?"
        val selectionArgs = arrayOf("0", CallLog.Calls.MISSED_TYPE.toString())

        try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                return@withContext cursor.count
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get unread count")
        }

        return@withContext 0
    }


    /**
     * Get call statistics (Scans history to build analytics)
     */
    suspend fun getCallStatistics(): CallStatistics = withContext(ioDispatcher) {
        val projection = arrayOf(
            CallLog.Calls.TYPE,
            CallLog.Calls.DURATION,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.DATE
        )

        val callCounts = mutableMapOf<CallType, Int>().withDefault { 0 }
        var totalDuration = 0L
        val contactCallCount = mutableMapOf<String, Int>()
        val callCountByDay = mutableMapOf<String, Int>()
        val callDurationByDay = mutableMapOf<String, Long>()

        try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val typeIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val durationIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                val numberIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val nameIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                val dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)

                while (cursor.moveToNext()) {
                    val type = when (cursor.getInt(typeIdx)) {
                        CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                        CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
                        CallLog.Calls.BLOCKED_TYPE -> CallType.BLOCKED
                        CallLog.Calls.VOICEMAIL_TYPE -> CallType.VOICEMAIL
                        else -> CallType.UNKNOWN
                    }

                    callCounts[type] = callCounts.getValue(type) + 1

                    val duration = cursor.getLong(durationIdx)
                    totalDuration += duration

                    val number = cursor.getString(numberIdx)
                    if (number != null) {
                        val name = cursor.getString(nameIdx)
                        val contactKey = name ?: number
                        contactCallCount[contactKey] =
                            contactCallCount.getOrDefault(contactKey, 0) + 1
                    }

                    // Group by day for charts/graphs
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = cursor.getLong(dateIdx)
                    val dateStr =
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

                    callCountByDay[dateStr] = callCountByDay.getOrDefault(dateStr, 0) + 1
                    callDurationByDay[dateStr] =
                        callDurationByDay.getOrDefault(dateStr, 0) + duration
                }
            }

            val mostActive = contactCallCount.maxByOrNull { it.value }

            return@withContext CallStatistics(
                totalCalls = callCounts.values.sum(),
                totalDuration = totalDuration,
                incomingCount = callCounts[CallType.INCOMING] ?: 0,
                outgoingCount = callCounts[CallType.OUTGOING] ?: 0,
                missedCount = callCounts[CallType.MISSED] ?: 0,
                rejectedCount = callCounts[CallType.REJECTED] ?: 0,
                blockedCount = callCounts[CallType.BLOCKED] ?: 0,
                voicemailCount = callCounts[CallType.VOICEMAIL] ?: 0,
                averageDuration = if (callCounts.values.sum() > 0) totalDuration / callCounts.values.sum() else 0,
                mostActiveContact = mostActive?.key,
                mostActiveNumber = mostActive?.key,
                callCountByDay = callCountByDay,
                callDurationByDay = callDurationByDay
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get statistics")
            return@withContext CallStatistics()
        }
    }


    /**
     * Get grouped call logs by date (Not paginated, loads top 500 for the grouped view)
     */
    suspend fun getGroupedCallLogs(
        filterType: CallType? = null,
        dateRange: DateRange = DateRange.ALL
    ): List<GroupedCallLog> = withContext(ioDispatcher) {
        val groupedCalls = mutableMapOf<String, MutableList<CallLogEntry>>()
        val result = mutableListOf<GroupedCallLog>()

        // 1. Query the DB directly (Max 500 items for memory safety in grouped view)
        val calls = mutableListOf<CallLogEntry>()
        val selection = buildSelection(filterType, dateRange, null)
        val sortOrderStr = "${CallLog.Calls.DATE} DESC LIMIT 500"

        try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null, // Get all columns so our mapper works
                selection,
                null,
                sortOrderStr
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    mapCursorToCallEntry(cursor)?.let { calls.add(it) }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching calls for grouping")
            return@withContext emptyList()
        }

        // 2. Group the results in memory
        val dateFormat = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
        val today = Calendar.getInstance()

        calls.forEach { call ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = call.timestamp

            val dateKey = when {
                isToday(calendar, today) -> "Today"
                isYesterday(calendar, today) -> "Yesterday"
                else -> dateFormat.format(calendar.time)
            }

            groupedCalls.getOrPut(dateKey) { mutableListOf() }.add(call)
        }

        // 3. Map to UI models
        groupedCalls.forEach { (date, callsList) ->
            result.add(
                GroupedCallLog(
                    date = date,
                    calls = callsList,
                    totalCount = callsList.size,
                    totalDuration = callsList.sumOf { it.durationSeconds }
                )
            )
        }

        // Sort the groups so the newest dates appear first
        return@withContext result.sortedByDescending { it.calls.firstOrNull()?.timestamp ?: 0L }
    }

    // Keep your existing date helpers for grouping:
    private fun isToday(calendar: Calendar, today: Calendar): Boolean {
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(calendar: Calendar, today: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            time = today.time
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
    }


    /**
     * Export all call logs to a CSV file (Bypasses Paging for raw data extraction)
     */
    suspend fun exportCallLogs(): String? = withContext(ioDispatcher) {
        try {
            val fileName = "call_logs_${System.currentTimeMillis()}.csv"
            val file = File(context.filesDir, fileName)

            // Query everything, ordered by date
            val sortOrder = "${CallLog.Calls.DATE} DESC"

            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null, // All columns
                null, // No selection (get everything)
                null,
                sortOrder
            )?.use { cursor ->

                // If there's no data, return early
                if (cursor.count == 0) return@withContext null

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                FileOutputStream(file).bufferedWriter().use { writer ->
                    // Write CSV Header
                    writer.write("ID,Number,Name,Date,Type,Duration,Location,IsVideo\n")

                    // Stream rows directly to the file to save memory
                    while (cursor.moveToNext()) {
                        val entry = mapCursorToCallEntry(cursor) ?: continue

                        val line = buildString {
                            append(entry.id).append(",")
                            append(escapeCsv(entry.number)).append(",")
                            append(escapeCsv(entry.name ?: "")).append(",")
                            append(dateFormat.format(Date(entry.timestamp))).append(",")
                            append(entry.type.name).append(",")
                            append(formatDuration(entry.durationSeconds)).append(",")
                            append(escapeCsv(entry.location ?: "")).append(",")
                            append(entry.isVideoCall).append("\n")
                        }
                        writer.write(line)
                    }
                }
            }

            Timber.d("Exported call logs successfully to ${file.absolutePath}")
            return@withContext file.absolutePath

        } catch (e: Exception) {
            Timber.e(e, "Failed to export call logs")
            return@withContext null
        }
    }

    // Keep your existing CSV formatting helpers:
    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
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


    // --- HELPER TIME METHODS ---

    private fun getDayStart(daysOffset: Int): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysOffset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getMonthStart(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getYearStart(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }


}



