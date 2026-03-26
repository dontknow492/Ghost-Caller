package com.ghost.caller.viewmodel.call

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.TelecomManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ghost.caller.models.ContactQuickInfo
import com.ghost.caller.repository.ContactFilter
import com.ghost.caller.repository.ContactRepository
import com.ghost.caller.repository.ContactSort
import com.ghost.caller.repository.normalizeNumber
import com.ghost.caller.service.CallManager
import com.ghost.caller.service.CallService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

class CallViewModel(
    application: Application,
    contactRepository: ContactRepository
) : AndroidViewModel(application) {

    private val applicationContext = application
    private val telephonyManager: TelephonyManager =
        applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val telecomManager: TelecomManager =
        applicationContext.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

    // Private mutable state flow
    private val _state = MutableStateFlow(CallState())
    val state: StateFlow<CallState> = _state.asStateFlow()

    // Side effects channel
    private val _sideEffect = MutableSharedFlow<CallSideEffect>(
        replay = 0,
        extraBufferCapacity = 10
    )
    val sideEffect: SharedFlow<CallSideEffect> = _sideEffect.asSharedFlow()

    // Event channel
    private val _event = MutableSharedFlow<CallEvent>(
        replay = 0,
        extraBufferCapacity = 10
    )

    // Internal state
    private var currentCall: Call? = null
    private var durationTimerJob: Job? = null
    private var wasInCall = false

    private var telephonyCallback: TelephonyCallback? = null
    private var legacyPhoneStateListener: android.telephony.PhoneStateListener? = null

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val suggestions: Flow<PagingData<ContactQuickInfo>> =
        _state
            .map { it.dialedNumber }
            .map { it.trim() }
            .distinctUntilChanged()
            .debounce(250) // ⚡ slightly faster for typing UX
            .flatMapLatest { query ->

                if (query.isBlank()) {
                    return@flatMapLatest flowOf(PagingData.empty())
                }

                // 🔥 Normalize for better matching
                val normalized = normalizeNumber(query)

                contactRepository.searchContactsPaged(
                    query = normalized,
                    filter = ContactFilter.ALL,
                    sortBy = ContactSort.RECENT_DESC // better UX for suggestions
                )
            }
            .cachedIn(viewModelScope) // ✅ correct for paging

    init {
        viewModelScope.launch {
            _event.collect { event ->
                handleEvent(event)
            }
        }

        registerPhoneStateListener()
        observeCallService()
        checkPermissions()
        observeNetworkChanges()
    }

    fun sendEvent(event: CallEvent) {
        viewModelScope.launch {
            _event.emit(event)
        }
    }

    private fun handleEvent(event: CallEvent) {
        when (event) {
            is CallEvent.ShowDialpad -> showDialpad()
            is CallEvent.HideDialpad -> hideDialpad()
            is CallEvent.AppendDigit -> appendDigit(event.digit)
            is CallEvent.DeleteDigit -> deleteDigit()
            is CallEvent.SetNumber -> setNumber(event.number)
            is CallEvent.ClearNumber -> clearNumber()
            is CallEvent.SelectContactSuggestion -> selectContactSuggestion(event.contact)

            is CallEvent.InitiateCall -> initiateCall()
            is CallEvent.AcceptCall -> acceptCall()
            is CallEvent.RejectCall -> rejectCall()
            is CallEvent.EndCall -> endCall()
            is CallEvent.ToggleMute -> toggleMute()
            is CallEvent.ToggleSpeaker -> toggleSpeaker()
            is CallEvent.ToggleHold -> toggleHold()
            is CallEvent.ToggleRecording -> toggleRecording()
            is CallEvent.ChangeAudioRoute -> changeAudioRoute(event.route)

            is CallEvent.AddToContacts -> addToContacts(event.number, event.name)
            is CallEvent.BlockNumber -> blockNumber(event.number)

            is CallEvent.CallStateChanged -> updateCallState(event.status, event.number)
            is CallEvent.CallDurationUpdated -> updateCallDuration(event.duration)
            is CallEvent.AudioRouteChanged -> updateAudioRoute(event.route)
            is CallEvent.NetworkChanged -> updateNetworkType(event.type)
            is CallEvent.PermissionsChanged -> updatePermissions(event.permissions)

            is CallEvent.DismissError -> dismissError()
            is CallEvent.DismissSuccess -> dismissSuccess()
            is CallEvent.ClearCallScreen -> clearCallScreen()
            is CallEvent.RetryCall -> retryCall()

            is CallEvent.InitiateCallDirectly -> {
                setNumber(event.contactNumber)
                initiateCall()
            }
        }
    }

    // ========== Dialer Actions ==========

    private fun showDialpad() {
        _state.update { it.copy(showDialpad = true) }
    }

    private fun hideDialpad() {
        _state.update { it.copy(showDialpad = false) }
    }

    private fun appendDigit(digit: String) {
        _state.update { currentState ->
            val newNumber = currentState.dialedNumber + digit
            currentState.copy(
                dialedNumber = newNumber,
                dialedNumberFormatted = formatPhoneNumber(newNumber)
            )
        }
    }

    private fun deleteDigit() {
        _state.update { currentState ->
            val newNumber = if (currentState.dialedNumber.isNotEmpty()) {
                currentState.dialedNumber.dropLast(1)
            } else ""
            currentState.copy(
                dialedNumber = newNumber,
                dialedNumberFormatted = formatPhoneNumber(newNumber)
            )
        }
    }

    private fun setNumber(number: String) {
        _state.update {
            it.copy(
                dialedNumber = number,
                dialedNumberFormatted = formatPhoneNumber(number)
            )
        }
    }

    private fun clearNumber() {
        _state.update {
            it.copy(
                dialedNumber = "",
                dialedNumberFormatted = "",
            )
        }
    }

    private fun selectContactSuggestion(contact: ContactQuickInfo) {
        val phoneNumber = contact.primaryPhoneNumber ?: return
        setNumber(phoneNumber)
    }

    // ========== Call Actions ==========

    @SuppressLint("MissingPermission")
    private fun initiateCall() {
        val number = _state.value.dialedNumber
        if (number.isEmpty()) {
            sendSideEffect(
                CallSideEffect.ShowError(
                    CallError(ErrorCode.NO_NUMBER, "Please enter a phone number")
                )
            )
            return
        }

        if (!_state.value.hasPhonePermission) {
            sendSideEffect(
                CallSideEffect.RequestPermissions(
                    listOf(Manifest.permission.CALL_PHONE)
                )
            )
            return
        }

        _state.update {
            it.copy(
                isLoading = true,
                callStatus = CallStatus.Dialing,
                phoneNumber = number,
                contactName = getContactName(number),
                isCallScreenVisible = true
            )
        }

        try {
            if (_state.value.hasManageOwnCallsPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Use TelecomManager for direct calling
                val uri = "tel:$number".toUri()
                val bundle = Bundle()
                telecomManager.placeCall(uri, bundle)
                wasInCall = true
                sendSideEffect(CallSideEffect.MakeCall(number))
            } else {
                // Fallback to ACTION_CALL
                fallbackToDialer(number)
            }

            _state.update { it.copy(isLoading = false) }
            sendSideEffect(CallSideEffect.Vibrate(100))

        } catch (e: SecurityException) {
            _state.update {
                it.copy(
                    isLoading = false,
                    error = CallError(ErrorCode.NO_PERMISSION, "Call permission denied"),
                    callStatus = CallStatus.Idle,
                    isCallScreenVisible = false
                )
            }
            sendSideEffect(
                CallSideEffect.ShowError(
                    CallError(ErrorCode.NO_PERMISSION, "Unable to make call: Permission denied")
                )
            )
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isLoading = false,
                    error = CallError(ErrorCode.CALL_FAILED, "Failed to make call"),
                    callStatus = CallStatus.Idle,
                    isCallScreenVisible = false
                )
            }
            sendSideEffect(
                CallSideEffect.ShowError(
                    CallError(ErrorCode.CALL_FAILED, "Failed to initiate call: ${e.message}")
                )
            )
        }
    }

    private fun fallbackToDialer(number: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = "tel:$number".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            applicationContext.startActivity(intent)
            wasInCall = true
            sendSideEffect(CallSideEffect.MakeCall(number))
        } catch (e: SecurityException) {
            throw e
        }
    }

    private fun acceptCall() {
        if (_state.value.callStatus == CallStatus.Ringing) {
            // 1. ACTUALLY answer the call at the Telecom System level
            CallManager.answerCall()

            // 2. Optimistic UI update, but relying on CallService state change for confirmation
            _state.update {
                it.copy(
                    callStatus = CallStatus.Connecting,
                    isIncomingCallScreenVisible = false,
                    isCallScreenVisible = true
                )
            }
            sendSideEffect(CallSideEffect.AnswerCall)
            sendSideEffect(CallSideEffect.StopRingtone)

            // Note: We don't force CallStatus.Active immediately here anymore.
            // We let the CallService STATE_ACTIVE callback dictate it!
        }
    }

    private fun rejectCall() {
        if (_state.value.callStatus == CallStatus.Ringing) {
            CallManager.rejectCall()

            // State update - We just hide it, letting the telecom disconnect cleanly
            _state.update {
                it.copy(
                    callStatus = CallStatus.Rejected,
                    isIncomingCallScreenVisible = false
                )
            }
            sendSideEffect(CallSideEffect.RejectCall)
            sendSideEffect(CallSideEffect.StopRingtone)
        }
    }

    private fun endCall() {
        val currentStatus = _state.value.callStatus
        Timber.i("Ending Call for phone: ${_state.value.phoneNumber}, Current Status: $currentStatus")

        if (currentStatus in listOf(
                CallStatus.Active,
                CallStatus.Connecting,
                CallStatus.Dialing,
                CallStatus.Ringing,
                CallStatus.OnHold
            )
        ) {
            CallManager.endCall()

            _state.update {
                it.copy(
                    callStatus = CallStatus.Disconnecting,
                    isCallEnding = true
                )
            }

            sendSideEffect(CallSideEffect.EndCall)
            stopCallTimer()

            // Note: We don't manually force Idle/Disconnected immediately anymore.
            // The CallService onCallRemoved / STATE_DISCONNECTED will process the safe UI teardown.
        }
    }

    private fun toggleMute() {
        val newMuteState = !_state.value.isMuted

        if (newMuteState) {
            CallManager.muteCall()
        } else {
            CallManager.unMuteCall()
        }

        _state.update { it.copy(isMuted = newMuteState) }
        sendSideEffect(CallSideEffect.ToggleMuteCall(newMuteState))
        sendSideEffect(CallSideEffect.ShowToast(if (newMuteState) "Microphone muted" else "Microphone unmuted"))
    }

    private fun toggleSpeaker() {
        val newSpeakerState = !_state.value.isSpeakerOn

        CallManager.setSpeakerphone(newSpeakerState)

        _state.update { it.copy(isSpeakerOn = newSpeakerState) }
        sendSideEffect(CallSideEffect.ToggleSpeakerCall(newSpeakerState))

        val newRoute = if (newSpeakerState) AudioRoute.SPEAKER else AudioRoute.EARPIECE
        _state.update { it.copy(audioRoute = newRoute) }

        sendSideEffect(CallSideEffect.ShowToast(if (newSpeakerState) "Speaker on" else "Speaker off"))
    }

    private fun toggleHold() {
        val newHoldState = !_state.value.isOnHold

        if (newHoldState) {
            CallManager.holdCall()
        } else {
            CallManager.unHoldCall()
        }

        _state.update { it.copy(isOnHold = newHoldState) }
        sendSideEffect(CallSideEffect.ToggleHoldCall(newHoldState))

        if (newHoldState) {
            stopCallTimer()
            _state.update { it.copy(callStatus = CallStatus.OnHold) }
        } else {
            startCallTimer()
            _state.update { it.copy(callStatus = CallStatus.Active) }
        }

        sendSideEffect(CallSideEffect.ShowToast(if (newHoldState) "Call on hold" else "Call resumed"))
    }

    private fun toggleRecording() {
        val newRecordingState = !_state.value.isRecording
        _state.update { it.copy(isRecording = newRecordingState) }

        if (newRecordingState) {
            val filePath = "${applicationContext.filesDir}/recording_${System.currentTimeMillis()}.mp3"
            sendSideEffect(CallSideEffect.StartRecording(filePath))
            sendSideEffect(CallSideEffect.ShowToast("Recording started"))
        } else {
            val filePath = "${applicationContext.filesDir}/recording_${System.currentTimeMillis()}.mp3"
            sendSideEffect(CallSideEffect.StopRecording(filePath))
            sendSideEffect(CallSideEffect.ShowToast("Recording saved"))
        }
    }

    private fun changeAudioRoute(route: AudioRoute) {
        val isSpeaker = route == AudioRoute.SPEAKER
        CallManager.setSpeakerphone(isSpeaker)

        _state.update {
            it.copy(
                audioRoute = route,
                isSpeakerOn = isSpeaker
            )
        }
        sendSideEffect(CallSideEffect.ChangeAudioRouteCall(route))
    }

