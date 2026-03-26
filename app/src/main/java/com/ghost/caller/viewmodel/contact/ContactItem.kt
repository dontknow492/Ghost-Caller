package com.ghost.caller.viewmodel.contact


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.ghost.caller.models.ContactQuickInfo
import com.ghost.caller.ui.components.ContactGridItem
import com.ghost.caller.ui.components.ContactListItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactPagingList(
    pagingItems: LazyPagingItems<ContactQuickInfo>,
    viewMode: ViewMode,
    isSelectionMode: Boolean,
    selectedContacts: Set<String>,
    onContactClick: (ContactQuickInfo) -> Unit,
    onContactLongClick: (ContactQuickInfo) -> Unit,
    onFavoriteClick: (ContactQuickInfo, Boolean) -> Unit,
    onCallClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    key(viewMode) {

        when (viewMode) {

            ViewMode.LIST -> {
                val listState = rememberLazyListState()

                LazyColumn(
                    state = listState,
                    modifier = modifier,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {

                    items(
                        count = pagingItems.itemCount,
                        key = { index ->
                            pagingItems.peek(index)?.id ?: "placeholder_$index"
                        }
                    ) { index ->

                        val contact = pagingItems[index]

                        // 🔥 Look at the previous contact to see if the letter changed
                        val prevContact = if (index > 0) pagingItems.peek(index - 1) else null

                        val getInitial: (ContactQuickInfo?) -> String = { info ->
                            val first = info?.displayName?.trim()?.firstOrNull()
                            if (first != null && first.isLetter()) first.uppercase() else "#"
                        }

                        val currentInitial = getInitial(contact)
                        val prevInitial = getInitial(prevContact)

                        Column {
                            // 📌 If it's the very first item, OR the letter changed, show the Header!
                            if (index == 0 || currentInitial != prevInitial) {
                                ContactHeaderLabel(text = currentInitial)
                            }

                            contact?.let {
                                ContactListItem(
                                    contact = it,
                                    isSelected = selectedContacts.contains(it.id),
                                    isSelectionMode = isSelectionMode,
                                    onClick = { onContactClick(it) },
                                    onLongClick = { onContactLongClick(it) },
                                    onFavoriteClick = { onFavoriteClick(it, it.isStarred) },
                                    onCallClick = { onCallClick(it.primaryPhoneNumber ?: "") }
                                )
                            }
                        }
                    }
                }
            }

            ViewMode.GRID -> {
                val gridState = rememberLazyGridState()

                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    modifier = modifier,
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = pagingItems.itemCount,
                        key = { index ->
                            pagingItems.peek(index)?.id ?: "placeholder_$index"
                        }
                    ) { index ->

                        val contact = pagingItems[index]

                        // Note: Headers are omitted in Grid mode for Pagination because injecting
                        // a full-width header dynamically inside a grid cell breaks the grid structure.
                        contact?.let {
                            ContactGridItem(
                                modifier = Modifier.animateItem(),
                                contact = it,
                                isSelected = selectedContacts.contains(it.id),
                                isSelectionMode = isSelectionMode,
                                onClick = { onContactClick(it) },
                                onLongClick = { onContactLongClick(it) },
                                onFavoriteClick = { onFavoriteClick(it, it.isStarred) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean, standard Material 3 header label for the alphabetical grouping
 */
@Composable
fun ContactHeaderLabel(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}