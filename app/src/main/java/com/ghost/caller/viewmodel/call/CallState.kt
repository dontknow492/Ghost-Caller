package com.ghost.caller.viewmodel.call

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Voicemail
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ghost.caller.models.ContactQuickInfo

// --- STATE MANAGEMENT ---


enum class CallType {
    INCOMING, OUTGOING, MISSED, REJECTED, BLOCKED, VOICEMAIL, UNKNOWN
}

// 2. Add the new fields to your Data Class
data class CallLogEntry(
    val id: String,
    val number: String,
    val name: String?,
    val timestamp: Long,
    val type: CallType,
    val durationSeconds: Long,      // NEW: For call duration
    val isRead: Boolean,            // NEW: For unread missed calls
    val location: String?,          // NEW: "California", "New York", etc.
    val isVideoCall: Boolean,       // NEW: Was it a video call?
    var groupedCount: Int = 1       // NEW: To show "Jane Doe (3)"
)


fun getCallTypeText(call: CallLogEntry): String {
    return when (call.type) {
        CallType.INCOMING -> "Incoming"
        CallType.OUTGOING -> "Outgoing"
        CallType.MISSED -> "Missed"
        CallType.REJECTED -> "Rejected"
        CallType.BLOCKED -> "Blocked"
        CallType.VOICEMAIL -> "Voicemail"
        else -> "Call"
    }
}


