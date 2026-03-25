package com.ghost.caller.models


import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds

data class Contact(
    val id: String,
    val displayName: String,
    val phoneNumbers: List<PhoneNumber>,
    val emailAddresses: List<EmailAddress>,
    val photoUri: Uri?,
    val starred: Boolean,
    val lastTimeContacted: Long,
    val timesContacted: Int,
    val lookupKey: String,
    val contactType: ContactType
)

data class PhoneNumber(
    val id: String,
    val number: String,
    val type: PhoneNumberType,
    val label: String?,
    val isPrimary: Boolean
)

data class EmailAddress(
    val id: String,
    val email: String,
    val type: EmailType,
    val label: String?,
    val isPrimary: Boolean
)

enum class PhoneNumberType {
    HOME, WORK, MOBILE, OTHER, CUSTOM
}

enum class EmailType {
    HOME, WORK, OTHER, CUSTOM
}

enum class ContactType {
    GOOGLE, PHONE, SIM, WHATSAPP, OTHER
}

data class ContactGroup(
    val id: String,
    val title: String,
    val count: Int
)

data class ContactWithDetails(
    val contact: Contact,
    val organizations: List<Organization>,
    val addresses: List<PostalAddress>,
    val websites: List<String>,
    val notes: String?,
    val events: List<ContactEvent>
)

data class Organization(
    val id: String,
    val name: String,
    val title: String,
    val department: String
)

data class PostalAddress(
    val id: String,
    val formattedAddress: String,
    val type: AddressType,
    val street: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val country: String?
)

enum class AddressType {
    HOME, WORK, OTHER
}

data class ContactEvent(
    val id: String,
    val type: EventType,
    val date: Long,
    val label: String?
)

enum class EventType {
    BIRTHDAY, ANNIVERSARY, OTHER
}

data class ContactQuickInfo(
    val id: String,
    val displayName: String,
    val primaryPhoneNumber: String?,
    val primaryEmail: String?,
    val photoUri: Uri?,
    val contactType: ContactType,
    val initials: String,
    val isStarred: Boolean
)


// Data classes for adding/updating contacts
data class PhoneNumberData(
    val number: String,
    val type: Int = CommonDataKinds.Phone.TYPE_MOBILE,
    val label: String? = null,
    val isPrimary: Boolean = false
)

data class EmailData(
    val email: String,
    val type: Int = CommonDataKinds.Email.TYPE_HOME,
    val label: String? = null,
    val isPrimary: Boolean = false
)

data class OrganizationData(
    val name: String,
    val title: String = "",
    val department: String = ""
)

data class AddressData(
    val formattedAddress: String = "",
    val street: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val type: Int = CommonDataKinds.StructuredPostal.TYPE_HOME
)