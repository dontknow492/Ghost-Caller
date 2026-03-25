@file:Suppress("D")

package com.ghost.caller.ui.components.contact


import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ghost.caller.models.ContactGroup
import com.ghost.caller.models.ContactQuickInfo
import com.ghost.caller.models.ContactType
import com.ghost.caller.viewmodel.contact.ContactGroupWithUi
import com.ghost.caller.viewmodel.contact.ContactTab
import com.ghost.caller.viewmodel.contact.ViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSortClick: () -> Unit,
    onFilterClick: () -> Unit,
    onViewModeToggle: () -> Unit,
    currentViewMode: ViewMode,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        TopAppBar(
            title = {
                Text("Contacts")
            },
            actions = {

                IconButton(onClick = onViewModeToggle) {
                    Icon(
                        if (currentViewMode == ViewMode.LIST) Icons.Default.GridView else Icons.AutoMirrored.Filled.List,
                        contentDescription = "View Mode"
                    )
                }
                IconButton(onClick = onSortClick) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                }
                IconButton(onClick = onFilterClick) {
                    Icon(Icons.Default.Filter, contentDescription = "Filter")
                }
            }
        )

        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onSearch = { onSearchQueryChange(it) },
            active = searchQuery.isNotEmpty(),
            onActiveChange = {},
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {

    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth(),
        placeholder = { Text("Search contacts...") },
        leadingIcon = {
            IconButton(
                onClick = {
                    onActiveChange(true)
                    onSearch(query)
                }
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
            }

        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun ContactTabs(
    currentTab: ContactTab,
    onTabSelected: (ContactTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {

        ScrollableTabRow(
            selectedTabIndex = currentTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            ContactTab.entries.forEach { tab ->
                Tab(
                    selected = currentTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            text = when (tab) {
                                ContactTab.CONTACTS -> "All"
                                ContactTab.FAVORITES -> "Favorites"
                                ContactTab.RECENT -> "Recent"
                                ContactTab.GROUPS -> "Groups"
                                ContactTab.SEARCH -> "Search"
                            }
                        )
                    }
                )
            }
        }

        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactList(
    contacts: List<ContactQuickInfo>,
    viewMode: ViewMode,
    isSelectionMode: Boolean,
    selectedContacts: Set<String>,
    onContactClick: (ContactQuickInfo) -> Unit,
    onContactLongClick: (ContactQuickInfo) -> Unit = {},
    onFavoriteClick: (ContactQuickInfo, Boolean) -> Unit,
    onCallClick: (String) -> Unit
) {
    when (viewMode) {
        ViewMode.LIST -> {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(contacts, key = { it.displayName }) { contact ->
                    ContactListItem(
                        contact = contact,
                        isSelected = selectedContacts.contains(contact.displayName),
                        isSelectionMode = isSelectionMode,
                        onClick = { onContactClick(contact) },
                        onLongClick = { onContactLongClick(contact) },
                        onFavoriteClick = { onFavoriteClick(contact, contact.isStarred) },
                        onCallClick = { onCallClick(contact.primaryPhoneNumber ?: "") }
                    )
                }
            }
        }

        ViewMode.GRID -> {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp
            ) {
                items(contacts, key = { it.displayName }) { contact ->
                    ContactGridItem(
                        contact = contact,
                        isSelected = selectedContacts.contains(contact.displayName),
                        isSelectionMode = isSelectionMode,
                        onClick = { onContactClick(contact) },
                        onLongClick = { onContactLongClick(contact) },
                        onFavoriteClick = { onFavoriteClick(contact, contact.isStarred) }
                    )
                }
            }
        }
    }
}

@Composable
fun ContactListItem(
    modifier: Modifier = Modifier,
    contact: ContactQuickInfo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)) // 👈 modern touch target
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .animateContentSize(),
        tonalElevation = if (isSelected) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ✅ Selection checkbox (cleaner spacing)
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // ✅ Avatar
            ContactAvatar(
                photoUri = contact.photoUri,
                initials = contact.initials,
                contactType = contact.contactType,
                size = 48.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // ✅ Main Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Text(
                        text = contact.displayName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (contact.isStarred) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (!contact.primaryPhoneNumber.isNullOrEmpty()) {
                    Text(
                        text = contact.primaryPhoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ✅ Actions (better spacing + consistency)
            if (!isSelectionMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(40.dp) // 👈 tighter touch target
                    ) {
                        Icon(
                            imageVector = if (contact.isStarred)
                                Icons.Rounded.Star
                            else
                                Icons.Rounded.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (contact.isStarred)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (contact.primaryPhoneNumber != null) {
                        IconButton(
                            onClick = onCallClick,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Call,
                                contentDescription = "Call",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactGridItem(
    modifier: Modifier = Modifier,
    contact: ContactQuickInfo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .animateContentSize(),
        tonalElevation = if (isSelected) 3.dp else 1.dp,
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {

            // 🔹 MAIN CONTENT
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.height(4.dp))

                // ✅ Avatar (hero element)
                ContactAvatar(
                    photoUri = contact.photoUri,
                    initials = contact.initials,
                    contactType = contact.contactType,
                    size = 64.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // ✅ Name (clean & centered)
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            // 🔹 TOP LEFT → Selection
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(22.dp)
                )
            }

            // 🔹 TOP RIGHT → Favorite
            if (!isSelectionMode) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (contact.isStarred)
                            Icons.Rounded.Star
                        else
                            Icons.Rounded.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (contact.isStarred)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ContactAvatar(
    photoUri: Uri?,
    initials: String,
    contactType: ContactType,
    size: Dp
) {
    val bgColor = when (contactType) {
        ContactType.GOOGLE -> MaterialTheme.colorScheme.primaryContainer
        ContactType.SIM -> MaterialTheme.colorScheme.secondaryContainer
        ContactType.WHATSAPP -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when (contactType) {
        ContactType.GOOGLE -> MaterialTheme.colorScheme.onPrimaryContainer
        ContactType.SIM -> MaterialTheme.colorScheme.onSecondaryContainer
        ContactType.WHATSAPP -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {

        if (photoUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Contact Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initials.take(2).uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = textColor
            )
        }
    }
}

@Composable
fun GroupsList(
    groups: List<ContactGroupWithUi>,
    onGroupClick: (ContactGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier
    ) {
        items(groups, key = { it.group.id }) { group ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 1.dp,
                onClick = { onGroupClick(group.group) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = "Group",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = group.group.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${group.group.count} contacts",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BoxScope.SelectionBottomBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onSelectAll: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedCount selected",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Row {
                TextButton(onClick = onSelectAll) {
                    Text("Select All")
                }
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyContactsView(
    message: String,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Contacts,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = message,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAddClick) {
                Text("Add Contact")
            }
        }
    }
}

@Composable
fun EmptyFavoritesView(onAddClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.StarBorder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No favorite contacts",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Star your important contacts to see them here",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Button(onClick = onAddClick) {
                Text("Add Contact")
            }
        }
    }
}

@Composable
fun EmptyRecentView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No recent contacts",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Your recently contacted contacts will appear here",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun EmptySearchView(
    query: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.FindInPage,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No  contacts found: $query",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Your search result will display here.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun EmptyGroupsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Group,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No contact groups",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Create groups to organize your contacts",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}