// ========== Contact Actions ==========

    private fun addToContacts(number: String, name: String?) {
        sendSideEffect(CallSideEffect.OpenAddContact(number, name))
    }

    private fun blockNumber(number: String) {
        sendSideEffect(CallSideEffect.OpenBlockNumber(number))
    }

// ========== System Event Handlers ==========

    private fun updateCallState(status: CallStatus, number: String?) {
        val name = number?.let { getContactName(it) }

        _state.update { currentState ->
            currentState.copy(
                callStatus = status,
                phoneNumber = number ?: currentState.phoneNumber,
                contactName = name ?: currentState.contactName
            )
        }

        when (status) {
            CallStatus.Ringing -> {
                _state.update {
                    it.copy(
                        isIncomingCallScreenVisible = true,
                        isCallScreenVisible = false
                    )
                }
                sendSideEffect(CallSideEffect.PlayRingtone(true))
                sendSideEffect(CallSideEffect.Vibrate(1000))
                sendSideEffect(CallSideEffect.NavigateToIncomingCallScreen(number ?: "", name))
            }

            CallStatus.Dialing -> {
                startCallTimer()
                _state.update {
                    it.copy(
                        isCallConnecting = true,
                        isCallScreenVisible = true
                    )
                }
                sendSideEffect(CallSideEffect.NavigateToCallScreen(number ?: "", name))
            }

            CallStatus.Active -> {
                // Initiated from the Telecom Callback safely!
                startCallTimer()
                _state.update {
                    it.copy(
                        isCallConnecting = false,
                        isCallConnected = true,
                        isCallScreenVisible = true,
                        callStartTime = System.currentTimeMillis()
                    )
                }
            }

            CallStatus.Disconnected -> {
                // Graceful cleanup driven by actual disconnected state
                stopCallTimer()
                _state.update {
                    it.copy(
                        isCallConnected = false,
                        isCallEnding = false,
                        isCallScreenVisible = false,
                        isIncomingCallScreenVisible = false
                    )
                }
                sendSideEffect(CallSideEffect.EndCall)

                // Final reset to idle with small visual delay
                viewModelScope.launch {
                    delay(500)
                    _state.update {
                        it.copy(
                            callStatus = CallStatus.Idle,
                            callDuration = 0,
                            callDurationFormatted = "00:00",
                            isMuted = false,
                            isSpeakerOn = false,
                            isOnHold = false,
                            isRecording = false
                        )
                    }
                }
            }

            else -> {}
        }
    }

    private fun updateCallDuration(duration: Int) {
        _state.update {
            it.copy(
                callDuration = duration,
                callDurationFormatted = formatDuration(duration)
            )
        }
        sendSideEffect(CallSideEffect.ShowCallDuration(formatDuration(duration)))
    }

    private fun updateAudioRoute(route: AudioRoute) {
        _state.update { it.copy(audioRoute = route) }
    }

    private fun updateNetworkType(type: NetworkType) {
        _state.update { it.copy(networkType = type) }

        val quality = when (type) {
            NetworkType.WIFI -> CallQuality.GOOD
            NetworkType.CELLULAR_4G, NetworkType.CELLULAR_5G -> CallQuality.GOOD
            NetworkType.CELLULAR_3G -> CallQuality.FAIR
            else -> CallQuality.POOR
        }
        _state.update { it.copy(callQuality = quality) }
    }

    private fun updatePermissions(permissions: Map<String, Boolean>) {
        _state.update {
            it.copy(
                hasPhonePermission = permissions[Manifest.permission.CALL_PHONE] ?: false,
                hasContactsPermission = permissions[Manifest.permission.READ_CONTACTS] ?: false,
                hasMicrophonePermission = permissions[Manifest.permission.RECORD_AUDIO] ?: false,
                hasManageOwnCallsPermission = permissions[Manifest.permission.MANAGE_OWN_CALLS] ?: false
            )
        }
    }

