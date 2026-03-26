package com.ghost.caller.ui.navigation

import android.content.Context
import android.content.Intent
import android.widget.Toast
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

// --------------------- Navigation Keys ---------------------

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

// --------------------- Helper Function ---------------------

private fun Context.launchIntentWithToast(intent: Intent, toastMessage: String) {
    try {
        startActivity(intent)
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Timber.e(e, "Failed to launch intent")
        Toast.makeText(this, "Unable to perform action", Toast.LENGTH_SHORT).show()
    }
}

// --------------------- App Navigation ---------------------

@Composable
fun AppNavigation(
    callLogViewModel: CallLogViewModel = koinViewModel(),
    contactViewModel: ContactViewModel = koinViewModel(),
    callViewModel: CallViewModel = koinViewModel(),
    launchedForCall: Boolean = false,
    onCloseApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val callState by callViewModel.state.collectAsState()

    val initialKeys = remember {
        val initialState = callViewModel.state.value
        val needsCallScreen =
            launchedForCall || initialState.isCallScreenVisible || initialState.isIncomingCallScreenVisible
        if (needsCallScreen) {
            val isInitiate = initialState.callStatus == CallStatus.Dialing
            arrayOf(
                NavigationBarKey.RecentCall,
                CallScreenKey(initialState.phoneNumber, isInitiate)
            )
        } else arrayOf(NavigationBarKey.RecentCall)
    }

    val backStack = rememberNavBackStack(*initialKeys)

    // --------------------- Reactive Call Screen Navigation ---------------------
    LaunchedEffect(callState.isCallScreenVisible, callState.isIncomingCallScreenVisible) {
        val needsCallScreen = callState.isCallScreenVisible || callState.isIncomingCallScreenVisible
        val isOnCallScreen = backStack.lastOrNull() is CallScreenKey

        if (needsCallScreen && !isOnCallScreen) {
            val isInitiate = callState.callStatus == CallStatus.Dialing
            backStack.add(CallScreenKey(callState.phoneNumber, isInitiate))
        } else if (!needsCallScreen && isOnCallScreen) {
            if (launchedForCall) onCloseApp()
            else backStack.removeLastOrNull()
        }
    }

    // --------------------- Navigation Display ---------------------
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            when (key) {

                NavigationBarKey.RecentCall -> NavEntry(key) {
                    CallLogScreen(
                        onNavigateToCall = { backStack.add(CallScreenKey(it, true)) },
                        onNavigateToSms = { phoneNumber ->
                            context.launchIntentWithToast(
                                Intent(Intent.ACTION_SENDTO).apply {
                                    data = "smsto:$phoneNumber".toUri()
                                },
                                "Opening SMS for $phoneNumber"
                            )
                        },
                        onNavigateToContact = { contactId ->
                            backStack.add(
                                ContactDetailKey(
                                    contactId
                                )
                            )
                        },
                        onNavigateToAddContact = { phone, name ->
                            backStack.add(
                                ContactDetailKey(
                                    phone,
                                    name
                                )
                            )
                        },
                        navigationBar = {
                            CallerNavigationBar(
                                onClick = backStack::add,
                                selected = 0
                            )
                        },
                        onKeypadClick = { backStack.add(CallScreenKey(null, false)) },
                        viewModel = callLogViewModel,
                    )
                }

                NavigationBarKey.Contacts -> NavEntry(key) {
                    ContactScreen(
                        onNavigateToContactDetail = { backStack.add(ContactDetailKey(it)) },
                        onNavigateToAddContact = { backStack.add(ContactDetailKey()) },
                        onNavigateToEditContact = { contactId ->
                            Timber.d("Editing contact: $contactId")
                            backStack.add(ContactDetailKey(contactId))
                        },
                        onNavigateToCall = { backStack.add(CallScreenKey(it, true)) },
                        onNavigateToSms = { phone ->
                            context.launchIntentWithToast(
                                Intent(Intent.ACTION_SENDTO).apply {
                                    data = "smsto:$phone".toUri()
                                },
                                "Opening SMS for $phone"
                            )
                        },
                        onNavigateToEmail = { email ->
                            context.launchIntentWithToast(
                                Intent(Intent.ACTION_SENDTO).apply {
                                    data = "mailto:$email".toUri()
                                },
                                "Opening Email for $email"
                            )
                        },
                        navigationBar = {
                            CallerNavigationBar(
                                onClick = backStack::add,
                                selected = 1
                            )
                        },
                        onNavigateBack = { backStack.removeLastOrNull() },
                        viewModel = contactViewModel
                    )
                }

                is ContactDetailKey -> NavEntry(key) {
                    val viewModel: AddEditContactViewModel =
                        koinViewModel(key = key.contactId) { parametersOf(key.contactId, key.name) }
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
                        initiateCallDirectly = key.initiateCall,
                        onMessage = { phone ->
                            context.launchIntentWithToast(
                                Intent(Intent.ACTION_SENDTO).apply {
                                    data = "smsto:$phone".toUri()
                                },
                                "Opening message for $phone"
                            )
                        },
                        onRemindMe = { phone ->
                            context.launchIntentWithToast(
                                Intent(Intent.ACTION_DIAL).apply { data = "tel:$phone".toUri() },
                                "Opening reminder for $phone"
                            )
                        }
                    )
                }

                else -> throw IllegalArgumentException("Unknown key: $key")
            }
        },
        transitionSpec = { slideInHorizontally { it } togetherWith slideOutHorizontally { -it } },
        popTransitionSpec = { slideInHorizontally { -it } togetherWith slideOutHorizontally { it } },
        predictivePopTransitionSpec = { slideInHorizontally { -it } togetherWith slideOutHorizontally { it } },
    )
}