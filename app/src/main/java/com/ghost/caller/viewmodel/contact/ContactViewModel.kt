// ContactViewModel.kt
package com.ghost.caller.viewmodel.contact

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ghost.caller.models.ContactGroup
import com.ghost.caller.models.ContactQuickInfo
import com.ghost.caller.models.ContactWithDetails
import com.ghost.caller.repository.ContactFilter
import com.ghost.caller.repository.ContactRepository
import com.ghost.caller.repository.ContactSort
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContactViewModel(
    application: Application,
    private val contactRepository: ContactRepository
) : AndroidViewModel(application) {

    // ========== UI State ==========
    private val _uiState = MutableStateFlow(ContactUiState())
    val uiState: StateFlow<ContactUiState> = _uiState.asStateFlow()

    // Combined UI State for Compose
    val screenUiState: StateFlow<ContactScreenUiState> = _uiState
        .map { ContactScreenUiState.fromContactUiState(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ContactScreenUiState.fromContactUiState(ContactUiState())
        )

    // ========== Side Effects ==========
    private val _sideEffect = MutableSharedFlow<ContactSideEffect>(
        replay = 0,
        extraBufferCapacity = 10
    )
    val sideEffect: SharedFlow<ContactSideEffect> = _sideEffect.asSharedFlow()

    // ========== Events ==========
    private val _event = MutableSharedFlow<ContactUiEvent>()

    // ========== Paginated Data Flows ==========

    // Main contacts list with pagination
    private val _pagingConfig = MutableStateFlow(
        ContactsPagingConfig(
            sortBy = ContactSort.NAME_ASC,
            filter = ContactFilter.ALL
        )
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val contactsPagingFlow: Flow<PagingData<ContactQuickInfo>> = _pagingConfig
        .flatMapLatest { config ->
            contactRepository.getContactsPaged(
                sortBy = config.sortBy,
                filter = config.filter
            ).cachedIn(viewModelScope)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PagingData.empty()
        )

    // Favorites paginated flow
    val favoritesPagingFlow: Flow<PagingData<ContactQuickInfo>> = contactRepository
        .getFavoriteContactsPaged()
        .cachedIn(viewModelScope)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PagingData.empty()
        )

    // Recent contacts paginated flow
    val recentContactsPagingFlow: Flow<PagingData<ContactQuickInfo>> = contactRepository
        .getRecentContactsPaged()
        .cachedIn(viewModelScope)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PagingData.empty()
        )

    // Search results paginated flow
    private val _searchQuery = MutableStateFlow("")
    val searchResultsPagingFlow: Flow<PagingData<ContactQuickInfo>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isNotBlank()) {
                contactRepository.searchContactsPaged(query).cachedIn(viewModelScope)
            } else {
                flowOf(PagingData.empty())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PagingData.empty()
        )

    // ========== Non-Paginated Data Flows ==========

    // Contact groups
    private val _contactGroups = MutableStateFlow<List<ContactGroup>>(emptyList())
    val contactGroups: StateFlow<List<ContactGroup>> = _contactGroups.asStateFlow()

    // Contact groups with UI data
    val contactGroupsWithUi: StateFlow<List<ContactGroupWithUi>> = _contactGroups
        .map { groups ->
            groups.map { ContactGroupWithUi(group = it) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Grouped contacts for alphabetical view
    private val _groupedContacts = MutableStateFlow<Map<String, List<ContactQuickInfo>>>(emptyMap())
    val groupedContacts: StateFlow<Map<String, List<ContactQuickInfo>>> =
        _groupedContacts.asStateFlow()

    // Selected contact details
    private val _selectedContact = MutableStateFlow<ContactWithDetails?>(null)
    val selectedContact: StateFlow<ContactWithDetails?> = _selectedContact.asStateFlow()

    // ========== Initialization ==========

    init {
        viewModelScope.launch {
            _event.collect { event ->
                handleEvent(event)
            }
        }

        // Load initial non-paginated data
        loadContactGroups()
        loadGroupedContacts()
    }

    // ========== Event Handling ==========

    fun sendEvent(event: ContactUiEvent) {
        viewModelScope.launch {
            _event.emit(event)
        }
    }

    private fun handleEvent(event: ContactUiEvent) {
        when (event) {
            is ContactUiEvent.ChangeTab -> changeTab(event.tab)
            is ContactUiEvent.ChangeViewMode -> changeViewMode(event.viewMode)
            is ContactUiEvent.ChangeSortOrder -> changeSortOrder(event.sortBy)
            is ContactUiEvent.ChangeFilter -> changeFilter(event.filter)
            is ContactUiEvent.SearchContacts -> searchContacts(event.query)
            is ContactUiEvent.ClearSearch -> clearSearch()
            is ContactUiEvent.SelectContact -> selectContact(event.contactId)
            is ContactUiEvent.LoadContactDetails -> loadContactDetails(event.contactId)
            is ContactUiEvent.AddContact -> addContact(event)
            is ContactUiEvent.UpdateContact -> updateContact(event)
            is ContactUiEvent.DeleteContact -> deleteContact(event.contactId)
            is ContactUiEvent.DeleteContacts -> deleteContacts(event.contactIds)
            is ContactUiEvent.ToggleFavorite -> toggleFavorite(event.contactId, event.isStarred)
            is ContactUiEvent.ToggleSelectionMode -> toggleSelectionMode(event.enabled)
            is ContactUiEvent.ToggleContactSelection -> toggleContactSelection(event.contactId)
            is ContactUiEvent.SelectAllContacts -> selectAllContacts()
            is ContactUiEvent.ClearSelectedContacts -> clearSelectedContacts()
            is ContactUiEvent.LoadContactsByGroup -> loadContactsByGroup(event.groupId)
            is ContactUiEvent.LoadContactGroups -> loadContactGroups()
            is ContactUiEvent.LoadGroupedContacts -> loadGroupedContacts()
            is ContactUiEvent.Refresh -> refresh()
            is ContactUiEvent.Retry -> retry()
            is ContactUiEvent.ClearError -> clearError()
            is ContactUiEvent.ClearSuccessMessage -> clearSuccessMessage()
        }
    }

    // ========== Tab and View Management ==========

    private fun changeTab(tab: ContactTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    private fun changeViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    private fun changeSortOrder(sortBy: ContactSort) {
        _pagingConfig.value = _pagingConfig.value.copy(sortBy = sortBy)
        _uiState.update {
            it.copy(contactSort = sortBy)
        }
        sendSideEffect(ContactSideEffect.ShowToast("Sorted by ${getSortDescription(sortBy)}"))
    }

    private fun changeFilter(filter: ContactFilter) {
        _pagingConfig.value = _pagingConfig.value.copy(filter = filter)
        _uiState.update {
            it.copy(currentFilter = filter)
        }
        if (filter != ContactFilter.ALL) {
            sendSideEffect(ContactSideEffect.ShowToast("Showing ${getFilterDescription(filter)}"))
        }
    }

    // ========== Search ==========

    private fun searchContacts(query: String) {
        _searchQuery.value = query
        _uiState.update {
            it.copy(
                searchQuery = query,
                isSearchActive = query.isNotBlank(),
                currentTab = if (query.isNotBlank()) ContactTab.SEARCH else ContactTab.CONTACTS
            )
        }
    }

    private fun clearSearch() {
        _searchQuery.value = ""
        _uiState.update {
            it.copy(
                searchQuery = "",
                isSearchActive = false,
                currentTab = ContactTab.CONTACTS
            )
        }
    }

    // ========== Contact Selection ==========

    private fun selectContact(contactId: String) {
        if (_uiState.value.isSelectionMode) {
            toggleContactSelection(contactId)
        } else {
            sendSideEffect(ContactSideEffect.NavigateToContactDetail(contactId))
        }
    }

    private fun loadContactDetails(contactId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val details = contactRepository.getContactDetails(contactId)
                _selectedContact.value = details
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load contact details") }
                sendSideEffect(ContactSideEffect.ShowError("Failed to load contact details"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ========== CRUD Operations ==========

    private fun addContact(event: ContactUiEvent.AddContact) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val success = contactRepository.addContact(
                    displayName = event.displayName,
                    phoneNumbers = event.phoneNumbers,
                    emails = event.emails,
                    organization = event.organization,
                    address = event.address,
                    note = event.note
                )

                if (success) {
                    _uiState.update { it.copy(successMessage = "Contact added successfully") }
                    sendSideEffect(ContactSideEffect.ShowSuccess("Contact added successfully"))
                    sendSideEffect(ContactSideEffect.NavigateBack)
                    refresh()
                } else {
                    throw Exception("Failed to add contact")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to add contact") }
                sendSideEffect(ContactSideEffect.ShowError("Failed to add contact"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun updateContact(event: ContactUiEvent.UpdateContact) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val success = contactRepository.updateContact(
                    contactId = event.contactId,
                    displayName = event.displayName,
                    phoneNumbers = event.phoneNumbers,
                    emails = event.emails,
                    organization = event.organization,
                    address = event.address,
                    note = event.note
                )

                if (success) {
                    _uiState.update { it.copy(successMessage = "Contact updated successfully") }
                    sendSideEffect(ContactSideEffect.ShowSuccess("Contact updated successfully"))
                    sendSideEffect(ContactSideEffect.NavigateBack)
                    refresh()
                } else {
                    throw Exception("Failed to update contact")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to update contact") }
                sendSideEffect(ContactSideEffect.ShowError("Failed to update contact"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun deleteContact(contactId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val success = contactRepository.deleteContact(contactId)
                if (success) {
                    _uiState.update { it.copy(successMessage = "Contact deleted successfully") }
                    sendSideEffect(ContactSideEffect.ShowSuccess("Contact deleted successfully"))
                    sendSideEffect(ContactSideEffect.ContactDeleted(contactId))
                    refresh()

                    // Remove from selection if selected
                    if (_uiState.value.selectedContacts.contains(contactId)) {
                        _uiState.update {
                            it.copy(selectedContacts = it.selectedContacts - contactId)
                        }
                    }
                } else {
                    throw Exception("Failed to delete contact")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete contact") }
                sendSideEffect(ContactSideEffect.ShowError("Failed to delete contact"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun deleteContacts(contactIds: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                var successCount = 0
                contactIds.forEach { contactId ->
                    if (contactRepository.deleteContact(contactId)) {
                        successCount++
                    }
                }

                if (successCount > 0) {
                    _uiState.update {
                        it.copy(
                            successMessage = "$successCount contacts deleted successfully",
                            isSelectionMode = false,
                            selectedContacts = emptySet()
                        )
                    }
                    sendSideEffect(ContactSideEffect.ShowSuccess("$successCount contacts deleted successfully"))
                    sendSideEffect(ContactSideEffect.ContactsDeleted(successCount))
                    refresh()
                } else {
                    throw Exception("Failed to delete contacts")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete contacts") }
                sendSideEffect(ContactSideEffect.ShowError("Failed to delete contacts"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun toggleFavorite(contactId: String, isStarred: Boolean) {
        viewModelScope.launch {
            try {
                val success = contactRepository.setContactStarred(contactId, isStarred)
                if (success) {
                    val message = if (isStarred) "Added to favorites" else "Removed from favorites"
                    sendSideEffect(ContactSideEffect.ShowToast(message))
                    sendSideEffect(ContactSideEffect.ContactStarredToggled(contactId, isStarred))
                    refresh()
                }
            } catch (e: Exception) {
                sendSideEffect(ContactSideEffect.ShowError("Failed to update favorite status"))
            }
        }
    }

    // ========== Selection Mode ==========

    private fun toggleSelectionMode(enabled: Boolean) {
        _uiState.update {
            it.copy(
                isSelectionMode = enabled,
                selectedContacts = if (!enabled) emptySet() else it.selectedContacts
            )
        }
    }

    private fun toggleContactSelection(contactId: String) {
        _uiState.update { state ->
            val selectedContacts = state.selectedContacts.toMutableSet()
            if (selectedContacts.contains(contactId)) {
                selectedContacts.remove(contactId)
            } else {
                selectedContacts.add(contactId)
            }
            state.copy(selectedContacts = selectedContacts)
        }
    }

    private fun selectAllContacts() {
        // This is a placeholder - actual implementation would need to load all contact IDs
        sendSideEffect(ContactSideEffect.ShowToast("Select all not available in paginated mode"))
    }

    private fun clearSelectedContacts() {
        _uiState.update { it.copy(selectedContacts = emptySet()) }
    }

    // ========== Group Operations ==========

    private fun loadContactsByGroup(groupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val group = _contactGroups.value.find { it.id == groupId }
                val groupName = group?.title ?: "Group"
                sendSideEffect(ContactSideEffect.NavigateToGroupContacts(groupId, groupName))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load group contacts") }
                sendSideEffect(ContactSideEffect.ShowError("Failed to load group contacts"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun loadContactGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingGroups = true) }
            try {
                val groups = contactRepository.getContactGroups()
                _contactGroups.value = groups
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load contact groups") }
                sendSideEffect(ContactSideEffect.ShowError("Failed to load contact groups"))
            } finally {
                _uiState.update { it.copy(isLoadingGroups = false) }
            }
        }
    }

    private fun loadGroupedContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val grouped = contactRepository.getContactsGrouped()
                _groupedContacts.value = grouped
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load grouped contacts") }
                sendSideEffect(ContactSideEffect.ShowError("Failed to load grouped contacts"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ========== Refresh and Retry ==========

    private fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadContactGroups()
        loadGroupedContacts()
        // Refresh paginated flows by updating config
        _pagingConfig.value = _pagingConfig.value.copy()
        _uiState.update { it.copy(isRefreshing = false) }
    }

    private fun retry() {
        clearError()
        refresh()
    }

    // ========== Error and Success Management ==========

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    // ========== Side Effects ==========

    private fun sendSideEffect(effect: ContactSideEffect) {
        viewModelScope.launch {
            _sideEffect.emit(effect)
        }
    }

    // ========== Convenience Methods ==========

    fun callContact(phoneNumber: String) {
        sendSideEffect(ContactSideEffect.NavigateToCall(phoneNumber))
    }

    fun smsContact(phoneNumber: String) {
        sendSideEffect(ContactSideEffect.NavigateToSms(phoneNumber))
    }

    fun emailContact(email: String) {
        sendSideEffect(ContactSideEffect.NavigateToEmail(email))
    }

    fun navigateToAddContact() {
        sendSideEffect(ContactSideEffect.NavigateToAddContact)
    }

    fun navigateToEditContact(contactId: String) {
        sendSideEffect(ContactSideEffect.NavigateToEditContact(contactId))
    }

    fun navigateBack() {
        sendSideEffect(ContactSideEffect.NavigateBack)
    }

    fun showDeleteConfirmation() {
        val selectedIds = _uiState.value.selectedContacts.toList()
        if (selectedIds.isNotEmpty()) {
            sendSideEffect(ContactSideEffect.ShowDeleteConfirmation(selectedIds, selectedIds.size))
        }
    }

    // ========== Helper Methods ==========

    private fun getSortDescription(sortBy: ContactSort): String {
        return when (sortBy) {
            ContactSort.NAME_ASC -> "Name (A-Z)"
            ContactSort.NAME_DESC -> "Name (Z-A)"
            ContactSort.RECENT_ASC -> "Recent (Oldest First)"
            ContactSort.RECENT_DESC -> "Recent (Newest First)"
        }
    }

    private fun getFilterDescription(filter: ContactFilter): String {
        return when (filter) {
            ContactFilter.ALL -> "All Contacts"
            ContactFilter.STARRED -> "Favorites Only"
        }
    }

}