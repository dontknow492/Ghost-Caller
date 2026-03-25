// ContactScreen.kt
package com.ghost.caller.ui.screens.contact

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.ghost.caller.models.ContactQuickInfo
import com.ghost.caller.ui.components.ConfirmDeleteDialog
import com.ghost.caller.ui.components.contact.ContactTabs
import com.ghost.caller.ui.components.contact.ContactTopBar
import com.ghost.caller.ui.components.contact.EmptyContactsView
import com.ghost.caller.ui.components.contact.EmptyFavoritesView
import com.ghost.caller.ui.components.contact.EmptyGroupsView
import com.ghost.caller.ui.components.contact.EmptyRecentView
import com.ghost.caller.ui.components.contact.EmptySearchView
import com.ghost.caller.ui.components.contact.GroupsList
import com.ghost.caller.ui.screens.recent.CallLogSelectionBottomBar
import com.ghost.caller.ui.screens.recent.ErrorView
import com.ghost.caller.viewmodel.contact.ContactPagingList
import com.ghost.caller.viewmodel.contact.ContactSideEffect
import com.ghost.caller.viewmodel.contact.ContactTab
import com.ghost.caller.viewmodel.contact.ContactUiEvent
import com.ghost.caller.viewmodel.contact.ContactViewModel
import com.ghost.caller.viewmodel.contact.ViewMode
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactScreen(
    onNavigateToContactDetail: (String) -> Unit,
    onNavigateToAddContact: () -> Unit,
    onNavigateToEditContact: (String) -> Unit,
    onNavigateToCall: (String) -> Unit,
    onNavigateToSms: (String) -> Unit,
    onNavigateToEmail: (String) -> Unit,
    navigationBar: (@Composable () -> Unit)?,
    viewModel: ContactViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Collect paginated data
    val contactsPagingItems = viewModel.contactsPagingFlow.collectAsLazyPagingItems()
    val favoritesPagingItems = viewModel.favoritesPagingFlow.collectAsLazyPagingItems()
    val recentContactsPagingItems = viewModel.recentContactsPagingFlow.collectAsLazyPagingItems()
    val searchResultsPagingItems = viewModel.searchResultsPagingFlow.collectAsLazyPagingItems()

    val contactGroups by viewModel.contactGroupsWithUi.collectAsStateWithLifecycle()

    var isSortDialogVisible by remember { mutableStateOf(false) }
    var isFilterDialogVisible by remember { mutableStateOf(false) }
    var isDeleteDialogVisible by remember { mutableStateOf<List<String>>(emptyList()) }


    val currentItems = when (uiState.currentTab) {
        ContactTab.CONTACTS -> contactsPagingItems
        ContactTab.FAVORITES -> favoritesPagingItems
        ContactTab.RECENT -> recentContactsPagingItems
        ContactTab.SEARCH -> contactsPagingItems // Handled by pagedContacts internally
        ContactTab.GROUPS -> contactsPagingItems // Handled by pagedContacts internally
    }


    // Handle side effects
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collectLatest { effect ->
            when (effect) {
                is ContactSideEffect.NavigateToContactDetail -> {
                    onNavigateToContactDetail(effect.contactId)
                }

                is ContactSideEffect.NavigateToAddContact -> {
                    onNavigateToAddContact()
                }

                is ContactSideEffect.NavigateToEditContact -> {
                    onNavigateToEditContact(effect.contactId)
                }

                is ContactSideEffect.NavigateToCall -> {
                    onNavigateToCall(effect.phoneNumber)
                }

                is ContactSideEffect.NavigateToSms -> {
                    onNavigateToSms(effect.phoneNumber)
                }

                is ContactSideEffect.NavigateToEmail -> {
                    onNavigateToEmail(effect.email)
                }

                is ContactSideEffect.NavigateToGroupContacts -> {
                    // Navigate to group contacts view
                    onNavigateToContactDetail(effect.groupId)
                }

                is ContactSideEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                is ContactSideEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }

                is ContactSideEffect.ShowSuccess -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                is ContactSideEffect.ShowDeleteConfirmation -> {
                    // Show delete confirmation dialog
                    if (effect.count > 0) {
                        isDeleteDialogVisible = effect.contactIds
                    }
                }

                is ContactSideEffect.ContactDeleted,
                is ContactSideEffect.ContactsDeleted,
                is ContactSideEffect.ContactAdded,
                is ContactSideEffect.ContactUpdated,
                is ContactSideEffect.ContactStarredToggled -> {
                    // Handled by UI state updates
                }

                is ContactSideEffect.NavigateBack -> {
                    // Handle navigation back if needed
                }
            }
        }
    }


    PullToRefreshBox(
        isRefreshing = currentItems.loadState.refresh is LoadState.Loading,
        onRefresh = { currentItems.refresh() },
    ) {
        Scaffold(
            topBar = {
                ContactTopBar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { query ->
                        if (query.isNotEmpty()) {
                            viewModel.sendEvent(ContactUiEvent.SearchContacts(query))
                        } else {
                            viewModel.sendEvent(ContactUiEvent.ClearSearch)
                        }
                    },
                    onSortClick = {
                        // Show sort options menu
                        isSortDialogVisible = true
                    },
                    onFilterClick = {
                        // Show filter options menu
                        isFilterDialogVisible = true
                    },
                    onViewModeToggle = {
                        viewModel.sendEvent(
                            ContactUiEvent.ChangeViewMode(
                                if (uiState.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                            )
                        )
                    },
                    currentViewMode = uiState.viewMode
                )
            },
            bottomBar = {
                if (uiState.isSelectionMode) {
                    CallLogSelectionBottomBar(
                        selectedCount = uiState.selectedContacts.size,
                        onCancel = {
                            viewModel.sendEvent(ContactUiEvent.ToggleSelectionMode(false))
                        },
                        onDelete = {
                            viewModel.showDeleteConfirmation()
                        },
                        onSelectAll = {
                            viewModel.sendEvent(ContactUiEvent.SelectAllContacts)
                        }
                    )
                } else {
                    navigationBar?.invoke()
                }
            },
            floatingActionButton = {
                if (!uiState.isSelectionMode) {
                    FloatingActionButton(
                        onClick = { viewModel.navigateToAddContact() }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Contact")
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    uiState.isLoading && currentItems.itemCount == 0 -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    uiState.error != null && currentItems.itemCount == 0 -> {
                        ErrorView(
                            message = uiState.error ?: "Failed to load contacts",
                            onRetry = { viewModel.sendEvent(ContactUiEvent.Retry) }
                        )
                    }

                    else -> {
                        ContactTabs(
                            currentTab = uiState.currentTab,
                            onTabSelected = { tab ->
                                viewModel.sendEvent(ContactUiEvent.ChangeTab(tab))
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            when (uiState.currentTab) {
                                ContactTab.CONTACTS -> {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        // Search results
                                        if (searchResultsPagingItems.itemCount == 0 && searchResultsPagingItems.loadState.refresh !is LoadState.Loading) {
                                            EmptyContactsView(
                                                message = "No contacts found for '${uiState.searchQuery}'",
                                                onAddClick = { viewModel.navigateToAddContact() }
                                            )
                                        } else {
                                            ContactPagingList(
                                                pagingItems = searchResultsPagingItems,
                                                viewMode = uiState.viewMode,
                                                isSelectionMode = uiState.isSelectionMode,
                                                selectedContacts = uiState.selectedContacts,
                                                onContactClick = { contact ->
                                                    handleContactClick(
                                                        contact = contact,
                                                        isSelectionMode = uiState.isSelectionMode,
                                                        onSelect = {
                                                            viewModel.sendEvent(
                                                                ContactUiEvent.ToggleContactSelection(
                                                                    it
                                                                )
                                                            )
                                                        },
                                                        onNavigate = {
                                                            viewModel.sendEvent(
                                                                ContactUiEvent.SelectContact(it)
                                                            )
                                                        }
                                                    )
                                                },
                                                onContactLongClick = { contact ->
                                                    if (!uiState.isSelectionMode) {
                                                        viewModel.sendEvent(
                                                            ContactUiEvent.ToggleSelectionMode(
                                                                true
                                                            )
                                                        )
                                                        viewModel.sendEvent(
                                                            ContactUiEvent.ToggleContactSelection(
                                                                contact.id
                                                            )
                                                        )
                                                    }
                                                },
                                                onFavoriteClick = { contact, isStarred ->
                                                    viewModel.sendEvent(
                                                        ContactUiEvent.ToggleFavorite(
                                                            contact.id,
                                                            !isStarred
                                                        )
                                                    )
                                                },
                                                onCallClick = { phoneNumber ->
                                                    viewModel.callContact(phoneNumber)
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    } else {
                                        // All contacts
                                        if (contactsPagingItems.itemCount == 0 && contactsPagingItems.loadState.refresh !is LoadState.Loading) {
                                            EmptyContactsView(
                                                message = "No contacts yet",
                                                onAddClick = { viewModel.navigateToAddContact() }
                                            )
                                        } else {
                                            ContactPagingList(
                                                pagingItems = contactsPagingItems,
                                                viewMode = uiState.viewMode,
                                                isSelectionMode = uiState.isSelectionMode,
                                                selectedContacts = uiState.selectedContacts,
                                                onContactClick = { contact ->
                                                    handleContactClick(
                                                        contact = contact,
                                                        isSelectionMode = uiState.isSelectionMode,
                                                        onSelect = {
                                                            viewModel.sendEvent(
                                                                ContactUiEvent.ToggleContactSelection(
                                                                    it
                                                                )
                                                            )
                                                        },
                                                        onNavigate = {
                                                            viewModel.sendEvent(
                                                                ContactUiEvent.SelectContact(it)
                                                            )
                                                        }
                                                    )
                                                },
                                                onContactLongClick = { contact ->
                                                    if (!uiState.isSelectionMode) {
                                                        viewModel.sendEvent(
                                                            ContactUiEvent.ToggleSelectionMode(
                                                                true
                                                            )
                                                        )
                                                        viewModel.sendEvent(
                                                            ContactUiEvent.ToggleContactSelection(
                                                                contact.id
                                                            )
                                                        )
                                                    }
                                                },
                                                onFavoriteClick = { contact, isStarred ->
                                                    viewModel.sendEvent(
                                                        ContactUiEvent.ToggleFavorite(
                                                            contact.id,
                                                            !isStarred
                                                        )
                                                    )
                                                },
                                                onCallClick = { phoneNumber ->
                                                    viewModel.callContact(phoneNumber)
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }

                                ContactTab.FAVORITES -> {
                                    if (favoritesPagingItems.itemCount == 0 && favoritesPagingItems.loadState.refresh !is LoadState.Loading) {
                                        EmptyFavoritesView(
                                            onAddClick = { viewModel.navigateToAddContact() }
                                        )
                                    } else {
                                        ContactPagingList(
                                            pagingItems = favoritesPagingItems,
                                            viewMode = uiState.viewMode,
                                            isSelectionMode = false,
                                            selectedContacts = emptySet(),
                                            onContactClick = { contact ->
                                                viewModel.sendEvent(
                                                    ContactUiEvent.SelectContact(
                                                        contact.id
                                                    )
                                                )
                                            },
                                            onFavoriteClick = { contact, isStarred ->
                                                viewModel.sendEvent(
                                                    ContactUiEvent.ToggleFavorite(
                                                        contact.id,
                                                        !isStarred
                                                    )
                                                )
                                            },
                                            onCallClick = { phoneNumber ->
                                                viewModel.callContact(phoneNumber)
                                            },
                                            onContactLongClick = { contact ->
                                                if (!uiState.isSelectionMode) {
                                                    viewModel.sendEvent(
                                                        ContactUiEvent.ToggleSelectionMode(
                                                            true
                                                        )
                                                    )
                                                    viewModel.sendEvent(
                                                        ContactUiEvent.ToggleContactSelection(
                                                            contact.id
                                                        )
                                                    )
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                ContactTab.RECENT -> {
                                    if (recentContactsPagingItems.itemCount == 0 && recentContactsPagingItems.loadState.refresh !is LoadState.Loading) {
                                        EmptyRecentView()
                                    } else {
                                        ContactPagingList(
                                            pagingItems = recentContactsPagingItems,
                                            viewMode = uiState.viewMode,
                                            isSelectionMode = false,
                                            selectedContacts = emptySet(),
                                            onContactClick = { contact ->
                                                viewModel.sendEvent(
                                                    ContactUiEvent.SelectContact(
                                                        contact.id
                                                    )
                                                )
                                            },
                                            onFavoriteClick = { contact, isStarred ->
                                                viewModel.sendEvent(
                                                    ContactUiEvent.ToggleFavorite(
                                                        contact.id,
                                                        !isStarred
                                                    )
                                                )
                                            },
                                            onCallClick = { phoneNumber ->
                                                viewModel.callContact(phoneNumber)
                                            },
                                            modifier = Modifier.fillMaxSize(),
                                            onContactLongClick = { contact ->
                                                if (!uiState.isSelectionMode) {
                                                    viewModel.sendEvent(
                                                        ContactUiEvent.ToggleSelectionMode(
                                                            true
                                                        )
                                                    )
                                                    viewModel.sendEvent(
                                                        ContactUiEvent.ToggleContactSelection(
                                                            contact.id
                                                        )
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }

                                ContactTab.GROUPS -> {
                                    if (contactGroups.isEmpty()) {
                                        EmptyGroupsView()
                                    } else {
                                        GroupsList(
                                            groups = contactGroups,
                                            onGroupClick = { group ->
                                                viewModel.sendEvent(
                                                    ContactUiEvent.LoadContactsByGroup(group.id)
                                                )
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                ContactTab.SEARCH -> {
                                    // Handled in CONTACTS tab with search query
                                    if (searchResultsPagingItems.itemCount == 0 && searchResultsPagingItems.loadState.refresh !is LoadState.Loading) {
                                        EmptySearchView(uiState.searchQuery)
                                    } else {
                                        ContactPagingList(
                                            pagingItems = searchResultsPagingItems,
                                            viewMode = uiState.viewMode,
                                            isSelectionMode = false,
                                            selectedContacts = emptySet(),
                                            onContactClick = { contact ->
                                                viewModel.sendEvent(
                                                    ContactUiEvent.SelectContact(
                                                        contact.id
                                                    )
                                                )
                                            },
                                            onFavoriteClick = { contact, isStarred ->
                                                viewModel.sendEvent(
                                                    ContactUiEvent.ToggleFavorite(
                                                        contact.id,
                                                        !isStarred
                                                    )
                                                )
                                            },
                                            onCallClick = { phoneNumber ->
                                                viewModel.callContact(phoneNumber)
                                            },
                                            modifier = Modifier.fillMaxSize(),
                                            onContactLongClick = { contact ->
                                                if (!uiState.isSelectionMode) {
                                                    viewModel.sendEvent(
                                                        ContactUiEvent.ToggleSelectionMode(
                                                            true
                                                        )
                                                    )
                                                    viewModel.sendEvent(
                                                        ContactUiEvent.ToggleContactSelection(
                                                            contact.id
                                                        )
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }




    if (isSortDialogVisible) {
        ContactSortDialog(
            currentSort = uiState.contactSort,
            onDismiss = { isSortDialogVisible = false },
            onApply = {
                isSortDialogVisible = false
                viewModel.sendEvent(ContactUiEvent.ChangeSortOrder(it))
            }
        )
    }
    if (isFilterDialogVisible) {
        ContactFilterDialog(
            currentFilter = uiState.currentFilter,
            onDismiss = { isFilterDialogVisible = false },
            onApply = {
                isFilterDialogVisible = false
                viewModel.sendEvent(ContactUiEvent.ChangeFilter(it))
            }
        )
    }
    if (isDeleteDialogVisible.isNotEmpty()) {
        ConfirmDeleteDialog(
            title = "Delete Contact",
            message = "Are you sure you want to delete this contact?",
            onDismiss = { isDeleteDialogVisible = emptyList() },
            onConfirm = {
                viewModel.sendEvent(ContactUiEvent.DeleteContacts(isDeleteDialogVisible))
                isDeleteDialogVisible = emptyList()
            }
        )
    }


}


private fun handleContactClick(
    contact: ContactQuickInfo,
    isSelectionMode: Boolean,
    onSelect: (String) -> Unit,
    onNavigate: (String) -> Unit
) {
    if (isSelectionMode) {
        onSelect(contact.id)
    } else {
        onNavigate(contact.id)
    }
}

