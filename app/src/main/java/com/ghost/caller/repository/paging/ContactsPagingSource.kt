package com.ghost.caller.repository.paging

import android.content.Context
import android.net.Uri
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

class ContactsPagingSource(
    private val context: Context,
    private val sortBy: ContactSort,
    private val filter: ContactFilter,
    private val hasPermission: Boolean
) : PagingSource<Int, ContactQuickInfo>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ContactQuickInfo> {

        if (!hasPermission) {
            return LoadResult.Page(emptyList(), null, null)
        }

        val page = params.key ?: 0
        val limit = params.loadSize
        val offset = page * limit

        return try {

            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI

            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                ContactsContract.CommonDataKinds.Phone.STARRED,
                ContactsContract.Contacts.LAST_TIME_CONTACTED
            )

            val selection = buildSelection(filter)

            val sortOrder = buildSortOrder(sortBy)

            val cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                null,
                "$sortOrder LIMIT $limit OFFSET $offset"
            )

            val contacts = mutableListOf<ContactQuickInfo>()
            val seenNumbers = mutableSetOf<String>() // 🔥 dedupe

            cursor?.use {
                val idIndex =
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex =
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex =
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex =
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val starredIndex =
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.STARRED)

                if (it.moveToPosition(offset)) {
                    var count = 0

                    do {
                        val id = it.getString(idIndex) ?: continue
                        val name = it.getString(nameIndex)?.takeIf { it.isNotBlank() } ?: "Unknown"
                        val rawNumber = it.getString(numberIndex)

                        // ✅ Validate
                        if (!isValidPhone(rawNumber)) continue

                        val normalized = normalizeNumber(rawNumber!!)

                        // ✅ Deduplicate
                        if (!seenNumbers.add(normalized)) continue

                        val photoUri = it.getString(photoIndex)?.let(Uri::parse)
                        val starred = it.getInt(starredIndex) == 1

                        contacts.add(
                            ContactQuickInfo(
                                id = "$id-$normalized", // 🔥 stable unique key
                                displayName = name,
                                primaryPhoneNumber = rawNumber,
                                primaryEmail = null,
                                photoUri = photoUri,
                                contactType = ContactType.PHONE,
                                initials = ContactMapper.generateInitials(name),
                                isStarred = starred
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

    // ---------------------------
    // 🔥 Filter
    // ---------------------------

    private fun buildSelection(filter: ContactFilter): String {
        return when (filter) {
            ContactFilter.STARRED -> """
                ${ContactsContract.CommonDataKinds.Phone.STARRED} = 1 AND
                ${ContactsContract.CommonDataKinds.Phone.NUMBER} IS NOT NULL AND
                ${ContactsContract.CommonDataKinds.Phone.NUMBER} != ''
            """.trimIndent()

            else -> """
                ${ContactsContract.CommonDataKinds.Phone.NUMBER} IS NOT NULL AND
                ${ContactsContract.CommonDataKinds.Phone.NUMBER} != ''
            """.trimIndent()
        }
    }

    // ---------------------------
    // 🔥 Sort
    // ---------------------------

    private fun buildSortOrder(sortBy: ContactSort): String {
        return when (sortBy) {
            ContactSort.NAME_ASC ->
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"

            ContactSort.NAME_DESC ->
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE DESC"

            // ❌ REMOVE deprecated usage
            ContactSort.RECENT_ASC ->
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"

            ContactSort.RECENT_DESC ->
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE DESC"
        }
    }

}