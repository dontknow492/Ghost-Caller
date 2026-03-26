package com.ghost.caller.service

import android.telecom.Call
import android.telecom.InCallService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import timber.log.Timber

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
        Timber.tag("CallService").d("CallService created")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        CallManager.setCallService(null)
        Timber.tag("CallService").d("CallService destroyed")
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Timber.tag("CallService").d("Call added: %s", call.details.handle)

        CallManager.setCurrentCall(call)
        _currentCall.postValue(call)

        // Register call state listener
        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                super.onStateChanged(call, state)
                Timber.tag("CallService").d("Call state changed: %d", state)
                handleCallState(call)
            }

            override fun onDetailsChanged(call: Call, details: Call.Details) {
                super.onDetailsChanged(call, details)
                Timber.tag("CallService").d("Call details changed")
            }
        })

        handleCallState(call)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Timber.tag("CallService").d("Call removed")

        if (_currentCall.value == call) {
            _currentCall.postValue(null)
            CallManager.setCurrentCall(null)
        }
    }

    private fun handleCallState(call: Call) {
        when (call.state) {
            Call.STATE_NEW -> {
                Timber.tag("CallService").d("New call")
            }

            Call.STATE_RINGING -> {
                Timber.tag("CallService").d("Incoming call ringing")
            }

            Call.STATE_DIALING -> {
                Timber.tag("CallService").d("Dialing out")
            }

            Call.STATE_ACTIVE -> {
                Timber.tag("CallService").d("Call active")
            }

            Call.STATE_HOLDING -> {
                Timber.tag("CallService").d("Call on hold")
            }

            Call.STATE_DISCONNECTED -> {
                Timber.tag("CallService").d("Call disconnected")
            }
        }
    }
}