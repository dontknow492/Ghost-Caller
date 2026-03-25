package com.ghost.caller.viewmodel.contact


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.ghost.caller.models.ContactQuickInfo
import com.ghost.caller.ui.components.contact.ContactGridItem
import com.ghost.caller.ui.components.contact.ContactListItem
import com.ghost.caller.viewmodel.contact.ViewMode

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
                    var lastHeader: String? = null

                    items(
                        count = pagingItems.itemCount,
                        key = { index ->
                            pagingItems.peek(index)?.id ?: "placeholder_$index"
                        }
                    ) { index ->

                        val contact = pagingItems.peek(index)

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

            ViewMode.GRID -> {
                val gridState = rememberLazyGridState()

                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    modifier = modifier,
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
//                    verticalItemSpacing = 8.dp
                ) {
                    items(
                        count = pagingItems.itemCount,
                        key = { index ->
                            pagingItems.peek(index)?.id ?: "placeholder_$index"
                        }
                    ) { index ->

                        val contact = pagingItems.peek(index)

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




fun getSectionTitle(name: String): String {
    val first = name.firstOrNull()?.uppercaseChar()
    return if (first != null && first.isLetter()) first.toString() else "#"
}

@Composable
fun StickyHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}