// ========== UI Actions ==========

    private fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun dismissSuccess() {
        _state.update { it.copy(successMessage = null) }
    }

    private fun clearCallScreen() {
        _state.update {
            it.copy(
                isCallScreenVisible = false,
                isIncomingCallScreenVisible = false,
                callStatus = CallStatus.Idle
            )
        }
    }

    private fun retryCall() {
        dismissError()
        initiateCall()
    }

// ========== Call Timer Management ==========

    private fun startCallTimer() {
        if (durationTimerJob?.isActive == true) return

        durationTimerJob = viewModelScope.launch {
            var seconds = _state.value.callDuration
            while (true) {
                delay(1000)
                seconds++
                if (_state.value.callStatus == CallStatus.Active && !_state.value.isOnHold) {
                    sendEvent(CallEvent.CallDurationUpdated(seconds))
                }
            }
        }
    }

    private fun stopCallTimer() {
        durationTimerJob?.cancel()
        durationTimerJob = null
    }

// ========== Call Service Observation ==========

    private fun observeCallService() {
        viewModelScope.launch {
            CallService.currentCall.asFlow().collect { call ->
                if (call != currentCall) {
                    currentCall = call
                    if (call != null) {
                        registerCallCallback(call)
                        updateCallFromTelecomCall(call)
                    } else {
                        handleCallEnded()
                    }
                }
            }
        }
    }

    private fun registerCallCallback(call: Call) {
        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                super.onStateChanged(call, state)
                updateCallFromTelecomCall(call)
            }

            override fun onDetailsChanged(call: Call, details: Call.Details) {
                super.onDetailsChanged(call, details)
                updateCallDetails(call)
            }
        })
    }

    private fun updateCallFromTelecomCall(call: Call) {
        val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            call.details?.state ?: call.state
        } else {
            @Suppress("DEPRECATION")
            call.state
        }

        val number = call.details?.handle?.schemeSpecificPart ?: ""

        val callStatus = when (state) {
            Call.STATE_RINGING -> CallStatus.Ringing
            Call.STATE_DIALING -> CallStatus.Dialing
            Call.STATE_CONNECTING -> CallStatus.Connecting
            Call.STATE_ACTIVE -> CallStatus.Active
            Call.STATE_HOLDING -> CallStatus.OnHold
            Call.STATE_DISCONNECTING -> CallStatus.Disconnecting
            Call.STATE_DISCONNECTED -> CallStatus.Disconnected
            else -> CallStatus.Idle
        }

        sendEvent(CallEvent.CallStateChanged(callStatus, number))
    }

    private fun updateCallDetails(call: Call) {
        val details = call.details ?: return

        val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            details.state
        } else {
            @Suppress("DEPRECATION")
            call.state
        }

        _state.update { it.copy(isOnHold = state == Call.STATE_HOLDING) }
    }

    private fun handleCallEnded() {
        if (wasInCall) {
            wasInCall = false
            stopCallTimer()
            // Make sure the state triggers as disconnected
            updateCallState(CallStatus.Disconnected, null)
        }
    }

