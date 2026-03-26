package com.ghost.caller.repository.paging

import android.content.Context
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ghost.caller.models.ContactQuickInfo
import com.ghost.caller.models.ContactType
import com.ghost.caller.repository.ContactFilter
import com.ghost.caller.repository.ContactSort
import com.ghost.caller.repository.isValidPhone
import com.ghost.caller.repository.mapper.ContactMapper
import com.ghost.caller.repository.normalizeNumber

class RecentContactsPagingSource(
    private val context: Context,
    private val hasPermission: Boolean,
    private val sort: ContactSort = ContactSort.RECENT_DESC,
    private val filter: ContactFilter = ContactFilter.ALL
) : PagingSource<Int, ContactQuickInfo>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ContactQuickInfo> {

        if (!hasPermission) {
            return LoadResult.Page(emptyList(), null, null)
        }

        val page = params.key ?: 0
        val limit = params.loadSize
        val offset = page * limit

        return try {

            val uri = CallLog.Calls.CONTENT_URI

            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.DATE
            )

            val sortOrder = buildSortOrder(sort)

            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                sortOrder
            )

            val contacts = mutableListOf<ContactQuickInfo>()
            val seenNumbers = mutableSetOf<String>()

            cursor?.use {
                val numberIndex = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val nameIndex = it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)

                if (it.moveToPosition(offset)) {
                    var count = 0

                    do {
                        val rawNumber = it.getString(numberIndex)

                        if (!isValidPhone(rawNumber)) continue

                        val normalized = normalizeNumber(rawNumber!!)

                        if (!seenNumbers.add(normalized)) continue

                        val name = it.getString(nameIndex)
                            ?.takeIf { it.isNotBlank() }
                            ?: rawNumber

                        val isStarred = isStarredContact(normalized)

                        // 🔥 Apply filter
                        if (!applyFilter(filter, isStarred)) continue

                        contacts.add(
                            ContactQuickInfo(
                                id = normalized,
                                displayName = name,
                                primaryPhoneNumber = rawNumber,
                                primaryEmail = null,
                                photoUri = null,
                                contactType = ContactType.PHONE,
                                initials = ContactMapper.generateInitials(name),
                                isStarred = isStarred
                            )
                        )

                        count++
                    } while (it.moveToNext() && count < limit)
                }
            }

            val nextKey = if (contacts.size == limit) page + 1 else null

            LoadResult.Page(
                data = contacts,
                prevKey = if (page > 0) page - 1 else null,
                nextKey = nextKey
            )

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ContactQuickInfo>): Int? {
        return state.anchorPosition?.let { pos ->
            state.closestPageToPosition(pos)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(pos)?.nextKey?.minus(1)
        }
    }

    // 🔥 SORT
    private fun buildSortOrder(sort: ContactSort): String {
        return when (sort) {
            ContactSort.RECENT_DESC -> "${CallLog.Calls.DATE} DESC"
            ContactSort.RECENT_ASC -> "${CallLog.Calls.DATE} ASC"
            ContactSort.NAME_ASC -> "${CallLog.Calls.CACHED_NAME} COLLATE NOCASE ASC"
            ContactSort.NAME_DESC -> "${CallLog.Calls.CACHED_NAME} COLLATE NOCASE DESC"
        }
    }

    // 🔥 FILTER
    private fun applyFilter(filter: ContactFilter, isStarred: Boolean): Boolean {
        return when (filter) {
            ContactFilter.ALL -> true
            ContactFilter.STARRED -> isStarred
        }
    }

    // 🔥 Check starred from Contacts DB
    private fun isStarredContact(normalizedNumber: String): Boolean {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI

        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.STARRED)
        val selection = "${ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER} = ?"
        val selectionArgs = arrayOf(normalizedNumber)

        return try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                ?.use {
                    if (it.moveToFirst()) {
                        it.getInt(0) == 1
                    } else false
                } ?: false
        } catch (e: Exception) {
            false
        }
    }
}