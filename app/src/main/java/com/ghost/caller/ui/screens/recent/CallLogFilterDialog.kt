package com.ghost.caller.ui.screens.recent


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Voicemail
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghost.caller.viewmodel.recent.CallTypeFilter
import com.ghost.caller.viewmodel.recent.DateRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallLogFilterDialog(
    currentType: CallTypeFilter,
    currentDateRange: DateRange,
    onDismiss: () -> Unit,
    onApply: (CallTypeFilter, DateRange) -> Unit
) {
    var selectedType by remember { mutableStateOf(currentType) }
    var selectedDateRange by remember { mutableStateOf(currentDateRange) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Filter Call Logs",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {

                // 🔹 Call Type Section
                FilterSection(title = "Call Type") {
                    CallTypeFilterChips(
                        selectedType = selectedType,
                        onTypeSelected = { selectedType = it }
                    )
                }

                // 🔹 Date Range Section
                FilterSection(title = "Date Range") {
                    DateRangeFilterChips(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        selectedRange = selectedDateRange,
                        onRangeSelected = { selectedDateRange = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(selectedType, selectedDateRange)
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

@Composable
fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallTypeFilterChips(
    selectedType: CallTypeFilter,
    onTypeSelected: (CallTypeFilter) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CallTypeFilter.entries.forEach { type ->

            val isSelected = selectedType == type

            FilterChip(
                selected = isSelected,
                onClick = { onTypeSelected(type) },
                label = {
                    Text(formatEnum(type.name))
                },
                leadingIcon = {
                    Icon(
                        imageVector = when (type) {
                            CallTypeFilter.ALL -> Icons.Rounded.Call
                            CallTypeFilter.INCOMING -> Icons.AutoMirrored.Rounded.CallReceived
                            CallTypeFilter.OUTGOING -> Icons.AutoMirrored.Rounded.CallMade
                            CallTypeFilter.MISSED -> Icons.AutoMirrored.Rounded.CallMissed
                            CallTypeFilter.REJECTED -> Icons.Rounded.Cancel
                            CallTypeFilter.BLOCKED -> Icons.Rounded.Block
                            CallTypeFilter.VOICEMAIL -> Icons.Rounded.Voicemail
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(0.15f)
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeFilterChips(
    modifier: Modifier = Modifier,
    selectedRange: DateRange,
    onRangeSelected: (DateRange) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DateRange.entries.forEach { range ->

            val isSelected = selectedRange == range

            FilterChip(
                selected = isSelected,
                onClick = { onRangeSelected(range) },
                label = {
                    Text(formatEnum(range.name))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(0.15f)
                )
            )
        }
    }
}


fun formatEnum(value: String): String {
    return value.lowercase()
        .split("_")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}