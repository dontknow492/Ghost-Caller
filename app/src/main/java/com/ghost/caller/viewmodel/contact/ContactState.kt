// ContactState.kt
package com.ghost.caller.viewmodel.contact

import com.ghost.caller.models.AddressData
import com.ghost.caller.models.ContactGroup
import com.ghost.caller.models.ContactQuickInfo
import com.ghost.caller.models.ContactWithDetails
import com.ghost.caller.models.EmailData
import com.ghost.caller.models.OrganizationData
import com.ghost.caller.models.PhoneNumberData
import com.ghost.caller.repository.ContactFilter
import com.ghost.caller.repository.ContactSort

/**
 * Contact UI State - For non-paginated, immediate UI state
 */
data class ContactUiState(
    // UI State
    val currentTab: ContactTab = ContactTab.CONTACTS,
    val viewMode: ViewMode = ViewMode.LIST,
    val searchQuery: String = "",
    val isSelectionMode: Boolean = false,
    val selectedContacts: Set<String> = emptySet(),

    // Loading States
    val isLoading: Boolean = false,
    val isLoadingGroups: Boolean = false,
    val isRefreshing: Boolean = false,

    // Error and Success
    val error: String? = null,
    val successMessage: String? = null,

    // Selected Contact (for detail view)
    val selectedContact: ContactWithDetails? = null,

    // Groups (non-paginated, usually small dataset)
    val contactGroups: List<ContactGroup> = emptyList(),
    val groupedContacts: Map<String, List<ContactQuickInfo>> = emptyMap(),

    // Search
    val isSearchActive: Boolean = false,
    val searchResultsCount: Int = 0,

    // filter
    val currentFilter: ContactFilter = ContactFilter.ALL,
    val contactSort: ContactSort = ContactSort.NAME_ASC,


    )

/**
 * Contact UI Events (User Actions)
 */
sealed class ContactUiEvent {
    // Navigation and Tab Management
    data class ChangeTab(val tab: ContactTab) : ContactUiEvent()
    data class ChangeViewMode(val viewMode: ViewMode) : ContactUiEvent()
    data class ChangeSortOrder(val sortBy: ContactSort) : ContactUiEvent()
    data class ChangeFilter(val filter: ContactFilter) : ContactUiEvent()

    // Search
    data class SearchContacts(val query: String) : ContactUiEvent()
    object ClearSearch : ContactUiEvent()

    // Contact Selection
    data class SelectContact(val contactId: String) : ContactUiEvent()
    data class LoadContactDetails(val contactId: String) : ContactUiEvent()

    // CRUD Operations
    data class AddContact(
        val displayName: String,
        val phoneNumbers: List<PhoneNumberData> = emptyList(),
        val emails: List<EmailData> = emptyList(),
        val organization: OrganizationData? = null,
        val address: AddressData? = null,
        val note: String? = null,
        val photoUri: android.net.Uri? = null
    ) : ContactUiEvent()

    data class UpdateContact(
        val contactId: String,
        val displayName: String? = null,
        val phoneNumbers: List<PhoneNumberData>? = null,
        val emails: List<EmailData>? = null,
        val organization: OrganizationData? = null,
        val address: AddressData? = null,
        val note: String? = null,
        val photoUri: android.net.Uri? = null
    ) : ContactUiEvent()

    data class DeleteContact(val contactId: String) : ContactUiEvent()
    data class DeleteContacts(val contactIds: List<String>) : ContactUiEvent()
    data class ToggleFavorite(val contactId: String, val isStarred: Boolean) : ContactUiEvent()

    // Selection Mode
    data class ToggleSelectionMode(val enabled: Boolean) : ContactUiEvent()
    data class ToggleContactSelection(val contactId: String) : ContactUiEvent()
    data object SelectAllContacts : ContactUiEvent()
    data object ClearSelectedContacts : ContactUiEvent()

    // Groups
    data class LoadContactsByGroup(val groupId: String) : ContactUiEvent()
    data object LoadContactGroups : ContactUiEvent()
    data object LoadGroupedContacts : ContactUiEvent()

    // System
    data object Refresh : ContactUiEvent()
    data object Retry : ContactUiEvent()
    data object ClearError : ContactUiEvent()
    data object ClearSuccessMessage : ContactUiEvent()
}

/**
 * Contact Side Effects (One-time events)
 */
sealed class ContactSideEffect {
    // Navigation
    data class NavigateToContactDetail(val contactId: String) : ContactSideEffect()
    data object NavigateToAddContact : ContactSideEffect()
    data class NavigateToEditContact(val contactId: String) : ContactSideEffect()
    data class NavigateToGroupContacts(val groupId: String, val groupName: String) :
        ContactSideEffect()

    data object NavigateBack : ContactSideEffect()

    // Communication
    data class NavigateToCall(val phoneNumber: String) : ContactSideEffect()
    data class NavigateToSms(val phoneNumber: String) : ContactSideEffect()
    data class NavigateToEmail(val email: String) : ContactSideEffect()

