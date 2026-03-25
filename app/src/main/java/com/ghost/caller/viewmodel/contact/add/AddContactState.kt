package com.ghost.caller.viewmodel.contact.add

import android.net.Uri
import com.ghost.caller.models.AddressType
import com.ghost.caller.models.EmailType
import com.ghost.caller.models.EventType
import com.ghost.caller.models.PhoneNumberType


/**
 * Add/Edit Contact UI State
 */
data class AddEditContactState(
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val contactId: String? = null,

    // Basic Info
    val displayName: String = "",
    val displayNameError: String? = null,
    val photoUri: Uri? = null,
    val isStarred: Boolean = false,

    // Phone Numbers
    val phoneNumbers: List<EditablePhoneNumber> = listOf(EditablePhoneNumber()),
    val phoneNumbersError: String? = null,

    // Email Addresses
    val emails: List<EditableEmail> = listOf(EditableEmail()),
    val emailsError: String? = null,

    // Organization
    val organization: String = "",
    val jobTitle: String = "",
    val department: String = "",

    // Address
    val address: EditableAddress = EditableAddress(),

    // Notes
    val notes: String = "",

    // Events
    val events: List<EditableEvent> = emptyList(),

    // Websites
    val websites: List<String> = emptyList(),

    // Validation
    val isValid: Boolean = false,
    val saveEnabled: Boolean = false,

    // UI State
    val expandedSections: Set<ContactSection> = setOf(
        ContactSection.BASIC_INFO,
        ContactSection.PHONE_NUMBERS
    ),
    val error: String? = null,
    val successMessage: String? = null
)

enum class ContactSection {
    BASIC_INFO,
    PHONE_NUMBERS,
    EMAIL_ADDRESSES,
    ORGANIZATION,
    ADDRESS,
    NOTES,
    EVENTS,
    WEBSITES
}

data class EditablePhoneNumber(
    val id: String = System.currentTimeMillis().toString(),
    val number: String = "",
    val type: PhoneNumberType = PhoneNumberType.MOBILE,
    val label: String? = null,
    val isPrimary: Boolean = false,
    val isNew: Boolean = true,
    val error: String? = null
)

data class EditableEmail(
    val id: String = System.currentTimeMillis().toString(),
    val email: String = "",
    val type: EmailType = EmailType.HOME,
    val label: String? = null,
    val isPrimary: Boolean = false,
    val isNew: Boolean = true,
    val error: String? = null
)

data class EditableAddress(
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val country: String = "",
    val type: AddressType = AddressType.HOME
)

data class EditableEvent(
    val id: String = System.currentTimeMillis().toString(),
    val type: EventType = EventType.BIRTHDAY,
    val date: Long? = null,
    val label: String? = null,
    val isNew: Boolean = true
)

/**
 * Add/Edit Contact UI Events
 */
sealed class AddEditContactEvent {
    // Basic Info
    data class UpdateDisplayName(val name: String) : AddEditContactEvent()
    data class UpdatePhoto(val uri: Uri) : AddEditContactEvent()
    data object RemovePhoto : AddEditContactEvent()
    data class ToggleStarred(val starred: Boolean) : AddEditContactEvent()

    // Phone Numbers
    data object AddPhoneNumber : AddEditContactEvent()
    data class RemovePhoneNumber(val id: String) : AddEditContactEvent()
    data class UpdatePhoneNumber(val id: String, val number: String) : AddEditContactEvent()
    data class UpdatePhoneNumberType(val id: String, val type: PhoneNumberType) :
        AddEditContactEvent()

    data class UpdatePhoneNumberLabel(val id: String, val label: String) : AddEditContactEvent()
    data class SetPrimaryPhoneNumber(val id: String) : AddEditContactEvent()

    // Email Addresses
    data object AddEmail : AddEditContactEvent()
    data class RemoveEmail(val id: String) : AddEditContactEvent()
    data class UpdateEmail(val id: String, val email: String) : AddEditContactEvent()
    data class UpdateEmailType(val id: String, val type: EmailType) : AddEditContactEvent()
    data class UpdateEmailLabel(val id: String, val label: String) : AddEditContactEvent()
    data class SetPrimaryEmail(val id: String) : AddEditContactEvent()

    // Organization
    data class UpdateOrganization(val organization: String) : AddEditContactEvent()
    data class UpdateJobTitle(val title: String) : AddEditContactEvent()
    data class UpdateDepartment(val department: String) : AddEditContactEvent()

    // Address
    data class UpdateAddressStreet(val street: String) : AddEditContactEvent()
    data class UpdateAddressCity(val city: String) : AddEditContactEvent()
    data class UpdateAddressState(val state: String) : AddEditContactEvent()
    data class UpdateAddressPostalCode(val code: String) : AddEditContactEvent()
    data class UpdateAddressCountry(val country: String) : AddEditContactEvent()
    data class UpdateAddressType(val type: AddressType) : AddEditContactEvent()

    // Notes
    data class UpdateNotes(val notes: String) : AddEditContactEvent()

    // Events
    data object AddEvent : AddEditContactEvent()
    data class RemoveEvent(val id: String) : AddEditContactEvent()
    data class UpdateEventType(val id: String, val type: EventType) : AddEditContactEvent()
    data class UpdateEventDate(val id: String, val date: Long) : AddEditContactEvent()
    data class UpdateEventLabel(val id: String, val label: String) : AddEditContactEvent()

    // Websites
    data object AddWebsite : AddEditContactEvent()
    data class RemoveWebsite(val index: Int) : AddEditContactEvent()
    data class UpdateWebsite(val index: Int, val url: String) : AddEditContactEvent()

    // UI State
    data class ToggleSection(val section: ContactSection, val expanded: Boolean) :
        AddEditContactEvent()

    // Save
    object SaveContact : AddEditContactEvent()
    object Cancel : AddEditContactEvent()

    // Clear errors
    object ClearError : AddEditContactEvent()
    object ClearSuccessMessage : AddEditContactEvent()
}

/**
 * Add/Edit Contact Side Effects
 */
sealed class AddEditContactSideEffect {
    data class ShowToast(val message: String) : AddEditContactSideEffect()
    data class ShowError(val message: String) : AddEditContactSideEffect()
    data class NavigateBack(val contactId: String? = null) : AddEditContactSideEffect()
    data object ShowImagePicker : AddEditContactSideEffect()
    data class ShowDatePicker(val eventId: String) : AddEditContactSideEffect()
    object ShowDeleteConfirmation : AddEditContactSideEffect()
}