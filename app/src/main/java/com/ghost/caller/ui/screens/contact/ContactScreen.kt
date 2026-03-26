package com.ghost.caller.ui.screens.contact

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.ghost.caller.models.ContactQuickInfo
import com.ghost.caller.ui.components.ConfirmDeleteDialog
import com.ghost.caller.ui.components.GroupsList
import com.ghost.caller.ui.screens.recent.CallLogSelectionBottomBar
import com.ghost.caller.ui.screens.recent.ErrorView
import com.ghost.caller.viewmodel.contact.ContactPagingList
import com.ghost.caller.viewmodel.contact.ContactSideEffect
import com.ghost.caller.viewmodel.contact.ContactTab
import com.ghost.caller.viewmodel.contact.ContactUiEvent
import com.ghost.caller.viewmodel.contact.ContactViewModel
import com.ghost.caller.viewmodel.contact.ViewMode
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactScreen(
    onNavigateToContactDetail: (contactId: String) -> Unit,
    onNavigateToAddContact: () -> Unit,
    onNavigateToEditContact: (String) -> Unit,
    onNavigateToCall: (String) -> Unit,
    onNavigateToSms: (String) -> Unit,
    onNavigateToEmail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    navigationBar: (@Composable () -> Unit)?,
    viewModel: ContactViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LocalContext.current

    val contactGroups by viewModel.contactGroupsWithUi.collectAsStateWithLifecycle()

    var isSortDialogVisible by remember { mutableStateOf(false) }
    var isFilterDialogVisible by remember { mutableStateOf(false) }
    var isDeleteDialogVisible by remember { mutableStateOf<List<String>>(emptyList()) }

    // Snackbar host state for displaying messages instead of Toasts
    val snackbarHostState = remember { SnackbarHostState() }

    val currentItems: LazyPagingItems<ContactQuickInfo> = when (uiState.currentTab) {
        ContactTab.CONTACTS -> viewModel.contactsPagingFlow.collectAsLazyPagingItems()
        ContactTab.FAVORITES -> viewModel.favoritesPagingFlow.collectAsLazyPagingItems()
        ContactTab.RECENT -> viewModel.recentContactsPagingFlow.collectAsLazyPagingItems()
        ContactTab.SEARCH -> viewModel.searchResultsPagingFlow.collectAsLazyPagingItems()
        ContactTab.GROUPS -> viewModel.contactsPagingFlow.collectAsLazyPagingItems() // Fallback, handled uniquely below
    }

    // Handle side effects
    LaunchedEffect(Unit) {
        // Use 'collect' instead of 'collectLatest' so we don't drop quick sequential side effects
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is ContactSideEffect.NavigateToContactDetail -> {
                    Timber.d("SideEffect: NavigateToContactDetail (id: ${effect.contactId})")
                    onNavigateToContactDetail(effect.contactId)
                }

                is ContactSideEffect.NavigateToAddContact -> {
                    Timber.d("SideEffect: NavigateToAddContact")
                    onNavigateToAddContact()
                }

                is ContactSideEffect.NavigateToEditContact -> {
                    Timber.d("SideEffect: NavigateToEditContact (id: ${effect.contactId})")
                    onNavigateToEditContact(effect.contactId)
                }

                is ContactSideEffect.NavigateToCall -> {
                    Timber.d("SideEffect: NavigateToCall (number: ${effect.phoneNumber})")
                    onNavigateToCall(effect.phoneNumber)
                }

                is ContactSideEffect.NavigateToSms -> {
                    Timber.d("SideEffect: NavigateToSms (number: ${effect.phoneNumber})")
                    onNavigateToSms(effect.phoneNumber)
                }

                is ContactSideEffect.NavigateToEmail -> {
                    Timber.d("SideEffect: NavigateToEmail (email: ${effect.email})")
                    onNavigateToEmail(effect.email)
                }

                is ContactSideEffect.NavigateToGroupContacts -> {
                    Timber.d("SideEffect: NavigateToGroupContacts (groupId: ${effect.groupId})")
                    onNavigateToContactDetail(effect.groupId)
                }

                is ContactSideEffect.ShowToast -> {
                    Timber.d("SideEffect: ShowToast - ${effect.message}")
                    launch { snackbarHostState.showSnackbar(effect.message) }
                }

                is ContactSideEffect.ShowError -> {
                    Timber.e("SideEffect: ShowError - ${effect.message}")
                    launch { snackbarHostState.showSnackbar(effect.message) }
                }

                is ContactSideEffect.ShowSuccess -> {
                    Timber.d("SideEffect: ShowSuccess - ${effect.message}")
                    launch { snackbarHostState.showSnackbar(effect.message) }
                }

                is ContactSideEffect.ShowDeleteConfirmation -> {
                    Timber.d("SideEffect: ShowDeleteConfirmation (${effect.count} items)")
                    if (effect.count > 0) {
                        isDeleteDialogVisible = effect.contactIds
                    }
                }

                is ContactSideEffect.ContactDeleted,
                is ContactSideEffect.ContactsDeleted,
                is ContactSideEffect.ContactAdded,
                is ContactSideEffect.ContactUpdated,
                is ContactSideEffect.ContactStarredToggled -> {
                    Timber.d("SideEffect: Data mutation occurred, refreshing list")
                    currentItems.refresh()
                }

                is ContactSideEffect.NavigateBack -> {
                    Timber.d("SideEffect: NavigateBack")
                    onNavigateBack()
                }
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = currentItems.loadState.refresh is LoadState.Loading,
        onRefresh = {
            Timber.d("User triggered manual refresh")
            currentItems.refresh()
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        Timber.d("Sort dialog opened")
                        isSortDialogVisible = true
                    },
                    onFilterClick = {
                        Timber.d("Filter dialog opened")
                        isFilterDialogVisible = true
                    },
                    onViewModeToggle = {
                        val newMode =
                            if (uiState.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                        Timber.d("Toggled view mode to $newMode")
                        viewModel.sendEvent(ContactUiEvent.ChangeViewMode(newMode))
                    },
                    currentViewMode = uiState.viewMode
                )
            },
            bottomBar = {
                if (uiState.isSelectionMode) {
                    CallLogSelectionBottomBar(
                        selectedCount = uiState.selectedContacts.size,
                        onCancel = {
                            Timber.d("Selection mode cancelled")
                            viewModel.sendEvent(ContactUiEvent.ToggleSelectionMode(false))
                        },
                        onDelete = {
                            Timber.d("Delete clicked in selection mode")
                            viewModel.showDeleteConfirmation()
                        },
                        onSelectAll = {
                            Timber.d("Select all clicked in selection mode")
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
                        onClick = {
                            Timber.d("FAB clicked to add new contact")
                            viewModel.navigateToAddContact()
                        }
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
                            onRetry = {
                                Timber.d("Retry clicked after error")
                                viewModel.sendEvent(ContactUiEvent.Retry)
                            }
                        )
                    }

                    else -> {
                        ContactTabs(
                            currentTab = uiState.currentTab,
                            onTabSelected = { tab ->
                                Timber.d("Tab changed to $tab")
                                viewModel.sendEvent(ContactUiEvent.ChangeTab(tab))
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            when (uiState.currentTab) {
                                ContactTab.CONTACTS -> {
                                    ContactListOrEmpty(
                                        pagingItems = currentItems,
                                        emptyMessageIcon = Icons.Filled.Contacts,
                                        emptyMessage = if (uiState.searchQuery.isNotEmpty())
                                            "No contacts found for '${uiState.searchQuery}'"
                                        else
                                            "No contacts yet",
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
                                                        ContactUiEvent.SelectContact(
                                                            it
                                                        )
                                                    )
                                                }
                                            )
                                        },
                                        onContactLongClick = { contact ->
                                            if (!uiState.isSelectionMode) {
                                                Timber.d("Long click on contact (id: ${contact.id}), entering selection mode")
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
                                            Timber.d("Favorite toggled for contact (id: ${contact.id}), new state: ${!isStarred}")
                                            viewModel.sendEvent(
                                                ContactUiEvent.ToggleFavorite(
                                                    contact.id,
                                                    !isStarred
                                                )
                                            )
                                        },
                                        onCallClick = { phoneNumber ->
                                            Timber.d("Quick call clicked for: $phoneNumber")
                                            viewModel.callContact(phoneNumber)
                                        },
                                        onAddClick = { viewModel.navigateToAddContact() },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                ContactTab.FAVORITES -> {
                                    ContactListOrEmpty(
                                        pagingItems = currentItems,
                                        emptyMessageIcon = Icons.Filled.Favorite,
                                        emptyMessage = "No favorites yet",
                                        viewMode = uiState.viewMode,
                                        onContactClick = { contact ->
                                            viewModel.sendEvent(ContactUiEvent.SelectContact(contact.id))
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
                                        onAddClick = { viewModel.navigateToAddContact() },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                ContactTab.RECENT -> {
                                    ContactListOrEmpty(
                                        pagingItems = currentItems,
                                        emptyMessageIcon = Icons.Filled.History,
                                        emptyMessage = "No recent contacts",
                                        viewMode = uiState.viewMode,
                                        onContactClick = { contact ->
                                            viewModel.sendEvent(ContactUiEvent.SelectContact(contact.id))
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
                                        onAddClick = { viewModel.navigateToAddContact() },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                ContactTab.GROUPS -> {
                                    if (contactGroups.isEmpty()) {
                                        EmptyGroupsView()
                                    } else {
                                        GroupsList(
                                            groups = contactGroups,
                                            onGroupClick = { group ->
                                                Timber.d("Group clicked (id: ${group.id})")
                                                viewModel.sendEvent(
                                                    ContactUiEvent.LoadContactsByGroup(
                                                        group.id
                                                    )
                                                )
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                ContactTab.SEARCH -> {
                                    ContactListOrEmpty(
                                        pagingItems = currentItems,
                                        emptyMessageIcon = Icons.Filled.BrokenImage,
                                        emptyMessage = "No results for '${uiState.searchQuery}'",
                                        viewMode = uiState.viewMode,
                                        onContactClick = { contact ->
                                            viewModel.sendEvent(ContactUiEvent.SelectContact(contact.id))
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
                                        onAddClick = { viewModel.navigateToAddContact() },
                                        modifier = Modifier.fillMaxSize()
                                    )
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
                Timber.d("Sort applied: $it")
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
                Timber.d("Filter applied: $it")
                viewModel.sendEvent(ContactUiEvent.ChangeFilter(it))
            }
        )
    }

    if (isDeleteDialogVisible.isNotEmpty()) {
        ConfirmDeleteDialog(
            title = "Delete Contact",
            message = "Are you sure you want to delete ${if (isDeleteDialogVisible.size > 1) "these contacts" else "this contact"}?",
            onDismiss = { isDeleteDialogVisible = emptyList() },
            onConfirm = {
                Timber.d("Confirmed deletion of ${isDeleteDialogVisible.size} contacts")
                viewModel.sendEvent(ContactUiEvent.DeleteContacts(isDeleteDialogVisible))
                isDeleteDialogVisible = emptyList()
            }
        )
    }
}

@Composable
private fun ContactListOrEmpty(
    pagingItems: LazyPagingItems<ContactQuickInfo>,
    emptyMessageIcon: ImageVector,
    emptyMessage: String,
    viewMode: ViewMode,
    isSelectionMode: Boolean = false,
    selectedContacts: Set<String> = emptySet(),
    onContactClick: (ContactQuickInfo) -> Unit,
    onContactLongClick: (ContactQuickInfo) -> Unit = {},
    onFavoriteClick: (ContactQuickInfo, Boolean) -> Unit,
    onCallClick: (String) -> Unit,
    onAddClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (pagingItems.itemCount == 0 && pagingItems.loadState.refresh !is LoadState.Loading) {
        if (onAddClick != null) {
            EmptyContactsView(
                message = emptyMessage,
                icon = emptyMessageIcon,
                onAddClick = onAddClick
            )
        } else {
            Text(
                text = emptyMessage,
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            )
        }
    } else {
        ContactPagingList(
            pagingItems = pagingItems,
            viewMode = viewMode,
            isSelectionMode = isSelectionMode,
            selectedContacts = selectedContacts,
            onContactClick = onContactClick,
            onContactLongClick = onContactLongClick,
            onFavoriteClick = onFavoriteClick,
            onCallClick = onCallClick,
            modifier = modifier
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