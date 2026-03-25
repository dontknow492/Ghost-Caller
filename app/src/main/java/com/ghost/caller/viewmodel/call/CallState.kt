package com.ghost.caller.viewmodel.call

import android.net.Uri

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
    val suggestions: List<ContactSuggestion> = emptyList(),
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
    data class SearchContacts(val query: String) : CallEvent()
    data class SelectContactSuggestion(val contact: ContactSuggestion) : CallEvent()

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