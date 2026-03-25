package com.ghost.caller.ui.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.ghost.caller.presentation.call.CallViewModel
import com.ghost.caller.ui.screens.recent.CallLogScreen
import com.ghost.caller.ui.screens.call.CallScreen
import com.ghost.caller.ui.screens.call.DialerScreen
import com.ghost.caller.ui.screens.contact.AddEditContactScreen
import com.ghost.caller.ui.screens.contact.ContactScreen
import com.ghost.caller.viewmodel.call.CallEvent
import com.ghost.caller.viewmodel.contact.ContactViewModel
import com.ghost.caller.viewmodel.contact.add.AddEditContactViewModel
import com.ghost.caller.viewmodel.recent.CallLogViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.util.logging.Logger


@Serializable
sealed interface NavigationBarKey : NavKey {
    @Serializable
    data object RecentCall : NavigationBarKey

    @Serializable
    data object Contacts : NavigationBarKey
}

@Serializable
data class ContactDetailKey(val contact: String? = null) : NavKey

@Serializable
data class CallScreenKey(val callId: String) : NavKey



@Composable
fun AppNavigation(
    callLogViewModel: CallLogViewModel = koinViewModel(),
    contactViewModel: ContactViewModel = koinViewModel(),
    callViewModel: CallViewModel = koinViewModel(),
) {

    val backStack = rememberNavBackStack(NavigationBarKey.RecentCall)

    val callState by callViewModel.state.collectAsState()

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            when (key) {
                NavigationBarKey.RecentCall -> NavEntry(key) {
                    CallLogScreen(
                        onNavigateToCall = { /* TODO: implement */ },
                        onNavigateToSms = { /* TODO: implement */ },
                        onNavigateToContact = { contactId ->
                            backStack.add(ContactDetailKey(contactId))
                        },
                        onNavigateToAddContact = { _, _ ->
                            backStack.add(ContactDetailKey(null))
                        },
                        navigationBar = {
                            CallerNavigationBar(onClick = backStack::add)
                        },
                        onKeypadClick = { backStack.add(CallScreenKey("")) },
                        viewModel = callLogViewModel,
                    )
                }

                NavigationBarKey.Contacts -> NavEntry(key) {
                    ContactScreen(
                        onNavigateToContactDetail = { contactId ->
                            backStack.add(ContactDetailKey(contactId))
                        },
                        onNavigateToAddContact = {
                            backStack.add(ContactDetailKey())
                        },
                        onNavigateToEditContact = { contactId ->
                            Timber.d("Editing contact: $contactId")
                            backStack.add(ContactDetailKey(contactId))
                        },
                        onNavigateToCall = { /* TODO: implement */ },
                        onNavigateToSms = { /* TODO: implement */ },
                        onNavigateToEmail = { /* TODO: implement */ },
                        navigationBar = {
                            CallerNavigationBar(onClick = backStack::add)
                        },
                        viewModel = contactViewModel
                    )
                }

                is ContactDetailKey -> NavEntry(key) {
                    val viewModel: AddEditContactViewModel = koinViewModel(key = key.contact) { parametersOf(key.contact) }
                    AddEditContactScreen(
                        contactId = key.contact,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        viewModel = viewModel
                    )
                }

                is CallScreenKey -> NavEntry(key) {
                    CallScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                        viewModel = callViewModel
                    )
                }

                else -> throw IllegalArgumentException("Unknown key: $key")
            }
        }
    )
}
