package com.ghost.caller.ui.navigation

import android.content.Intent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.ghost.caller.ui.screens.call.CallScreen
import com.ghost.caller.ui.screens.contact.AddEditContactScreen
import com.ghost.caller.ui.screens.contact.ContactScreen
import com.ghost.caller.ui.screens.recent.CallLogScreen
import com.ghost.caller.viewmodel.call.CallStatus
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
data class ContactDetailKey(val contactId: String? = null, val name: String? = null) : NavKey

@Serializable
data class CallScreenKey(val phoneNumber: String?, val initiateCall: Boolean) : NavKey

@Composable
fun AppNavigation(
    callLogViewModel: CallLogViewModel = koinViewModel(),
    contactViewModel: ContactViewModel = koinViewModel(),
    callViewModel: CallViewModel = koinViewModel(),
    launchedForCall: Boolean = false,
    onCloseApp: () -> Unit = {}
) {
    val context = LocalContext.current

    // 🔥 1. Collect the Call State directly
    val callState by callViewModel.state.collectAsState()

    // 🔥 2. ELIMINATE LAG: Pre-calculate the initial back stack
    // If the app is launched for a call, we put the CallScreen directly on top BEFORE the first frame is drawn.
    // This entirely skips the laggy animation of transitioning from RecentCall -> CallScreen on cold boot.
    val initialKeys = remember {
        val initialState = callViewModel.state.value
        val initialNeedsCallScreen =
            launchedForCall || initialState.isCallScreenVisible || initialState.isIncomingCallScreenVisible

        if (initialNeedsCallScreen) {
            val isInitiate = initialState.callStatus == CallStatus.Dialing
            arrayOf(
                NavigationBarKey.RecentCall,
                CallScreenKey(initialState.phoneNumber, isInitiate)
            )
        } else {
            arrayOf(NavigationBarKey.RecentCall)
        }
    }

    val backStack = rememberNavBackStack(*initialKeys)

    // 🔥 3. State-driven navigation for ongoing updates (e.g. call ends)
    LaunchedEffect(callState.isCallScreenVisible, callState.isIncomingCallScreenVisible) {
        val needsCallScreen = callState.isCallScreenVisible || callState.isIncomingCallScreenVisible
        val isCurrentlyOnCallScreen = backStack.lastOrNull() is CallScreenKey

        if (needsCallScreen && !isCurrentlyOnCallScreen) {
            // This now only runs for mid-session calls, not cold boots
            val isInitiate = callState.callStatus == CallStatus.Dialing
            backStack.add(CallScreenKey(callState.phoneNumber, isInitiate))
        } else if (!needsCallScreen && isCurrentlyOnCallScreen) {
            // Auto-pop when the call ends or is dismissed
            if (launchedForCall) {
                // If the app was closed and woke up just for the call, put it back to sleep
                onCloseApp()
            } else {
                // If the app was already open, just pop the call screen to reveal what the user was looking at
                backStack.removeLastOrNull()
            }
        }
    }

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
                        onNavigateToSms = { phoneNumber ->
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "smsto:$phoneNumber".toUri()
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to launch SMS app")
                            }
                        },
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
                        onNavigateToSms = { phoneNumber ->
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "smsto:$phoneNumber".toUri()
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to launch SMS app")
                            }
                        },
                        onNavigateToEmail = { email ->
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:$email".toUri()
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to launch Email app")
                            }
                        },
                        navigationBar = {
                            CallerNavigationBar(onClick = backStack::add, selected = 1)
                        },
                        onNavigateBack = { backStack.removeLastOrNull() },
                        viewModel = contactViewModel
                    )
                }

                is ContactDetailKey -> NavEntry(key) {
                    val viewModel: AddEditContactViewModel =
                        koinViewModel(key = key.contactId) {
                            parametersOf(
                                key.contactId,
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