package com.ghost.caller.repository.mapper

import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import com.ghost.caller.models.ContactQuickInfo
import com.ghost.caller.models.ContactType

object ContactMapper {

    fun mapCursorToContacts(cursor: Cursor): List<ContactQuickInfo> {
        val contacts = mutableListOf<ContactQuickInfo>()
        val seen = mutableSetOf<String>()

        val idIndex = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
        val nameIndex = cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME)
        val dataIndex = cursor.getColumnIndex(ContactsContract.Data.DATA1)
        val mimeIndex = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
        val photoIndex = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
        val starredIndex = cursor.getColumnIndex(ContactsContract.Contacts.STARRED)

        while (cursor.moveToNext()) {
            val id = cursor.getString(idIndex) ?: continue

            // 🔥 dedupe
            if (!seen.add(id)) continue

            val name = cursor.getString(nameIndex) ?: "Unknown"
            val data = cursor.getString(dataIndex)
            val mime = cursor.getString(mimeIndex)

            val phone =
                if (mime == ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE) data else null
            val email =
                if (mime == ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE) data else null

            val photoUri = cursor.getString(photoIndex)?.let(Uri::parse)
            val starred = starredIndex >= 0 && cursor.getInt(starredIndex) == 1

            contacts.add(
                ContactQuickInfo(
                    id = id,
                    displayName = name,
                    primaryPhoneNumber = phone,
                    primaryEmail = email,
                    photoUri = photoUri,
                    contactType = ContactType.PHONE,
                    initials = generateInitials(name),
                    isStarred = starred
                )
            )
        }

        return contacts
    }

    fun generateInitials(name: String): String {
        val parts = name.trim().split("\\s+".toRegex())
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(1).uppercase()
            else -> "${parts[0][0]}${parts[1][0]}".uppercase()
        }
    }
}