    // User Feedback
    data class ShowToast(val message: String) : ContactSideEffect()
    data class ShowError(val message: String) : ContactSideEffect()
    data class ShowDeleteConfirmation(val contactIds: List<String>, val count: Int) :
        ContactSideEffect()

    data class ShowSuccess(val message: String) : ContactSideEffect()

    // Events
    data class ContactDeleted(val contactId: String) : ContactSideEffect()
    data class ContactsDeleted(val count: Int) : ContactSideEffect()
    data class ContactAdded(val contactId: String) : ContactSideEffect()
    data class ContactUpdated(val contactId: String) : ContactSideEffect()
    data class ContactStarredToggled(val contactId: String, val isStarred: Boolean) :
        ContactSideEffect()
}

/**
 * Enums
 */
enum class ViewMode {
    LIST, GRID
}

enum class ContactTab {
    CONTACTS, FAVORITES, RECENT, GROUPS, SEARCH
}


/**
 * Pagination Configuration
 */
data class ContactsPagingConfig(
    val sortBy: ContactSort = ContactSort.NAME_ASC,
    val filter: ContactFilter = ContactFilter.ALL
)

/**
 * Contact Quick Info with additional display data
 */
data class ContactQuickInfoWithDisplay(
    val contact: ContactQuickInfo,
    val formattedNumber: String = "",
    val initials: String = "",
    val displayName: String = "",
    val primaryPhoneFormatted: String = ""
) {
    companion object {
        fun fromContact(contact: ContactQuickInfo): ContactQuickInfoWithDisplay {
            return ContactQuickInfoWithDisplay(
                contact = contact,
                formattedNumber = formatPhoneNumber(contact.primaryPhoneNumber ?: ""),
                initials = generateInitials(contact.displayName),
                displayName = contact.displayName,
                primaryPhoneFormatted = formatPhoneNumber(contact.primaryPhoneNumber ?: "")
            )
        }

        private fun formatPhoneNumber(number: String): String {
            val cleaned = number.replace(Regex("[^\\d+]"), "")
            return when {
                cleaned.length == 10 -> "${cleaned.substring(0, 3)}-${
                    cleaned.substring(
                        3,
                        6
                    )
                }-${cleaned.substring(6)}"

                cleaned.length == 11 && cleaned.startsWith("1") ->
                    "+1 ${cleaned.substring(1, 4)}-${
                        cleaned.substring(
                            4,
                            7
                        )
                    }-${cleaned.substring(7)}"

                else -> number
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
}

/**
 * Contact Group with additional UI data
 */
data class ContactGroupWithUi(
    val group: ContactGroup,
    val iconRes: Int = android.R.drawable.ic_menu_gallery,
    val formattedCount: String = "${group.count} contact${if (group.count != 1) "s" else ""}"
)

/**
 * Search State
 */
data class SearchState(
    val query: String = "",
    val isActive: Boolean = false,
    val resultsCount: Int = 0,
    val suggestions: List<ContactSuggestion> = emptyList()
)

/**
 * Contact Suggestion for search
 */
data class ContactSuggestion(
    val id: String,
    val name: String,
    val number: String,
    val photoUri: android.net.Uri? = null,
    val contactType: String? = null
)

/**
 * Selection State
 */
data class SelectionState(
    val isActive: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val selectedCount: Int = 0
) {
    fun isSelected(contactId: String): Boolean = selectedIds.contains(contactId)
    fun hasSelection(): Boolean = selectedIds.isNotEmpty()
}

/**
 * Loading States
 */
data class LoadingState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingGroups: Boolean = false
)

/**
 * Error State
 */
data class ErrorState(
    val message: String? = null,
    val isCritical: Boolean = false,
    val retryAction: (() -> Unit)? = null
)

/**
 * Combined UI State for Compose
 */
data class ContactScreenUiState(
    val uiState: ContactUiState,
    val selectionState: SelectionState,
    val loadingState: LoadingState,
    val errorState: ErrorState,
    val searchState: SearchState,
    val currentTab: ContactTab,
    val viewMode: ViewMode
) {
    companion object {
        fun fromContactUiState(state: ContactUiState): ContactScreenUiState {
            return ContactScreenUiState(
                uiState = state,
                selectionState = SelectionState(
                    isActive = state.isSelectionMode,
                    selectedIds = state.selectedContacts,
                    selectedCount = state.selectedContacts.size
                ),
                loadingState = LoadingState(
                    isLoading = state.isLoading,
                    isLoadingGroups = state.isLoadingGroups,
                    isRefreshing = state.isRefreshing
                ),
                errorState = ErrorState(message = state.error),
                searchState = SearchState(
                    query = state.searchQuery,
                    isActive = state.isSearchActive,
                    resultsCount = state.searchResultsCount
                ),
                currentTab = state.currentTab,
                viewMode = state.viewMode
            )
        }
    }
}