// ========== Phone State Listener ==========

    @SuppressLint("ObsoleteSdkInt")
    private fun registerPhoneStateListener() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handlePhoneState(state)
                }
            }
            telephonyManager.registerTelephonyCallback(
                applicationContext.mainExecutor,
                telephonyCallback!!
            )
        } else {
            registerLegacyPhoneStateListener()
        }
    }

    @Suppress("DEPRECATION")
    private fun registerLegacyPhoneStateListener() {
        legacyPhoneStateListener = object : android.telephony.PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                handlePhoneState(state)
            }
        }
        telephonyManager.listen(legacyPhoneStateListener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
    }

    @Suppress("DEPRECATION")
    private fun unregisterLegacyPhoneStateListener() {
        legacyPhoneStateListener?.let {
            telephonyManager.listen(it, android.telephony.PhoneStateListener.LISTEN_NONE)
        }
    }

    private fun handlePhoneState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (!wasInCall && _state.value.callStatus == CallStatus.Idle) {
                    wasInCall = true
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (wasInCall) {
                    wasInCall = false
                    if (_state.value.callStatus != CallStatus.Idle) {
                        endCall()
                    }
                }
            }
        }
    }

// ========== Network Observation ==========

    private fun observeNetworkChanges() {
        viewModelScope.launch {
            delay(1000)
            sendEvent(CallEvent.NetworkChanged(NetworkType.WIFI))
        }
    }

