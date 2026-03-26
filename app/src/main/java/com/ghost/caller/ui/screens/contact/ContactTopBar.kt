@file:Suppress("D")

package com.ghost.caller.ui.screens.contact


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ghost.caller.ui.components.ModernSearchTextField
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

        ModernSearchTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth(),
            onSearch = { onSearchQueryChange(searchQuery) },
            placeholder = "Search contacts...",
            autoFocus = false
        )
    }
}