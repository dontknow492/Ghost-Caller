// ContactRepository.kt
package com.ghost.caller.repository

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds
import androidx.core.content.ContextCompat
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.ghost.caller.models.AddressData
import com.ghost.caller.models.AddressType
import com.ghost.caller.models.Contact
import com.ghost.caller.models.ContactEvent
import com.ghost.caller.models.ContactGroup
import com.ghost.caller.models.ContactQuickInfo
import com.ghost.caller.models.ContactType
import com.ghost.caller.models.ContactWithDetails
import com.ghost.caller.models.EmailAddress
import com.ghost.caller.models.EmailData
import com.ghost.caller.models.EmailType
import com.ghost.caller.models.EventType
import com.ghost.caller.models.Organization
import com.ghost.caller.models.OrganizationData
import com.ghost.caller.models.PhoneNumber
import com.ghost.caller.models.PhoneNumberData
import com.ghost.caller.models.PhoneNumberType
import com.ghost.caller.models.PostalAddress
import com.ghost.caller.repository.paging.ContactsPagingSource
import com.ghost.caller.repository.paging.FavoriteContactsPagingSource
import com.ghost.caller.repository.paging.RecentContactsPagingSource
import com.ghost.caller.repository.paging.SearchContactsPagingSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.Collator
import java.util.Calendar
import java.util.Locale

class ContactRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val TAG = "ContactRepository"
        private const val PAGE_SIZE = 50
    }

    /**
     * Check if contacts permission is granted
     */
    fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasWriteContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get paginated contacts flow
     */
    fun getContactsPaged(
        sortBy: ContactSort = ContactSort.NAME_ASC,
        filter: ContactFilter = ContactFilter.ALL
    ): Flow<PagingData<ContactQuickInfo>> {
        return Pager(
            config = PagingConfig(
                prefetchDistance = 10,
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                maxSize = PAGE_SIZE * 3
            ),
            pagingSourceFactory = {
                ContactsPagingSource(context, sortBy, filter, hasContactsPermission())
            }
        ).flow
    }

    /**
     * Get favorite contacts with pagination
     */
    fun getFavoriteContactsPaged(
        sortBy: ContactSort,
    ): Flow<PagingData<ContactQuickInfo>> {
        return Pager(
            config = PagingConfig(
                prefetchDistance = 10,
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                maxSize = PAGE_SIZE * 3
            ),
            pagingSourceFactory = {
                FavoriteContactsPagingSource(context, hasContactsPermission(), sortBy)
            }
        ).flow
    }

    /**
     * Get recent contacts with pagination
     */
    fun getRecentContactsPaged(
        sortBy: ContactSort = ContactSort.RECENT_ASC,
        filter: ContactFilter = ContactFilter.ALL
    ): Flow<PagingData<ContactQuickInfo>> {
        return Pager(
            config = PagingConfig(
                prefetchDistance = 10,
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                maxSize = PAGE_SIZE * 3
            ),
            pagingSourceFactory = {
                RecentContactsPagingSource(
                    context,
                    hasContactsPermission(),
                    sortBy,
                    filter
                )
            }
        ).flow
    }

    /**
     * Search contacts with pagination
     */
    fun searchContactsPaged(
        query: String,
        sortBy: ContactSort,
        filter: ContactFilter,
    ): Flow<PagingData<ContactQuickInfo>> {
        return Pager(
            config = PagingConfig(
                prefetchDistance = 10,
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                maxSize = PAGE_SIZE * 3
            ),
            pagingSourceFactory = {
                SearchContactsPagingSource(
                    context = context,
                    query = query.trim(),
                    hasPermission = hasContactsPermission(),
                    sortBy = sortBy,
                    filter = filter
                )
            }
        ).flow // 🔥 important
    }

    /**
     * Get contacts grouped by first letter (for alphabetical scrolling)
     * This still returns all contacts but with efficient cursor usage
     */
    suspend fun getContactsGrouped(): Map<String, List<ContactQuickInfo>> =
        withContext(ioDispatcher) {
            if (!hasContactsPermission()) return@withContext emptyMap()

            val groupedContacts = mutableMapOf<String, MutableList<ContactQuickInfo>>()
            Collator.getInstance(Locale.getDefault())

            try {
                val projection = arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.PHOTO_URI,
                    ContactsContract.Contacts.STARRED
                )

                val sortOrder = "${ContactsContract.Contacts.DISPLAY_NAME} COLLATE LOCALIZED ASC"

                context.contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val photoIndex = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
                    val starredIndex = cursor.getColumnIndex(ContactsContract.Contacts.STARRED)

                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idIndex)
                        val displayName = cursor.getString(nameIndex) ?: "Unknown"
                        val photoUri = if (photoIndex >= 0) {
                            cursor.getString(photoIndex)?.let { Uri.parse(it) }
                        } else null
                        val starred = starredIndex >= 0 && cursor.getInt(starredIndex) == 1

                        val primaryPhone = getPrimaryPhoneNumber(id)
                        val contactType = determineContactType(id)
                        val initials = generateInitials(displayName)

                        val contact = ContactQuickInfo(
                            id = id,
                            displayName = displayName,
                            primaryPhoneNumber = primaryPhone,
                            primaryEmail = getPrimaryEmail(id),
                            photoUri = photoUri,
                            contactType = contactType,
                            initials = initials,
                            isStarred = starred
                        )

                        val firstChar = displayName.firstOrNull()?.uppercase() ?: "#"
                        val key = if (firstChar.first().isLetter()) firstChar else "#"
                        groupedContacts.getOrPut(key) { mutableListOf() }.add(contact)
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error getting grouped contacts")
            }

            return@withContext groupedContacts.mapValues { (_, list) ->
                list.sortedBy { it.displayName.lowercase() }
            }.toSortedMap()
        }

    /**
     * Get contact details by ID or Phone Number
     */
    suspend fun getContactDetails(identifier: String): ContactWithDetails? =
        withContext(ioDispatcher) {
            if (!hasContactsPermission()) return@withContext null

            // 🔥 FIX: Clean identifier just in case it's a composite key (e.g., "3494-9118001025963")
            val cleanIdentifier = if (identifier.contains("-")) {
                val parts = identifier.split("-")
                if (parts[0].all { it.isDigit() } && parts.size == 2) parts[0] else identifier
            } else identifier

            // 1. Try treating it as a direct Contact ID
            var contact = getContactById(cleanIdentifier)
            var finalContactId = cleanIdentifier

            // 2. If that fails (it wasn't an ID), try treating it as a Phone Number
            if (contact == null && cleanIdentifier.any { it.isDigit() }) {
                Timber.tag(TAG).d("Not found as ID, treating as phone number: $cleanIdentifier")
                val resolvedId = getContactIdFromPhoneNumber(cleanIdentifier)
                if (resolvedId != null) {
                    finalContactId = resolvedId
                    contact = getContactById(resolvedId)
                }
            }

            // 3. If both failed, the contact doesn't exist
            if (contact == null) {
                Timber.tag(TAG).w("Failed to load contact info for: $cleanIdentifier")
                return@withContext null
            }

            return@withContext ContactWithDetails(
                contact = contact,
                organizations = getOrganizations(finalContactId),
                addresses = getAddresses(finalContactId),
                websites = getWebsites(finalContactId),
                notes = getNotes(finalContactId),
                events = getEvents(finalContactId)
            )
        }

    /**
     * Get primary phone number (cached version)
     */
    private val primaryPhoneCache = mutableMapOf<String, String?>()

    fun getPrimaryPhoneNumber(contactId: String): String? {
        return primaryPhoneCache.getOrPut(contactId) {
            val phoneNumbers = getPhoneNumbers(contactId)
            phoneNumbers.find { it.isPrimary }?.number ?: phoneNumbers.firstOrNull()?.number
        }
    }

    /**
     * Get primary email (cached version)
     */
    private val primaryEmailCache = mutableMapOf<String, String?>()

    fun getPrimaryEmail(contactId: String): String? {
        return primaryEmailCache.getOrPut(contactId) {
            val emails = getEmails(contactId)
            emails.find { it.isPrimary }?.email ?: emails.firstOrNull()?.email
        }
    }

    /**
     * Get contact by ID
     */
    private suspend fun getContactById(contactId: String): Contact? = withContext(ioDispatcher) {
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)

        val projection = arrayOf(
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.STARRED,
            ContactsContract.Contacts.LAST_TIME_CONTACTED,
            ContactsContract.Contacts.TIMES_CONTACTED,
            ContactsContract.Contacts.LOOKUP_KEY
        )

        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val photoIndex = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
                    val starredIndex = cursor.getColumnIndex(ContactsContract.Contacts.STARRED)
                    val lastContactedIndex =
                        cursor.getColumnIndex(ContactsContract.Contacts.LAST_TIME_CONTACTED)
                    val timesContactedIndex =
                        cursor.getColumnIndex(ContactsContract.Contacts.TIMES_CONTACTED)
                    val lookupKeyIndex = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)

                    val displayName = cursor.getString(nameIndex) ?: "Unknown"
                    val photoUri = if (photoIndex >= 0) {
                        cursor.getString(photoIndex)?.let { Uri.parse(it) }
                    } else null
                    val starred = starredIndex >= 0 && cursor.getInt(starredIndex) == 1
                    val lastTimeContacted =
                        if (lastContactedIndex >= 0) cursor.getLong(lastContactedIndex) else 0L
                    val timesContacted =
                        if (timesContactedIndex >= 0) cursor.getInt(timesContactedIndex) else 0
                    val lookupKey =
                        if (lookupKeyIndex >= 0) cursor.getString(lookupKeyIndex) ?: "" else ""

                    val phoneNumbers = getPhoneNumbers(contactId)
                    val emails = getEmails(contactId)
                    val contactType = determineContactType(contactId)

                    return@withContext Contact(
                        id = contactId,
                        displayName = displayName,
                        phoneNumbers = phoneNumbers,
                        emailAddresses = emails,
                        photoUri = photoUri,
                        starred = starred,
                        lastTimeContacted = lastTimeContacted,
                        timesContacted = timesContacted,
                        lookupKey = lookupKey,
                        contactType = contactType
                    )
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting contact by ID")
        }

        return@withContext null
    }

    /**
     * Get phone numbers for a contact
     */
    private fun getPhoneNumbers(contactId: String): List<PhoneNumber> {
        val phoneNumbers = mutableListOf<PhoneNumber>()

        val uri = CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            CommonDataKinds.Phone._ID,
            CommonDataKinds.Phone.NUMBER,
            CommonDataKinds.Phone.TYPE,
            CommonDataKinds.Phone.LABEL,
            CommonDataKinds.Phone.IS_PRIMARY
        )

        val selection = "${CommonDataKinds.Phone.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(contactId)

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                ?.use { cursor ->
                    val idIndex = cursor.getColumnIndex(CommonDataKinds.Phone._ID)
                    val numberIndex = cursor.getColumnIndex(CommonDataKinds.Phone.NUMBER)
                    val typeIndex = cursor.getColumnIndex(CommonDataKinds.Phone.TYPE)
                    val labelIndex = cursor.getColumnIndex(CommonDataKinds.Phone.LABEL)
                    val primaryIndex = cursor.getColumnIndex(CommonDataKinds.Phone.IS_PRIMARY)

                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idIndex)
                        val number = cursor.getString(numberIndex) ?: continue
                        val type = when (cursor.getInt(typeIndex)) {
                            CommonDataKinds.Phone.TYPE_HOME -> PhoneNumberType.HOME
                            CommonDataKinds.Phone.TYPE_WORK -> PhoneNumberType.WORK
                            CommonDataKinds.Phone.TYPE_MOBILE -> PhoneNumberType.MOBILE
                            CommonDataKinds.Phone.TYPE_OTHER -> PhoneNumberType.OTHER
                            else -> PhoneNumberType.CUSTOM
                        }
                        val label = if (type == PhoneNumberType.CUSTOM) {
                            cursor.getString(labelIndex)
                        } else null
                        val isPrimary = primaryIndex >= 0 && cursor.getInt(primaryIndex) == 1

                        phoneNumbers.add(
                            PhoneNumber(
                                id = id,
                                number = number,
                                type = type,
                                label = label,
                                isPrimary = isPrimary
                            )
                        )
                    }
                }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting phone numbers")
        }

        return phoneNumbers
    }

    /**
     * Get email addresses for a contact
     */
    private fun getEmails(contactId: String): List<EmailAddress> {
        val emails = mutableListOf<EmailAddress>()

        val uri = CommonDataKinds.Email.CONTENT_URI
        val projection = arrayOf(
            CommonDataKinds.Email._ID,
            CommonDataKinds.Email.ADDRESS,
            CommonDataKinds.Email.TYPE,
            CommonDataKinds.Email.LABEL,
            CommonDataKinds.Email.IS_PRIMARY
        )

        val selection = "${CommonDataKinds.Email.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(contactId)

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                ?.use { cursor ->
                    val idIndex = cursor.getColumnIndex(CommonDataKinds.Email._ID)
                    val emailIndex = cursor.getColumnIndex(CommonDataKinds.Email.ADDRESS)
                    val typeIndex = cursor.getColumnIndex(CommonDataKinds.Email.TYPE)
                    val labelIndex = cursor.getColumnIndex(CommonDataKinds.Email.LABEL)
                    val primaryIndex = cursor.getColumnIndex(CommonDataKinds.Email.IS_PRIMARY)

                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idIndex)
                        val email = cursor.getString(emailIndex) ?: continue
                        val type = when (cursor.getInt(typeIndex)) {
                            CommonDataKinds.Email.TYPE_HOME -> EmailType.HOME
                            CommonDataKinds.Email.TYPE_WORK -> EmailType.WORK
                            CommonDataKinds.Email.TYPE_OTHER -> EmailType.OTHER
                            else -> EmailType.CUSTOM
                        }
                        val label = if (type == EmailType.CUSTOM) {
                            cursor.getString(labelIndex)
                        } else null
                        val isPrimary = primaryIndex >= 0 && cursor.getInt(primaryIndex) == 1

                        emails.add(
                            EmailAddress(
                                id = id,
                                email = email,
                                type = type,
                                label = label,
                                isPrimary = isPrimary
                            )
                        )
                    }
                }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting emails")
        }

        return emails
    }

    /**
     * Get organizations for a contact
     */
    private fun getOrganizations(contactId: String): List<Organization> {
        val organizations = mutableListOf<Organization>()

        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(
            CommonDataKinds.Organization._ID,
            CommonDataKinds.Organization.COMPANY,
            CommonDataKinds.Organization.TITLE,
            CommonDataKinds.Organization.DEPARTMENT
        )

        val selection =
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(contactId, CommonDataKinds.Organization.CONTENT_ITEM_TYPE)

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                ?.use { cursor ->
                    val idIndex = cursor.getColumnIndex(CommonDataKinds.Organization._ID)
                    val companyIndex = cursor.getColumnIndex(CommonDataKinds.Organization.COMPANY)
                    val titleIndex = cursor.getColumnIndex(CommonDataKinds.Organization.TITLE)
                    val deptIndex = cursor.getColumnIndex(CommonDataKinds.Organization.DEPARTMENT)

                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idIndex)
                        val company = cursor.getString(companyIndex) ?: ""
                        val title = cursor.getString(titleIndex) ?: ""
                        val department = cursor.getString(deptIndex) ?: ""

                        if (company.isNotEmpty() || title.isNotEmpty()) {
                            organizations.add(
                                Organization(
                                    id = id,
                                    name = company,
                                    title = title,
                                    department = department
                                )
                            )
                        }
                    }
                }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting organizations")
        }

        return organizations
    }

    /**
     * Get postal addresses for a contact
     */
    private fun getAddresses(contactId: String): List<PostalAddress> {
        val addresses = mutableListOf<PostalAddress>()

        val uri = CommonDataKinds.StructuredPostal.CONTENT_URI
        val projection = arrayOf(
            CommonDataKinds.StructuredPostal._ID,
            CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
            CommonDataKinds.StructuredPostal.TYPE,
            CommonDataKinds.StructuredPostal.STREET,
            CommonDataKinds.StructuredPostal.CITY,
            CommonDataKinds.StructuredPostal.REGION,
            CommonDataKinds.StructuredPostal.POSTCODE,
            CommonDataKinds.StructuredPostal.COUNTRY
        )

        val selection = "${CommonDataKinds.StructuredPostal.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(contactId)

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                ?.use { cursor ->
                    val idIndex = cursor.getColumnIndex(CommonDataKinds.StructuredPostal._ID)
                    val formattedIndex =
                        cursor.getColumnIndex(CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)
                    val typeIndex = cursor.getColumnIndex(CommonDataKinds.StructuredPostal.TYPE)
                    val streetIndex = cursor.getColumnIndex(CommonDataKinds.StructuredPostal.STREET)
                    val cityIndex = cursor.getColumnIndex(CommonDataKinds.StructuredPostal.CITY)
                    val regionIndex = cursor.getColumnIndex(CommonDataKinds.StructuredPostal.REGION)
                    val postcodeIndex =
                        cursor.getColumnIndex(CommonDataKinds.StructuredPostal.POSTCODE)
                    val countryIndex =
                        cursor.getColumnIndex(CommonDataKinds.StructuredPostal.COUNTRY)

                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idIndex)
                        val formattedAddress = cursor.getString(formattedIndex) ?: ""
                        val type = when (cursor.getInt(typeIndex)) {
                            CommonDataKinds.StructuredPostal.TYPE_HOME -> AddressType.HOME
                            CommonDataKinds.StructuredPostal.TYPE_WORK -> AddressType.WORK
                            else -> AddressType.OTHER
                        }
                        val street = cursor.getString(streetIndex)
                        val city = cursor.getString(cityIndex)
                        val state = cursor.getString(regionIndex)
                        val postalCode = cursor.getString(postcodeIndex)
                        val country = cursor.getString(countryIndex)

                        addresses.add(
                            PostalAddress(
                                id = id,
                                formattedAddress = formattedAddress,
                                type = type,
                                street = street,
                                city = city,
                                state = state,
                                postalCode = postalCode,
                                country = country
                            )
                        )
                    }
                }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting addresses")
        }

        return addresses
    }

    /**
     * Get websites for a contact
     */
    private fun getWebsites(contactId: String): List<String> {
        val websites = mutableListOf<String>()

        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(CommonDataKinds.Website.URL)

        val selection =
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(contactId, CommonDataKinds.Website.CONTENT_ITEM_TYPE)

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                ?.use { cursor ->
                    val urlIndex = cursor.getColumnIndex(CommonDataKinds.Website.URL)

                    while (cursor.moveToNext()) {
                        val url = cursor.getString(urlIndex)
                        if (!url.isNullOrBlank()) {
                            websites.add(url)
                        }
                    }
                }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting websites")
        }

        return websites
    }

    /**
     * Get notes for a contact
     */
    private fun getNotes(contactId: String): String? {
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(CommonDataKinds.Note.NOTE)

        val selection =
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(contactId, CommonDataKinds.Note.CONTENT_ITEM_TYPE)

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                ?.use { cursor ->
                    val noteIndex = cursor.getColumnIndex(CommonDataKinds.Note.NOTE)
                    if (cursor.moveToFirst()) {
                        return cursor.getString(noteIndex)
                    }
                }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting notes")
        }

        return null
    }

    /**
     * Get events (birthdays, anniversaries) for a contact
     */
    private fun getEvents(contactId: String): List<ContactEvent> {
        val events = mutableListOf<ContactEvent>()

        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(
            CommonDataKinds.Event._ID,
            CommonDataKinds.Event.START_DATE,
            CommonDataKinds.Event.TYPE,
            CommonDataKinds.Event.LABEL
        )

        val selection =
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(contactId, CommonDataKinds.Event.CONTENT_ITEM_TYPE)

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                ?.use { cursor ->
                    val idIndex = cursor.getColumnIndex(CommonDataKinds.Event._ID)
                    val dateIndex = cursor.getColumnIndex(CommonDataKinds.Event.START_DATE)
                    val typeIndex = cursor.getColumnIndex(CommonDataKinds.Event.TYPE)
                    val labelIndex = cursor.getColumnIndex(CommonDataKinds.Event.LABEL)

                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idIndex)
                        val dateStr = cursor.getString(dateIndex) ?: continue
                        val type = when (cursor.getInt(typeIndex)) {
                            CommonDataKinds.Event.TYPE_BIRTHDAY -> EventType.BIRTHDAY
                            CommonDataKinds.Event.TYPE_ANNIVERSARY -> EventType.ANNIVERSARY
                            else -> EventType.OTHER
                        }
                        val label = cursor.getString(labelIndex)

                        val date = try {
                            val cleanDate = dateStr.removePrefix("--")
                            val parts = cleanDate.split("-")
                            val calendar = Calendar.getInstance()

                            if (parts.size == 3) {
                                calendar.set(
                                    parts[0].toInt(),
                                    parts[1].toInt() - 1,
                                    parts[2].toInt()
                                )
                            } else if (parts.size == 2) {
                                calendar.set(Calendar.MONTH, parts[0].toInt() - 1)
                                calendar.set(Calendar.DAY_OF_MONTH, parts[1].toInt())
                            }
                            calendar.timeInMillis
                        } catch (e: Exception) {
                            0L
                        }

                        events.add(
                            ContactEvent(
                                id = id,
                                type = type,
                                date = date,
                                label = label
                            )
                        )
                    }
                }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting events")
        }

        return events
    }

    /**
     * Determine contact type
     */
    fun determineContactType(contactId: String): ContactType {
        val uri = ContactsContract.RawContacts.CONTENT_URI
        val projection = arrayOf(ContactsContract.RawContacts.ACCOUNT_TYPE)
        val selection = "${ContactsContract.RawContacts.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(contactId)

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                ?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val accountType = cursor.getString(0) ?: continue
                        return when {
                            accountType.contains("google", ignoreCase = true) -> ContactType.GOOGLE
                            accountType.contains("sim", ignoreCase = true) -> ContactType.SIM
                            accountType.contains(
                                "whatsapp",
                                ignoreCase = true
                            ) -> ContactType.WHATSAPP

                            else -> ContactType.PHONE
                        }
                    }
                }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error determining contact type")
        }

        return ContactType.PHONE
    }

    /**
     * Generate initials from display name
     */
    fun generateInitials(displayName: String): String {
        val parts = displayName.trim().split("\\s+".toRegex())
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(1).uppercase()
            else -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
        }
    }

    /**
     * Add a new contact
     */
    suspend fun addContact(
        displayName: String,
        phoneNumbers: List<PhoneNumberData> = emptyList(),
        emails: List<EmailData> = emptyList(),
        organization: OrganizationData? = null,
        address: AddressData? = null,
        note: String? = null
    ): Boolean = withContext(ioDispatcher) {
        if (!hasWriteContactsPermission()) return@withContext false

        val operations = ArrayList<ContentProviderOperation>()

        operations.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )

        operations.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                )
                .withValue(CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                .build()
        )

        phoneNumbers.forEach { phoneData ->
            operations.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
                    .withValue(CommonDataKinds.Phone.NUMBER, phoneData.number)
                    .withValue(CommonDataKinds.Phone.TYPE, phoneData.type)
                    .withValue(CommonDataKinds.Phone.LABEL, phoneData.label)
                    .withValue(CommonDataKinds.Phone.IS_PRIMARY, phoneData.isPrimary)
                    .build()
            )
        }

        emails.forEach { emailData ->
            operations.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        CommonDataKinds.Email.CONTENT_ITEM_TYPE
                    )
                    .withValue(CommonDataKinds.Email.ADDRESS, emailData.email)
                    .withValue(CommonDataKinds.Email.TYPE, emailData.type)
                    .withValue(CommonDataKinds.Email.LABEL, emailData.label)
                    .withValue(CommonDataKinds.Email.IS_PRIMARY, emailData.isPrimary)
                    .build()
            )
        }

        organization?.let {
            operations.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        CommonDataKinds.Organization.CONTENT_ITEM_TYPE
                    )
                    .withValue(CommonDataKinds.Organization.COMPANY, it.name)
                    .withValue(CommonDataKinds.Organization.TITLE, it.title)
                    .withValue(CommonDataKinds.Organization.DEPARTMENT, it.department)
                    .build()
            )
        }

        address?.let {
            operations.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE
                    )
                    .withValue(
                        CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
                        it.formattedAddress
                    )
                    .withValue(CommonDataKinds.StructuredPostal.STREET, it.street)
                    .withValue(CommonDataKinds.StructuredPostal.CITY, it.city)
                    .withValue(CommonDataKinds.StructuredPostal.REGION, it.state)
                    .withValue(CommonDataKinds.StructuredPostal.POSTCODE, it.postalCode)
                    .withValue(CommonDataKinds.StructuredPostal.COUNTRY, it.country)
                    .withValue(CommonDataKinds.StructuredPostal.TYPE, it.type)
                    .build()
            )
        }

        note?.let {
            operations.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        CommonDataKinds.Note.CONTENT_ITEM_TYPE
                    )
                    .withValue(CommonDataKinds.Note.NOTE, note)
                    .build()
            )
        }

        return@withContext try {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            primaryPhoneCache.clear()
            primaryEmailCache.clear()
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error adding contact")
            false
        }
    }

    /**
     * Update an existing contact
     */
    suspend fun updateContact(
        contactId: String,
        displayName: String? = null,
        phoneNumbers: List<PhoneNumberData>? = null,
        emails: List<EmailData>? = null,
        organization: OrganizationData? = null,
        address: AddressData? = null,
        note: String? = null
    ): Boolean = withContext(ioDispatcher) {
        if (!hasWriteContactsPermission()) return@withContext false

        val operations = ArrayList<ContentProviderOperation>()

        displayName?.let {
            operations.add(
                ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(contactId, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    )
                    .withValue(CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                    .build()
            )
        }

        phoneNumbers?.let {
            operations.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(contactId, CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    )
                    .build()
            )

            phoneNumbers.forEach { phoneData ->
                operations.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, contactId)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                        )
                        .withValue(CommonDataKinds.Phone.NUMBER, phoneData.number)
                        .withValue(CommonDataKinds.Phone.TYPE, phoneData.type)
                        .withValue(CommonDataKinds.Phone.LABEL, phoneData.label)
                        .withValue(CommonDataKinds.Phone.IS_PRIMARY, phoneData.isPrimary)
                        .build()
                )
            }
        }

        return@withContext try {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            primaryPhoneCache.clear()
            primaryEmailCache.clear()
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error updating contact")
            false
        }
    }

    /**
     * Delete a contact
     */
    suspend fun deleteContact(contactId: String): Boolean = withContext(ioDispatcher) {
        if (!hasWriteContactsPermission()) return@withContext false

        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)

        return@withContext try {
            val deletedRows = context.contentResolver.delete(uri, null, null)
            if (deletedRows > 0) {
                primaryPhoneCache.remove(contactId)
                primaryEmailCache.remove(contactId)
            }
            deletedRows > 0
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error deleting contact")
            false
        }
    }

    /**
     * Mark contact as favorite/starred
     */
    suspend fun setContactStarred(contactId: String, starred: Boolean): Boolean =
        withContext(ioDispatcher) {
            if (!hasWriteContactsPermission()) return@withContext false

            val values = ContentValues().apply {
                put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0)
            }

            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)

            return@withContext try {
                context.contentResolver.update(uri, values, null, null) > 0
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error setting contact starred")
                false
            }
        }

    /**
     * Get contact groups
     */
    suspend fun getContactGroups(): List<ContactGroup> = withContext(ioDispatcher) {
        val groups = mutableListOf<ContactGroup>()

        val projection = arrayOf(
            ContactsContract.Groups._ID,
            ContactsContract.Groups.TITLE,
            ContactsContract.Groups.SUMMARY_COUNT
        )

        try {
            context.contentResolver.query(
                ContactsContract.Groups.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.Groups.TITLE
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(ContactsContract.Groups._ID)
                val titleIndex = cursor.getColumnIndex(ContactsContract.Groups.TITLE)
                val countIndex = cursor.getColumnIndex(ContactsContract.Groups.SUMMARY_COUNT)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIndex)
                    val title = cursor.getString(titleIndex) ?: continue
                    val count = if (countIndex >= 0) cursor.getInt(countIndex) else 0

                    if (count > 0) {
                        groups.add(ContactGroup(id = id, title = title, count = count))
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting contact groups")
        }

        return@withContext groups
    }

    /**
     * Get contacts by group
     */
    suspend fun getContactsByGroup(groupId: String): List<ContactQuickInfo> =
        withContext(ioDispatcher) {
            if (!hasContactsPermission()) return@withContext emptyList()

            val contacts = mutableListOf<ContactQuickInfo>()

            val uri = ContactsContract.Data.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.DISPLAY_NAME
            )

            val selection =
                "${ContactsContract.Data.MIMETYPE} = ? AND ${CommonDataKinds.GroupMembership.GROUP_ROW_ID} = ?"
            val selectionArgs = arrayOf(CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE, groupId)

            try {
                context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                    ?.use { cursor ->
                        val contactIdIndex = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                        val nameIndex = cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME)

                        while (cursor.moveToNext()) {
                            val contactId = cursor.getString(contactIdIndex) ?: continue
                            val displayName = cursor.getString(nameIndex) ?: "Unknown"

                            contacts.add(
                                ContactQuickInfo(
                                    id = contactId,
                                    displayName = displayName,
                                    primaryPhoneNumber = getPrimaryPhoneNumber(contactId),
                                    primaryEmail = getPrimaryEmail(contactId),
                                    photoUri = null,
                                    contactType = determineContactType(contactId),
                                    initials = generateInitials(displayName),
                                    isStarred = false
                                )
                            )
                        }
                    }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error getting contacts by group")
            }

            return@withContext contacts
        }

    // Helper methods
    private suspend fun getContactIdFromPhoneNumber(phoneNumber: String): String? =
        withContext(ioDispatcher) {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup._ID)

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return@withContext cursor.getString(0)
                }
            }
            return@withContext null
        }
}

// Paging Sources

// Enums for sorting and filtering
enum class ContactSort {
    NAME_ASC,
    NAME_DESC,
    RECENT_ASC,
    RECENT_DESC
}

enum class ContactFilter {
    ALL,
    STARRED
}


fun isValidPhone(number: String?): Boolean {
    if (number.isNullOrBlank()) return false

    val cleaned = number.filter { it.isDigit() || it == '+' }

    return cleaned.length >= 5 // basic sanity (avoid junk like "1", "*", etc)
}

fun normalizeNumber(number: String): String {
    return number.filter { it.isDigit() }
}