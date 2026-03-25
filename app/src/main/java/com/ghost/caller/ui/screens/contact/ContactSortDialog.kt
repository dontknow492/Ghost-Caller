package com.ghost.caller.ui.screens.contact


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghost.caller.repository.ContactSort

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSortDialog(
    currentSort: ContactSort,
    onDismiss: () -> Unit,
    onApply: (ContactSort) -> Unit
) {
    var selectedSort by remember { mutableStateOf(currentSort) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Sort Contacts",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                ContactSort.entries.forEach { sort ->

                    val isSelected = selectedSort == sort

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSort = sort },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(0.1f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isSelected) 6.dp else 1.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // 🔹 Icon
                            Icon(
                                imageVector = when (sort) {
                                    ContactSort.NAME_ASC -> Icons.Rounded.SortByAlpha
                                    ContactSort.NAME_DESC -> Icons.Rounded.SortByAlpha
                                    ContactSort.RECENT_ASC -> Icons.Rounded.History
                                    ContactSort.RECENT_DESC -> Icons.Rounded.Schedule
                                },
                                contentDescription = null,
                                tint = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // 🔹 Texts
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (sort) {
                                        ContactSort.NAME_ASC -> "Name (A → Z)"
                                        ContactSort.NAME_DESC -> "Name (Z → A)"
                                        ContactSort.RECENT_ASC -> "Oldest First"
                                        ContactSort.RECENT_DESC -> "Recently Added"
                                    },
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                Text(
                                    text = when (sort) {
                                        ContactSort.NAME_ASC -> "Contacts sorted alphabetically"
                                        ContactSort.NAME_DESC -> "Reverse alphabetical order"
                                        ContactSort.RECENT_ASC -> "Old contacts appear first"
                                        ContactSort.RECENT_DESC -> "Newest contacts on top"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // 🔹 Radio
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedSort = sort }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(selectedSort)
                    onDismiss()
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}