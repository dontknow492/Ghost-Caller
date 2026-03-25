package com.ghost.caller.ui.screens.recent


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
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Schedule
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
import com.ghost.caller.viewmodel.recent.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallLogSortDialog(
    currentSort: SortOrder,
    onDismiss: () -> Unit,
    onApply: (SortOrder) -> Unit
) {
    var selectedSort by remember { mutableStateOf(currentSort) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Sort Call Logs",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                SortOrder.values().forEach { sort ->

                    val isSelected = selectedSort == sort

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSort = sort },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isSelected) 6.dp else 1.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector = when (sort) {
                                    SortOrder.DATE_DESC -> Icons.Rounded.Schedule
                                    SortOrder.DATE_ASC -> Icons.Rounded.History
                                    SortOrder.DURATION_DESC -> Icons.AutoMirrored.Rounded.TrendingUp
                                    SortOrder.DURATION_ASC -> Icons.AutoMirrored.Rounded.TrendingDown
                                },
                                contentDescription = null,
                                tint = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (sort) {
                                        SortOrder.DATE_DESC -> "Newest First"
                                        SortOrder.DATE_ASC -> "Oldest First"
                                        SortOrder.DURATION_DESC -> "Longest Calls"
                                        SortOrder.DURATION_ASC -> "Shortest Calls"
                                    },
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                Text(
                                    text = when (sort) {
                                        SortOrder.DATE_DESC -> "Recently received calls first"
                                        SortOrder.DATE_ASC -> "Old calls appear first"
                                        SortOrder.DURATION_DESC -> "Calls with more duration first"
                                        SortOrder.DURATION_ASC -> "Short calls appear first"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

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