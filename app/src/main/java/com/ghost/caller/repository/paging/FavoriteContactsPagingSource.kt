package com.ghost.caller.repository.paging

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ghost.caller.models.ContactQuickInfo
import com.ghost.caller.models.ContactType
import com.ghost.caller.repository.ContactSort
import com.ghost.caller.repository.isValidPhone
import com.ghost.caller.repository.mapper.ContactMapper
import com.ghost.caller.repository.normalizeNumber

class FavoriteContactsPagingSource(
    private val context: Context,
    private val hasPermission: Boolean,
    private val sort: ContactSort
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
                ContactsContract.CommonDataKinds.Phone.STARRED
            )

            val selection = """
                ${ContactsContract.CommonDataKinds.Phone.STARRED} = 1 AND
                ${ContactsContract.CommonDataKinds.Phone.NUMBER} IS NOT NULL AND
                ${ContactsContract.CommonDataKinds.Phone.NUMBER} != ''
            """.trimIndent()

            val sortOrder = buildSortOrder(sort)

            val cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                null,
                sortOrder
            )

            val contacts = mutableListOf<ContactQuickInfo>()
            val seenNumbers = mutableSetOf<String>()

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

                        if (!isValidPhone(rawNumber)) continue

                        val normalized = normalizeNumber(rawNumber!!)

                        if (!seenNumbers.add(normalized)) continue

                        val photoUri = it.getString(photoIndex)?.let(Uri::parse)
                        val starred = it.getInt(starredIndex) == 1

                        contacts.add(
                            ContactQuickInfo(
                                id = "$id-$normalized",
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

    // 🔥 SORT LOGIC
    private fun buildSortOrder(sort: ContactSort): String {
        return when (sort) {

            ContactSort.NAME_ASC ->
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"

            ContactSort.NAME_DESC ->
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE DESC"

            // ⚠️ "Recent" not valid here → fallback
            ContactSort.RECENT_DESC ->
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"

            ContactSort.RECENT_ASC ->
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
        }
    }
}