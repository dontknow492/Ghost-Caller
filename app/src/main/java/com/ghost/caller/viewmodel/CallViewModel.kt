@file:Suppress("D")

package com.ghost.caller.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallViewModel(application: Application) : AndroidViewModel(application) {

    val phoneUtil = PhoneNumberUtil.getInstance()
    val geocoder = PhoneNumberOfflineGeocoder.getInstance()
    val userCountryCode = Locale.getDefault().country

    private val applicationContext = application
    private val telephonyManager =
        applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    // --- State Flows remain exactly the same ---
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _dialedNumber = MutableStateFlow("")
    val dialedNumber: StateFlow<String> = _dialedNumber.asStateFlow()

    private val _callDuration = MutableStateFlow(0)
    val callDuration: StateFlow<Int> = _callDuration.asStateFlow()

    private val _recentCalls = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val recentCalls: StateFlow<List<CallLogEntry>> = _recentCalls.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isOnHold = MutableStateFlow(false)
    val isOnHold: StateFlow<Boolean> = _isOnHold.asStateFlow()

    // Variable to track if we were actually in a call before going IDLE
    private var wasInCall = false

    private var telephonyCallback: TelephonyCallback? = null
    private var phoneStateListener: PhoneStateListener? = null


    private val _searchQuery = MutableStateFlow<String>("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()


    val recentCallsFiltered = _recentCalls.combine(_searchQuery) { calls, query ->
        if (query.isBlank()) {
            calls
        } else {
            calls.filter { call ->
                call.number.contains(query, ignoreCase = true) ||
                        call.name?.contains(query, ignoreCase = true) == true
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )


    init {
        // Start listening to phone state changes right away
        registerPhoneStateListener()
    }

    private fun registerPhoneStateListener() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("CallViewModel", "Phone state permission not granted")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.i("CallViewModel", "Using TelephonyCallback for Android 12+")

            // 2. Assign the object to your class-level variable first
            telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handlePhoneState(state)
                }
            }

            // 3. Pass that variable to the manager
            telephonyManager.registerTelephonyCallback(
                applicationContext.mainExecutor,
                telephonyCallback!! // Now it won't be garbage collected!
            )
        } else {
            Log.i("CallViewModel", "Using PhoneStateListener for older Android versions")

            // Do the same for the older listener
            phoneStateListener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handlePhoneState(state)
                }
            }

            @Suppress("DEPRECATION")
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    // 4. Good Practice: Clean up when the ViewModel dies
    override fun onCleared() {
        super.onCleared()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { telephonyManager.unregisterTelephonyCallback(it) }
        } else {
            @Suppress("DEPRECATION")
            phoneStateListener?.let { telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE) }
        }
    }

    private fun handlePhoneState(state: Int) {
        Log.i("CallViewModel", "Phone state changed: $state")
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                Log.i("CallViewModel", "Phone is OFFHOOK (In Call)")
                wasInCall = true
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                Log.i("CallViewModel", "Phone is IDLE (Hung up)")
                if (wasInCall) {
                    wasInCall = false
                    endCall()

                    viewModelScope.launch {
                        delay(1000)
                        loadRecentCalls()
                    }
                }
            }

            TelephonyManager.CALL_STATE_RINGING -> {
                Log.i("CallViewModel", "Phone is RINGING")
            }
        }
    }

    fun getContactName(number: String): String? {
        // ... (Your existing getContactName code)
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        var name: String? = null
        try {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.READ_CONTACTS
                )
                == PackageManager.PERMISSION_GRANTED
            ) {
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
            e.printStackTrace()
        }
        return name
    }

    // ... (appendDigit, deleteDigit, setNumber, clearDialedNumber remain the same) ...
    fun appendDigit(digit: String) {
        if (_dialedNumber.value.length < 15) _dialedNumber.value += digit
    }

    fun deleteDigit() {
        if (_dialedNumber.value.isNotEmpty()) _dialedNumber.value = _dialedNumber.value.dropLast(1)
    }

    fun setNumber(number: String) {
        _dialedNumber.value = number
    }

    fun clearDialedNumber() {
        _dialedNumber.value = ""
    }

    fun initiateCall(context: Context, makeRealCall: Boolean = true) {
        if (_dialedNumber.value.isEmpty()) return
        val name = getContactName(_dialedNumber.value)
        _callState.value = CallState.Outgoing(_dialedNumber.value, name)

        if (makeRealCall) {
            try {
                // Note: The listener will catch when this call actually ends!
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:${_dialedNumber.value}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: SecurityException) {
                simulateAnswer()
            }
        } else {
            simulateAnswer()
        }
    }

    private fun simulateAnswer() {
        viewModelScope.launch {
            delay(3000)
            if (_callState.value is CallState.Outgoing) {
                val outgoing = _callState.value as CallState.Outgoing
                _callState.value = CallState.Active(outgoing.number, outgoing.name)
            }
        }
    }

    fun acceptCall() {
        if (_callState.value is CallState.Incoming) {
            val incoming = _callState.value as CallState.Incoming
            _callState.value = CallState.Active(incoming.number, incoming.name)
        }
    }

    fun endCall() {
        _callState.value = CallState.Idle
        _dialedNumber.value = ""
        _callDuration.value = 0
        _isMuted.value = false
        _isSpeakerOn.value = false
        _isRecording.value = false
        _isOnHold.value = false
    }

    // ... (Your existing toggle and loadRecentCalls code remains exactly the same) ...
    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
    }

    fun toggleRecording() {
        _isRecording.value = !_isRecording.value
    }

    fun toggleHold() {
        _isOnHold.value = !_isOnHold.value
    }

    fun incrementDuration() {
        if (!_isOnHold.value && _callState.value is CallState.Active) _callDuration.value++
    }

    fun resetDuration() {
        _callDuration.value = 0
    }

    fun loadRecentCalls() {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CALL_LOG)
            == PackageManager.PERMISSION_GRANTED
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                val calls = mutableListOf<CallLogEntry>()

                val projection = arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.DATE,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.IS_READ,
                    CallLog.Calls.GEOCODED_LOCATION,
                    CallLog.Calls.FEATURES
                )

                // 1. FIXED: Removed the "LIMIT 100" hack to prevent SQL Injection crashes
                val sortOrder = "${CallLog.Calls.DATE} DESC"

                val cursor = try {
                    applicationContext.contentResolver.query(
                        CallLog.Calls.CONTENT_URI,
                        projection,
                        null,
                        null,
                        sortOrder
                    )
                } catch (e: Exception) {
                    // Better logging so you can see the exact error message if it fails
                    Log.e("CallViewModel", "Failed to query CallLog database", e)
                    null
                }

                cursor?.use {
                    val numberIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                    val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                    val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
                    val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
                    val durationIdx = it.getColumnIndex(CallLog.Calls.DURATION)
                    val isReadIdx = it.getColumnIndex(CallLog.Calls.IS_READ)
                    val locationIdx = it.getColumnIndex(CallLog.Calls.GEOCODED_LOCATION)
                    val featuresIdx = it.getColumnIndex(CallLog.Calls.FEATURES)

                    // 2. FIXED: Brought back your manual count limit (Safe for all Android versions)
                    var count = 0
                    val maxCount = 100
















                    while (it.moveToNext() && count < maxCount) {
                        val number =
                            if (numberIdx >= 0) it.getString(numberIdx) ?: continue else continue


                        var resolvedLocation: String? = null
                        try {
                            // Parse the number. If it lacks a '+', it assumes it belongs to userCountryCode
                            val parsedNumber = phoneUtil.parse(number, userCountryCode)

                            // Get the region/city (e.g., "California" or "London")
                            val geoDesc =
                                geocoder.getDescriptionForNumber(parsedNumber, Locale.getDefault())

                            if (geoDesc.isNotBlank()) {
                                resolvedLocation = geoDesc
                            } else {
                                // Fallback: If it's a valid number but no city is known, just grab the Country name
                                val regionCode = phoneUtil.getRegionCodeForNumber(parsedNumber)
                                if (regionCode != null) {
                                    resolvedLocation = Locale("", regionCode).displayCountry
                                }
                            }
                        } catch (e: Exception) {
                            // If parsing fails (e.g. weird shortcodes), fallback to Android's basic geocoded column
                            resolvedLocation =
                                if (locationIdx >= 0) it.getString(locationIdx) else null
                        }


                        val type = if (typeIdx >= 0) {
                            when (it.getInt(typeIdx)) {
                                CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                                CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                                CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                                CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
                                CallLog.Calls.BLOCKED_TYPE -> CallType.BLOCKED
                                CallLog.Calls.VOICEMAIL_TYPE -> CallType.VOICEMAIL
                                else -> CallType.UNKNOWN
                            }
                        } else CallType.UNKNOWN

                        val features = if (featuresIdx >= 0) it.getInt(featuresIdx) else 0
                        val isVideo =
                            (features and CallLog.Calls.FEATURES_VIDEO) == CallLog.Calls.FEATURES_VIDEO

                        val entry = CallLogEntry(
                            number = number,
                            name = if (nameIdx >= 0) it.getString(nameIdx) else null,
                            timestamp = if (dateIdx >= 0) it.getLong(dateIdx) else 0L,
                            type = type,
                            durationSeconds = if (durationIdx >= 0) it.getLong(durationIdx) else 0L,
                            isRead = if (isReadIdx >= 0) it.getInt(isReadIdx) == 1 else true,
                            location = resolvedLocation,
                            isVideoCall = isVideo,
                            groupedCount = 1
                        )

                        val lastEntry = calls.lastOrNull()
                        if (lastEntry != null && lastEntry.number == entry.number && lastEntry.type == entry.type) {
                            lastEntry.groupedCount++
                        } else {
                            calls.add(entry)
                            count++ // Only increment the count for unique UI rows
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    _recentCalls.value = calls
                }
            }
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    fun deleteCallLog(call: CallLogEntry) {
        // 1. CRITICAL: You must have WRITE_CALL_LOG permission to do this!
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.WRITE_CALL_LOG
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    // 2. We target the specific row by matching the number and exact time
                    val selection = "${CallLog.Calls.NUMBER} = ? AND ${CallLog.Calls.DATE} = ?"
                    val selectionArgs = arrayOf(call.number, call.timestamp.toString())

                    // 3. Execute the deletion
                    val deletedRows = applicationContext.contentResolver.delete(
                        CallLog.Calls.CONTENT_URI,
                        selection,
                        selectionArgs
                    )

                    // 4. If it successfully deleted the row, refresh our UI list!
                    if (deletedRows > 0) {
                        Log.i("CallViewModel", "Successfully deleted call log.")
                        loadRecentCalls()
                    } else {
                        Log.w("CallViewModel", "Could not find that exact call to delete.")
                    }

                } catch (e: Exception) {
                    Log.e("CallViewModel", "Failed to delete call log", e)
                }
            }
        } else {
            Log.e("CallViewModel", "Missing WRITE_CALL_LOG permission!")
        }
    }

    fun filterValueChange(value: String){
        _searchQuery.update {
            value
        }
    }


}

// ... (Your sealed classes and data classes remain the same) ...