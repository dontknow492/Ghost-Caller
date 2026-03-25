package com.ghost.caller.repository

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ghost.caller.models.ContactQuickInfo
import com.ghost.caller.models.ContactType

class FavoriteContactsPagingSource(
    private val context: Context,
    private val hasPermission: Boolean
) : PagingSource<Int, ContactQuickInfo>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ContactQuickInfo> {
        if (!hasPermission) {
            return LoadResult.Error(SecurityException("Contacts permission not granted"))
        }

        val currentPage = params.key ?: 0
        val offset = currentPage * params.loadSize

        return try {
            val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_URI,
                ContactsContract.Contacts.STARRED
            )

            val selection = "${ContactsContract.Contacts.STARRED} = 1"
            val sortOrder = "${ContactsContract.Contacts.DISPLAY_NAME} COLLATE LOCALIZED ASC"

            val cursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                selection,
                null,
                "$sortOrder LIMIT ${params.loadSize} OFFSET $offset"
            )

            val contacts = mutableListOf<ContactQuickInfo>()
            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val photoIndex = it.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)

                while (it.moveToNext()) {
                    val id = it.getString(idIndex)
                    val displayName = it.getString(nameIndex) ?: "Unknown"
                    val photoUri = if (photoIndex >= 0) {
                        it.getString(photoIndex)?.let { Uri.parse(it) }
                    } else null

                    contacts.add(
                        ContactQuickInfo(
                            id = id,
                            displayName = displayName,
                            primaryPhoneNumber = null,
                            primaryEmail = null,
                            photoUri = photoUri,
                            contactType = ContactType.PHONE,
                            initials = generateInitials(displayName),
                            isStarred = true
                        )
                    )
                }
            }

            val nextKey = if (contacts.size == params.loadSize) currentPage + 1 else null
            LoadResult.Page(
                contacts,
                prevKey = if (currentPage > 0) currentPage - 1 else null,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ContactQuickInfo>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    private fun generateInitials(displayName: String): String {
        val parts = displayName.trim().split("\\s+".toRegex())
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(1).uppercase()
            else -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
        }
    }
}