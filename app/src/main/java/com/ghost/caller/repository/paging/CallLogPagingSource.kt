package com.ghost.caller.repository.paging

import android.content.Context
import android.provider.CallLog
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ghost.caller.repository.CallLogRepository
import com.ghost.caller.viewmodel.call.CallLogEntry
import com.ghost.caller.viewmodel.call.CallType
import com.ghost.caller.viewmodel.recent.DateRange
import com.ghost.caller.viewmodel.recent.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class CallLogPagingSource(
    private val context: Context,
    private val repository: CallLogRepository,
    private val filterType: CallType?,
    private val dateRange: DateRange,
    private val searchQuery: String?,
    private val sortOrder: SortOrder
) : PagingSource<Int, CallLogEntry>() {

    override fun getRefreshKey(state: PagingState<Int, CallLogEntry>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(state.config.pageSize)
                ?: anchorPage?.nextKey?.minus(state.config.pageSize)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CallLogEntry> =
        withContext(Dispatchers.IO) {
            val offset = params.key ?: 0
            val limit = params.loadSize

            try {
                val calls = mutableListOf<CallLogEntry>()

                val selection = repository.buildSelection(filterType, dateRange, searchQuery)

                val sortOrderStr = when (sortOrder) {
                    SortOrder.DATE_DESC -> "${CallLog.Calls.DATE} DESC"
                    SortOrder.DATE_ASC -> "${CallLog.Calls.DATE} ASC"
                    SortOrder.DURATION_DESC -> "${CallLog.Calls.DURATION} DESC, ${CallLog.Calls.DATE} DESC"
                    SortOrder.DURATION_ASC -> "${CallLog.Calls.DURATION} ASC, ${CallLog.Calls.DATE} DESC"
                } + " LIMIT $limit OFFSET $offset"

                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    null, // Getting all columns to support repository mapping
                    selection,
                    null,
                    sortOrderStr
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        repository.mapCursorToCallEntry(cursor)?.let { calls.add(it) }
                    }
                }

                Timber.v("Loaded ${calls.size} call logs for offset $offset")

                LoadResult.Page(
                    data = calls,
                    prevKey = if (offset == 0) null else maxOf(0, offset - limit),
                    nextKey = if (calls.isEmpty() || calls.size < limit) null else offset + limit
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading paged call logs")
                LoadResult.Error(e)
            }
        }
}