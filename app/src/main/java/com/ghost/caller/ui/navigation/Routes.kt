package com.ghost.caller.ui.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.ghost.caller.ui.screens.call.CallScreen
import com.ghost.caller.ui.screens.contact.AddEditContactScreen
import com.ghost.caller.ui.screens.contact.ContactScreen
import com.ghost.caller.ui.screens.recent.CallLogScreen
import com.ghost.caller.viewmodel.call.CallViewModel
import com.ghost.caller.viewmodel.contact.ContactViewModel
import com.ghost.caller.viewmodel.contact.add.AddEditContactViewModel
import com.ghost.caller.viewmodel.recent.CallLogViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber


@Serializable
sealed interface NavigationBarKey : NavKey {
    @Serializable
    data object RecentCall : NavigationBarKey

    @Serializable
    data object Contacts : NavigationBarKey
}

@Serializable
data class ContactDetailKey(val phoneNumber: String? = null, val name: String? = null) : NavKey

@Serializable
data class CallScreenKey(val phoneNumber: String?, val initiateCall: Boolean) : NavKey


@Composable
fun AppNavigation(
    callLogViewModel: CallLogViewModel = koinViewModel(),
    contactViewModel: ContactViewModel = koinViewModel(),
    callViewModel: CallViewModel = koinViewModel(),
) {

    val backStack = rememberNavBackStack(NavigationBarKey.RecentCall)

    NavDisplay(
        backStack = backStack,
        onBack = {
            backStack.removeLastOrNull()
        },
        entryProvider = { key ->
            when (key) {
                NavigationBarKey.RecentCall -> NavEntry(key) {
                    CallLogScreen(
                        onNavigateToCall = {
                            backStack.add(CallScreenKey(it, true))
                        },
                        onNavigateToSms = { /* TODO: implement */ },
                        onNavigateToContact = { contactId ->
                            backStack.add(ContactDetailKey(contactId))
                        },
                        onNavigateToAddContact = { phoneNumber, name ->
                            backStack.add(ContactDetailKey(phoneNumber, name))
                        },
                        navigationBar = {
                            CallerNavigationBar(onClick = backStack::add, selected = 0)
                        },
                        onKeypadClick = { backStack.add(CallScreenKey(null, false)) },
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
                        onNavigateToCall = { phoneNumber ->
                            backStack.add(CallScreenKey(phoneNumber, true))
                        },
                        onNavigateToSms = { /* TODO: implement */ },
                        onNavigateToEmail = { /* TODO: implement */ },
                        navigationBar = {
                            CallerNavigationBar(onClick = backStack::add, selected = 1)
                        },
                        viewModel = contactViewModel
                    )
                }

                is ContactDetailKey -> NavEntry(key) {
                    val viewModel: AddEditContactViewModel =
                        koinViewModel(key = key.phoneNumber) {
                            parametersOf(
                                key.phoneNumber,
                                key.name
                            )
                        }
                    AddEditContactScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                        viewModel = viewModel
                    )
                }

                is CallScreenKey -> NavEntry(key) {
                    CallScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                        viewModel = callViewModel,
                        phoneNumber = key.phoneNumber,
                        initiateCallDirectly = key.initiateCall
                    )
                }

                else -> throw IllegalArgumentException("Unknown key: $key")
            }
        },
        transitionSpec = {
            // Slide in from right when navigating forward
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { -it })
        },
        popTransitionSpec = {
            // Slide in from left when navigating back
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            // Slide in from left when navigating back
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        },
    )
}
