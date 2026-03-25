package com.ghost.caller.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey


data class MyNavigationBarItem(
    val icon: ImageVector,
    val label: String,
    val key: NavigationBarKey,
    val selectedIcon: ImageVector?
)

val items = listOf<MyNavigationBarItem>(
    MyNavigationBarItem(
        icon = Icons.Filled.Home,
        label = "Recent Call",
        key = NavigationBarKey.RecentCall,
        selectedIcon = Icons.Outlined.Home
    ),
    MyNavigationBarItem(
        icon = Icons.Filled.Contacts,
        label = "Contacts",
        key = NavigationBarKey.Contacts,
        selectedIcon = Icons.Outlined.Contacts
    )
)

@Composable
fun CallerNavigationBar(
    modifier: Modifier = Modifier,
    selected: Int = 0,
    onClick: (NavKey) -> Unit
) {
    NavigationBar(
        modifier = modifier
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onClick(item.key) },
                icon = {
                    if (selected == index) {
                        item.selectedIcon?.let { icon ->
                            Icon(imageVector = icon, contentDescription = item.label)
                        }
                    } else {
                        Icon(imageVector = item.icon, contentDescription = item.label)
                    }
                },
                label = {
                    Text(text = item.label)
                }
            )

        }
    }
}