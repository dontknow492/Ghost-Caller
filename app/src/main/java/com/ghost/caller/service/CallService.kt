package com.ghost.caller.service

import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class CallService : InCallService() {

    companion object {
        private var instance: CallService? = null

        fun getInstance(): CallService? = instance

        private val _currentCall = MutableLiveData<Call?>()
        val currentCall: LiveData<Call?> = _currentCall
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        CallManager.setCallService(this)
        Log.d("CallService", "CallService created")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        CallManager.setCallService(null)
        Log.d("CallService", "CallService destroyed")
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d("CallService", "Call added: ${call.details.handle}")

        CallManager.setCurrentCall(call)
        _currentCall.postValue(call)

        // Register call state listener
        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                super.onStateChanged(call, state)
                Log.d("CallService", "Call state changed: $state")
                handleCallState(call)
            }

            override fun onDetailsChanged(call: Call, details: Call.Details) {
                super.onDetailsChanged(call, details)
                Log.d("CallService", "Call details changed")
            }
        })

        handleCallState(call)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d("CallService", "Call removed")

        if (_currentCall.value == call) {
            _currentCall.postValue(null)
            CallManager.setCurrentCall(null)
        }
    }

    private fun handleCallState(call: Call) {
        when (call.state) {
            Call.STATE_NEW -> {
                Log.d("CallService", "New call")
            }

            Call.STATE_RINGING -> {
                Log.d("CallService", "Incoming call ringing")
            }

            Call.STATE_DIALING -> {
                Log.d("CallService", "Dialing out")
            }

            Call.STATE_ACTIVE -> {
                Log.d("CallService", "Call active")
            }

            Call.STATE_HOLDING -> {
                Log.d("CallService", "Call on hold")
            }

            Call.STATE_DISCONNECTED -> {
                Log.d("CallService", "Call disconnected")
            }
        }
    }
}