// ========== Permission Management ==========

    private fun checkPermissions() {
        val permissions = mapOf(
            Manifest.permission.CALL_PHONE to (ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED),

            Manifest.permission.READ_CONTACTS to (ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED),

            Manifest.permission.RECORD_AUDIO to (ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED),

            Manifest.permission.MANAGE_OWN_CALLS to (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.MANAGE_OWN_CALLS
                ) == PackageManager.PERMISSION_GRANTED
            } else false)
        )

        sendEvent(CallEvent.PermissionsChanged(permissions))
    }

// ========== Helper Functions ==========

    fun getContactName(number: String): String? {
        if (number.isEmpty()) return null

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        var name: String? = null

        try {
            if (_state.value.hasContactsPermission) {
                applicationContext.contentResolver.query(uri, projection, null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex =
                                cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                            if (nameIndex >= 0) name = cursor.getString(nameIndex)
                        }
                    }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get contact name")
        }
        return name
    }

    private fun formatPhoneNumber(number: String): String {
        if (number.isEmpty()) return ""

        val cleaned = number.replace(Regex("[^\\d]"), "")
        return when {
            cleaned.length <= 7 -> cleaned
            cleaned.length == 10 -> "${cleaned.substring(0, 3)}-${
                cleaned.substring(3, 6)
            }-${cleaned.substring(6)}"

            cleaned.length == 11 && cleaned.startsWith("1") ->
                "+1 ${cleaned.substring(1, 4)}-${cleaned.substring(4, 7)}-${cleaned.substring(7)}"

            else -> number
        }
    }

    private fun formatDuration(seconds: Int): String {
        val minutes = TimeUnit.SECONDS.toMinutes(seconds.toLong())
        val remainingSeconds = seconds - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    private fun sendSideEffect(effect: CallSideEffect) {
        viewModelScope.launch {
            _sideEffect.emit(effect)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopCallTimer()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { telephonyManager.unregisterTelephonyCallback(it) }
        } else {
            unregisterLegacyPhoneStateListener()
        }
    }
}