@Composable
fun getCallTypeColor(call: CallLogEntry): Color {
    return when (call.type) {
        CallType.MISSED -> MaterialTheme.colorScheme.error
        CallType.INCOMING -> MaterialTheme.colorScheme.primary
        CallType.OUTGOING -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
fun getCallTypeContainerColor(type: CallType): Color {
    val scheme = MaterialTheme.colorScheme

    return when (type) {
        CallType.INCOMING -> scheme.primary.copy(alpha = 0.15f)
        CallType.OUTGOING -> scheme.secondary.copy(alpha = 0.15f)
        CallType.MISSED -> scheme.error.copy(alpha = 0.15f)
        CallType.REJECTED -> scheme.error.copy(alpha = 0.12f)
        CallType.BLOCKED -> scheme.error.copy(alpha = 0.10f)
        CallType.VOICEMAIL -> scheme.tertiary.copy(alpha = 0.15f)
        CallType.UNKNOWN -> scheme.surfaceVariant
    }
}

fun getCallTypeIcon(type: CallType): ImageVector {
    return when (type) {
        CallType.INCOMING -> Icons.AutoMirrored.Rounded.CallReceived
        CallType.OUTGOING -> Icons.AutoMirrored.Rounded.CallMade
        CallType.MISSED -> Icons.AutoMirrored.Rounded.CallMissed
        CallType.REJECTED -> Icons.Rounded.CallEnd
        CallType.BLOCKED -> Icons.Rounded.Block
        CallType.VOICEMAIL -> Icons.Rounded.Voicemail
        CallType.UNKNOWN -> Icons.AutoMirrored.Rounded.HelpOutline
    }
}


/**
 * Call UI State
 */
data class CallState(
    val callStatus: CallStatus = CallStatus.Idle,
    val callType: CallType = CallType.UNKNOWN,
    val phoneNumber: String = "",
    val contactName: String? = null,
    val contactPhotoUri: Uri? = null,
    val callDuration: Int = 0,
    val callDurationFormatted: String = "00:00",
    val callStartTime: Long = 0,

    // Call controls state
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isRecording: Boolean = false,
    val isOnHold: Boolean = false,
    val isBluetoothConnected: Boolean = false,
    val audioRoute: AudioRoute = AudioRoute.EARPIECE,

    // Dialer state
    val dialedNumber: String = "",
    val dialedNumberFormatted: String = "",
    val showDialpad: Boolean = true,

    // UI state
    val isCallScreenVisible: Boolean = false,
    val isIncomingCallScreenVisible: Boolean = false,
    val isCallConnecting: Boolean = false,
    val isCallConnected: Boolean = false,
    val isCallEnding: Boolean = false,

    // Error and loading
    val isLoading: Boolean = false,
    val error: CallError? = null,
    val successMessage: String? = null,

    // Statistics
    val missedCallCount: Int = 0,
    val callQuality: CallQuality = CallQuality.GOOD,
    val networkType: NetworkType = NetworkType.UNKNOWN,

    // Permissions
    val hasPhonePermission: Boolean = false,
    val hasContactsPermission: Boolean = false,
    val hasMicrophonePermission: Boolean = false,
    val hasManageOwnCallsPermission: Boolean = false
)

enum class CallStatus {
    Idle,           // No active call
    Dialing,        // Dialing out
    Ringing,        // Incoming call ringing
    Connecting,     // Call is connecting
    Active,         // Call is active
    OnHold,         // Call is on hold
    Disconnecting,  // Call is disconnecting
    Disconnected,   // Call has ended
    Missed,         // Missed call
    Rejected        // Call was rejected
}


enum class AudioRoute {
    EARPIECE,
    SPEAKER,
    BLUETOOTH,
    WIRED_HEADSET
}

enum class CallQuality {
    GOOD,
    FAIR,
    POOR
}

enum class NetworkType {
    UNKNOWN,
    WIFI,
    CELLULAR_2G,
    CELLULAR_3G,
    CELLULAR_4G,
    CELLULAR_5G
}

data class CallError(
    val code: ErrorCode,
    val message: String,
    val details: String? = null
)

enum class ErrorCode {
    NO_NUMBER,
    NO_PERMISSION,
    CALL_FAILED,
    NO_NETWORK,
    DEVICE_NOT_SUPPORTED,
    UNKNOWN
}

data class ContactSuggestion(
    val name: String,
    val number: String,
    val photoUri: Uri?,
    val contactType: String?
)

/**
 * Call UI Events (User Actions)
 */
sealed class CallEvent {
    // Dialer actions
    object ShowDialpad : CallEvent()
    object HideDialpad : CallEvent()
    data class AppendDigit(val digit: String) : CallEvent()
    object DeleteDigit : CallEvent()
    data class SetNumber(val number: String) : CallEvent()
    object ClearNumber : CallEvent()
    data class SelectContactSuggestion(val contact: ContactQuickInfo) : CallEvent()

    // Call actions
    object InitiateCall : CallEvent()
    data class InitiateCallDirectly(val contactNumber: String) : CallEvent()
    object AcceptCall : CallEvent()
    object RejectCall : CallEvent()
    object EndCall : CallEvent()
    object ToggleMute : CallEvent()
    object ToggleSpeaker : CallEvent()
    object ToggleHold : CallEvent()
    object ToggleRecording : CallEvent()
    data class ChangeAudioRoute(val route: AudioRoute) : CallEvent()

    // Contact actions
    data class AddToContacts(val number: String, val name: String?) : CallEvent()
    data class BlockNumber(val number: String) : CallEvent()

    // System events
    data class CallStateChanged(val status: CallStatus, val number: String?) : CallEvent()
    data class CallDurationUpdated(val duration: Int) : CallEvent()
    data class AudioRouteChanged(val route: AudioRoute) : CallEvent()
    data class NetworkChanged(val type: NetworkType) : CallEvent()
    data class PermissionsChanged(val permissions: Map<String, Boolean>) : CallEvent()

    // UI events
    object DismissError : CallEvent()
    object DismissSuccess : CallEvent()
    object ClearCallScreen : CallEvent()
    object RetryCall : CallEvent()
}

/**
 * Call Side Effects (One-time events)
 */
sealed class CallSideEffect {
    // Navigation effects
    data class NavigateToCallScreen(val number: String, val name: String?) : CallSideEffect()
    data class NavigateToIncomingCallScreen(val number: String, val name: String?) :
        CallSideEffect()

    object NavigateToDialer : CallSideEffect()
    object NavigateBack : CallSideEffect()

    // Communication effects
    data class MakeCall(val number: String) : CallSideEffect()
    data object AnswerCall : CallSideEffect()
    data object RejectCall : CallSideEffect()
    data object EndCall : CallSideEffect()
    data class ToggleMuteCall(val mute: Boolean) : CallSideEffect()
    data class ToggleSpeakerCall(val on: Boolean) : CallSideEffect()
    data class ToggleHoldCall(val hold: Boolean) : CallSideEffect()
    data class ChangeAudioRouteCall(val route: AudioRoute) : CallSideEffect()

    // UI effects
    data class ShowToast(val message: String) : CallSideEffect()
    data class ShowError(val error: CallError) : CallSideEffect()
    data class ShowCallDuration(val duration: String) : CallSideEffect()
    data class Vibrate(val duration: Long) : CallSideEffect()
    data class PlayRingtone(val loop: Boolean) : CallSideEffect()
    object StopRingtone : CallSideEffect()
    data class ShowNotification(val title: String, val message: String) : CallSideEffect()

    // Permission effects
    data class RequestPermissions(val permissions: List<String>) : CallSideEffect()
    data object OpenSettings : CallSideEffect()

    // Contact effects
    data class OpenAddContact(val number: String, val name: String?) : CallSideEffect()
    data class OpenBlockNumber(val number: String) : CallSideEffect()

    // Recording effects
    data class StartRecording(val filePath: String) : CallSideEffect()
    data class StopRecording(val filePath: String) : CallSideEffect()
}

/**
 * Call Result (for internal use)
 */
sealed class CallResult {
    data class Success(val message: String) : CallResult()
    data class Error(val error: CallError) : CallResult()
    object Loading : CallResult()
}