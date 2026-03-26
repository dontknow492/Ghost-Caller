@file:Suppress("D")

package com.ghost.caller.viewmodel.contact.add


import android.app.Application
import android.net.Uri
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ghost.caller.models.AddressData
import com.ghost.caller.models.AddressType
import com.ghost.caller.models.EmailData
import com.ghost.caller.models.EmailType
import com.ghost.caller.models.EventType
import com.ghost.caller.models.OrganizationData
import com.ghost.caller.models.PhoneNumberData
import com.ghost.caller.models.PhoneNumberType
import com.ghost.caller.repository.ContactRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class AddEditContactViewModel(
    application: Application,
    private val contactRepository: ContactRepository,
    private val phoneNumber: String? = null,
    private val name: String? = null,
) : AndroidViewModel(application) {

    // Private mutable state flow
    private val _state = MutableStateFlow(AddEditContactState(isEditMode = phoneNumber != null))
    val state: StateFlow<AddEditContactState> = _state.asStateFlow()

    // Side effects channel
    private val _sideEffect = MutableSharedFlow<AddEditContactSideEffect>(
        replay = 0,
        extraBufferCapacity = 10
    )
    val sideEffect: SharedFlow<AddEditContactSideEffect> = _sideEffect.asSharedFlow()

    // Event channel
    private val _event = MutableSharedFlow<AddEditContactEvent>(
        replay = 0,
        extraBufferCapacity = 10
    )

    init {
        viewModelScope.launch {
            _event.collect { event ->
                handleEvent(event)
            }
        }

        // Load contact if in edit mode
        if (phoneNumber != null) {
            loadContact(phoneNumber)
        }

        // Validate form on any change
        viewModelScope.launch {
            _state
                .debounce(300)
                .collect { validateForm() }
        }
    }

    fun sendEvent(event: AddEditContactEvent) {
        viewModelScope.launch {
            _event.emit(event)
        }
    }

    private fun handleEvent(event: AddEditContactEvent) {
        when (event) {
            is AddEditContactEvent.UpdateDisplayName -> updateDisplayName(event.name)
            is AddEditContactEvent.UpdatePhoto -> updatePhoto(event.uri)
            is AddEditContactEvent.RemovePhoto -> removePhoto()
            is AddEditContactEvent.ToggleStarred -> toggleStarred(event.starred)

            is AddEditContactEvent.AddPhoneNumber -> addPhoneNumber()
            is AddEditContactEvent.RemovePhoneNumber -> removePhoneNumber(event.id)
            is AddEditContactEvent.UpdatePhoneNumber -> updatePhoneNumber(event.id, event.number)
            is AddEditContactEvent.UpdatePhoneNumberType -> updatePhoneNumberType(
                event.id,
                event.type
            )

            is AddEditContactEvent.UpdatePhoneNumberLabel -> updatePhoneNumberLabel(
                event.id,
                event.label
            )

            is AddEditContactEvent.SetPrimaryPhoneNumber -> setPrimaryPhoneNumber(event.id)

            is AddEditContactEvent.AddEmail -> addEmail()
            is AddEditContactEvent.RemoveEmail -> removeEmail(event.id)
            is AddEditContactEvent.UpdateEmail -> updateEmail(event.id, event.email)
            is AddEditContactEvent.UpdateEmailType -> updateEmailType(event.id, event.type)
            is AddEditContactEvent.UpdateEmailLabel -> updateEmailLabel(event.id, event.label)
            is AddEditContactEvent.SetPrimaryEmail -> setPrimaryEmail(event.id)

            is AddEditContactEvent.UpdateOrganization -> updateOrganization(event.organization)
            is AddEditContactEvent.UpdateJobTitle -> updateJobTitle(event.title)
            is AddEditContactEvent.UpdateDepartment -> updateDepartment(event.department)

            is AddEditContactEvent.UpdateAddressStreet -> updateAddressStreet(event.street)
            is AddEditContactEvent.UpdateAddressCity -> updateAddressCity(event.city)
            is AddEditContactEvent.UpdateAddressState -> updateAddressState(event.state)
            is AddEditContactEvent.UpdateAddressPostalCode -> updateAddressPostalCode(event.code)
            is AddEditContactEvent.UpdateAddressCountry -> updateAddressCountry(event.country)
            is AddEditContactEvent.UpdateAddressType -> updateAddressType(event.type)

            is AddEditContactEvent.UpdateNotes -> updateNotes(event.notes)

            is AddEditContactEvent.AddEvent -> addEvent()
            is AddEditContactEvent.RemoveEvent -> removeEvent(event.id)
            is AddEditContactEvent.UpdateEventType -> updateEventType(event.id, event.type)
            is AddEditContactEvent.UpdateEventDate -> updateEventDate(event.id, event.date)
            is AddEditContactEvent.UpdateEventLabel -> updateEventLabel(event.id, event.label)

            is AddEditContactEvent.AddWebsite -> addWebsite()
            is AddEditContactEvent.RemoveWebsite -> removeWebsite(event.index)
            is AddEditContactEvent.UpdateWebsite -> updateWebsite(event.index, event.url)

            is AddEditContactEvent.ToggleSection -> toggleSection(event.section, event.expanded)

            is AddEditContactEvent.SaveContact -> saveContact()
            is AddEditContactEvent.Cancel -> cancel()

            is AddEditContactEvent.ClearError -> clearError()
            is AddEditContactEvent.ClearSuccessMessage -> clearSuccessMessage()
        }
    }

    private fun loadContact(contactId: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }

                val contactDetails = contactRepository.getContactDetails(contactId)
                contactDetails?.let { details ->
                    val contact = details.contact

                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            contactId = contact.id,
                            displayName = contact.displayName,
                            photoUri = contact.photoUri,
                            isStarred = contact.starred,
                            phoneNumbers = contact.phoneNumbers.map { phone ->
                                EditablePhoneNumber(
                                    id = phone.id,
                                    number = phone.number,
                                    type = phone.type,
                                    label = phone.label,
                                    isPrimary = phone.isPrimary,
                                    isNew = false
                                )
                            },
                            emails = contact.emailAddresses.map { email ->
                                EditableEmail(
                                    id = email.id,
                                    email = email.email,
                                    type = email.type,
                                    label = email.label,
                                    isPrimary = email.isPrimary,
                                    isNew = false
                                )
                            },
                            organization = details.organizations.firstOrNull()?.name ?: "",
                            jobTitle = details.organizations.firstOrNull()?.title ?: "",
                            department = details.organizations.firstOrNull()?.department ?: "",
                            address = details.addresses.firstOrNull()?.let { addr ->
                                EditableAddress(
                                    street = addr.street ?: "",
                                    city = addr.city ?: "",
                                    state = addr.state ?: "",
                                    postalCode = addr.postalCode ?: "",
                                    country = addr.country ?: "",
                                    type = addr.type
                                )
                            } ?: EditableAddress(),
                            notes = details.notes ?: "",
                            events = details.events.map { event ->
                                EditableEvent(
                                    id = event.id,
                                    type = event.type,
                                    date = event.date,
                                    label = event.label,
                                    isNew = false
                                )
                            },
                            websites = details.websites.toMutableList()
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
                sendSideEffect(AddEditContactSideEffect.ShowError("Failed to load contact"))
            }
        }
    }

    // Basic Info Handlers
    private fun updateDisplayName(name: String) {
        _state.update { it.copy(displayName = name) }
    }

    private fun updatePhoto(uri: Uri) {
        _state.update { it.copy(photoUri = uri) }
    }

    private fun removePhoto() {
        _state.update { it.copy(photoUri = null) }
    }

    private fun toggleStarred(starred: Boolean) {
        _state.update { it.copy(isStarred = starred) }
    }

    // Phone Number Handlers
    private fun addPhoneNumber() {
        _state.update { state ->
            state.copy(
                phoneNumbers = state.phoneNumbers + EditablePhoneNumber(
                    id = System.currentTimeMillis().toString()
                )
            )
        }
    }

    private fun removePhoneNumber(id: String) {
        _state.update { state ->
            val phoneNumbers = state.phoneNumbers.filter { it.id != id }
            state.copy(phoneNumbers = phoneNumbers)
        }
    }

    private fun updatePhoneNumber(id: String, number: String) {
        _state.update { state ->
            val updatedNumbers = state.phoneNumbers.map { phone ->
                if (phone.id == id) {
                    phone.copy(
                        number = number,
                        error = if (number.isBlank()) "Phone number required" else null
                    )
                } else phone
            }
            state.copy(phoneNumbers = updatedNumbers)
        }
    }

    private fun updatePhoneNumberType(id: String, type: PhoneNumberType) {
        _state.update { state ->
            val updatedNumbers = state.phoneNumbers.map { phone ->
                if (phone.id == id) phone.copy(type = type) else phone
            }
            state.copy(phoneNumbers = updatedNumbers)
        }
    }

    private fun updatePhoneNumberLabel(id: String, label: String) {
        _state.update { state ->
            val updatedNumbers = state.phoneNumbers.map { phone ->
                if (phone.id == id) phone.copy(label = label) else phone
            }
            state.copy(phoneNumbers = updatedNumbers)
        }
    }

    private fun setPrimaryPhoneNumber(id: String) {
        _state.update { state ->
            val updatedNumbers = state.phoneNumbers.map { phone ->
                phone.copy(isPrimary = phone.id == id)
            }
            state.copy(phoneNumbers = updatedNumbers)
        }
    }

    // Email Handlers
    private fun addEmail() {
        _state.update { state ->
            state.copy(
                emails = state.emails + EditableEmail(
                    id = System.currentTimeMillis().toString()
                )
            )
        }
    }

    private fun removeEmail(id: String) {
        _state.update { state ->
            val emails = state.emails.filter { it.id != id }
            state.copy(emails = emails)
        }
    }

    private fun updateEmail(id: String, email: String) {
        _state.update { state ->
            val updatedEmails = state.emails.map { emailItem ->
                if (emailItem.id == id) {
                    emailItem.copy(
                        email = email,
                        error = if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(
                                email
                            ).matches()
                        )
                            "Invalid email format" else null
                    )
                } else emailItem
            }
            state.copy(emails = updatedEmails)
        }
    }

    private fun updateEmailType(id: String, type: EmailType) {
        _state.update { state ->
            val updatedEmails = state.emails.map { email ->
                if (email.id == id) email.copy(type = type) else email
            }
            state.copy(emails = updatedEmails)
        }
    }

    private fun updateEmailLabel(id: String, label: String) {
        _state.update { state ->
            val updatedEmails = state.emails.map { email ->
                if (email.id == id) email.copy(label = label) else email
            }
            state.copy(emails = updatedEmails)
        }
    }

    private fun setPrimaryEmail(id: String) {
        _state.update { state ->
            val updatedEmails = state.emails.map { email ->
                email.copy(isPrimary = email.id == id)
            }
            state.copy(emails = updatedEmails)
        }
    }

    // Organization Handlers
    private fun updateOrganization(organization: String) {
        _state.update { it.copy(organization = organization) }
    }

    private fun updateJobTitle(title: String) {
        _state.update { it.copy(jobTitle = title) }
    }

    private fun updateDepartment(department: String) {
        _state.update { it.copy(department = department) }
    }

    // Address Handlers
    private fun updateAddressStreet(street: String) {
        _state.update { state ->
            state.copy(address = state.address.copy(street = street))
        }
    }

    private fun updateAddressCity(city: String) {
        _state.update { state ->
            state.copy(address = state.address.copy(city = city))
        }
    }

    private fun updateAddressState(state: String) {
        _state.update { currentState ->
            currentState.copy(address = currentState.address.copy(state = state))
        }
    }

    private fun updateAddressPostalCode(code: String) {
        _state.update { currentState ->
            currentState.copy(address = currentState.address.copy(postalCode = code))
        }
    }

    private fun updateAddressCountry(country: String) {
        _state.update { currentState ->
            currentState.copy(address = currentState.address.copy(country = country))
        }
    }

    private fun updateAddressType(type: AddressType) {
        _state.update { currentState ->
            currentState.copy(address = currentState.address.copy(type = type))
        }
    }

    // Notes Handler
    private fun updateNotes(notes: String) {
        _state.update { it.copy(notes = notes) }
    }

    // Events Handlers
    private fun addEvent() {
        _state.update { state ->
            state.copy(
                events = state.events + EditableEvent(
                    id = System.currentTimeMillis().toString()
                )
            )
        }
    }

    private fun removeEvent(id: String) {
        _state.update { state ->
            state.copy(events = state.events.filter { it.id != id })
        }
    }

    private fun updateEventType(id: String, type: EventType) {
        _state.update { state ->
            val updatedEvents = state.events.map { event ->
                if (event.id == id) event.copy(type = type) else event
            }
            state.copy(events = updatedEvents)
        }
    }

    private fun updateEventDate(id: String, date: Long) {
        _state.update { state ->
            val updatedEvents = state.events.map { event ->
                if (event.id == id) event.copy(date = date) else event
            }
            state.copy(events = updatedEvents)
        }
    }

    private fun updateEventLabel(id: String, label: String) {
        _state.update { state ->
            val updatedEvents = state.events.map { event ->
                if (event.id == id) event.copy(label = label) else event
            }
            state.copy(events = updatedEvents)
        }
    }

    // Websites Handlers
    private fun addWebsite() {
        _state.update { state ->
            state.copy(websites = state.websites + "")
        }
    }

    private fun removeWebsite(index: Int) {
        _state.update { state ->
            val websites = state.websites.toMutableList()
            if (index in websites.indices) {
                websites.removeAt(index)
            }
            state.copy(websites = websites)
        }
    }

    private fun updateWebsite(index: Int, url: String) {
        _state.update { state ->
            val websites = state.websites.toMutableList()
            if (index in websites.indices) {
                websites[index] = url
            }
            state.copy(websites = websites)
        }
    }

    // UI State Handlers
    private fun toggleSection(section: ContactSection, expanded: Boolean) {
        _state.update { state ->
            val expandedSections = state.expandedSections.toMutableSet()
            if (expanded) {
                expandedSections.add(section)
            } else {
                expandedSections.remove(section)
            }
            state.copy(expandedSections = expandedSections)
        }
    }

    // Validation
    private fun validateForm() {
        val state = _state.value

        // Validate display name
        val displayNameError = if (state.displayName.isBlank()) {
            "Name is required"
        } else null

        // Validate phone numbers
        val phoneNumbersError = if (state.phoneNumbers.isEmpty()) {
            "At least one phone number is required"
        } else if (state.phoneNumbers.all { it.number.isBlank() }) {
            "Please enter at least one valid phone number"
        } else null

        val hasValidPhoneNumber = state.phoneNumbers.any { it.number.isNotBlank() }

        // Validate emails
        val emailsError = if (state.emails.any { it.email.isNotBlank() && it.error != null }) {
            "Some emails have invalid format"
        } else null

        val isValid = displayNameError == null &&
                phoneNumbersError == null &&
                emailsError == null &&
                hasValidPhoneNumber

        val saveEnabled = isValid && !state.isLoading

        _state.update {
            it.copy(
                displayNameError = displayNameError,
                phoneNumbersError = phoneNumbersError,
                emailsError = emailsError,
                isValid = isValid,
                saveEnabled = saveEnabled
            )
        }
    }

    // Save Contact
    private fun saveContact() {
        if (!_state.value.isValid) return

        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }

                val state = _state.value

                val phoneNumbersData = state.phoneNumbers
                    .filter { it.number.isNotBlank() }
                    .map { phone ->
                        PhoneNumberData(
                            number = phone.number,
                            type = when (phone.type) {
                                PhoneNumberType.HOME -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
                                PhoneNumberType.WORK -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
                                PhoneNumberType.MOBILE -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                                PhoneNumberType.OTHER -> ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
                                PhoneNumberType.CUSTOM -> ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM
                            },
                            label = phone.label,
                            isPrimary = phone.isPrimary
                        )
                    }

                val emailsData = state.emails
                    .filter { it.email.isNotBlank() }
                    .map { email ->
                        EmailData(
                            email = email.email,
                            type = when (email.type) {
                                EmailType.HOME -> ContactsContract.CommonDataKinds.Email.TYPE_HOME
                                EmailType.WORK -> ContactsContract.CommonDataKinds.Email.TYPE_WORK
                                EmailType.OTHER -> ContactsContract.CommonDataKinds.Email.TYPE_OTHER
                                EmailType.CUSTOM -> ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM
                            },
                            label = email.label,
                            isPrimary = email.isPrimary
                        )
                    }

                val organizationData = if (state.organization.isNotBlank()) {
                    OrganizationData(
                        name = state.organization,
                        title = state.jobTitle,
                        department = state.department
                    )
                } else null

                val addressData = if (state.address.street.isNotBlank() ||
                    state.address.city.isNotBlank() ||
                    state.address.country.isNotBlank()
                ) {
                    AddressData(
                        formattedAddress = buildString {
                            if (state.address.street.isNotBlank()) append(state.address.street)
                            if (state.address.city.isNotBlank()) {
                                if (isNotEmpty()) append(", ")
                                append(state.address.city)
                            }
                            if (state.address.state.isNotBlank()) {
                                if (isNotEmpty()) append(", ")
                                append(state.address.state)
                            }
                            if (state.address.postalCode.isNotBlank()) {
                                if (isNotEmpty()) append(" ")
                                append(state.address.postalCode)
                            }
                            if (state.address.country.isNotBlank()) {
                                if (isNotEmpty()) append(", ")
                                append(state.address.country)
                            }
                        },
                        street = state.address.street,
                        city = state.address.city,
                        state = state.address.state,
                        postalCode = state.address.postalCode,
                        country = state.address.country,
                        type = when (state.address.type) {
                            AddressType.HOME -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME
                            AddressType.WORK -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK
                            AddressType.OTHER -> ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER
                        }
                    )
                } else null

                val success = if (state.isEditMode && state.contactId != null) {
                    contactRepository.updateContact(
                        contactId = state.contactId,
                        displayName = state.displayName,
                        phoneNumbers = phoneNumbersData,
                        emails = emailsData,
                        organization = organizationData,
                        address = addressData,
                        note = state.notes.takeIf { it.isNotBlank() }
                    )
                } else {
                    contactRepository.addContact(
                        displayName = state.displayName,
                        phoneNumbers = phoneNumbersData,
                        emails = emailsData,
                        organization = organizationData,
                        address = addressData,
                        note = state.notes.takeIf { it.isNotBlank() }
                    )
                }

                if (success) {
                    sendSideEffect(
                        AddEditContactSideEffect.ShowToast(
                            if (state.isEditMode) "Contact updated successfully" else "Contact added successfully"
                        )
                    )
                    sendSideEffect(AddEditContactSideEffect.NavigateBack(state.contactId))
                } else {
                    throw Exception("Failed to save contact")
                }

                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
                sendSideEffect(AddEditContactSideEffect.ShowError("Failed to save contact"))
            }
        }
    }

    private fun cancel() {
        sendSideEffect(AddEditContactSideEffect.NavigateBack())
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun clearSuccessMessage() {
        _state.update { it.copy(successMessage = null) }
    }

    private fun sendSideEffect(effect: AddEditContactSideEffect) {
        viewModelScope.launch {
            _sideEffect.emit(effect)
        }
    }

    // Helper functions
    fun showDatePicker(eventId: String) {
        sendSideEffect(AddEditContactSideEffect.ShowDatePicker(eventId))
    }

    fun showImagePicker() {
        sendSideEffect(AddEditContactSideEffect.ShowImagePicker)
    }
}