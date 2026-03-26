package com.ghost.caller.service

import android.annotation.SuppressLint
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import timber.log.Timber

@SuppressLint("StaticFieldLeak")
object CallManager {
    private var currentCall: Call? = null
    private var callService: InCallService? = null

    fun setCallService(service: InCallService?) {
        callService = service
    }

    fun setCurrentCall(call: Call?) {
        currentCall = call
    }

    fun getCurrentCall(): Call? = currentCall

    fun answerCall() {
        try {
            // Note: Depending on your exact API level, answer() might require a videoState Int parameter
            currentCall?.answer(0)
            Timber.tag("CallManager").d("Call answered")
        } catch (e: Exception) {
            Timber.tag("CallManager").e(e, "Failed to answer call")
        }
    }

    fun rejectCall() {
        try {
            currentCall?.reject(false, null)
            Timber.tag("CallManager").d("Call rejected")
        } catch (e: Exception) {
            Timber.tag("CallManager").e(e, "Failed to reject call")
        }
    }

    fun endCall() {
        try {
            currentCall?.disconnect()
            Timber.tag("CallManager").d("Call ended")
        } catch (e: Exception) {
            Timber.tag("CallManager").e(e, "Failed to end call")
        }
    }

    fun muteCall() {
        try {
            // Muting is handled by the InCallService, not the individual Call
            callService?.setMuted(true)
            Timber.tag("CallManager").d("Call muted")
        } catch (e: Exception) {
            Timber.tag("CallManager").e(e, "Failed to mute call")
        }
    }

    fun unMuteCall() {
        try {
            // Unmuting is handled by the InCallService
            callService?.setMuted(false)
            Timber.tag("CallManager").d("Call unmuted")
        } catch (e: Exception) {
            Timber.tag("CallManager").e(e, "Failed to unmute call")
        }
    }

    fun holdCall() {
        try {
            currentCall?.hold()
            Timber.tag("CallManager").d("Call held")
        } catch (e: Exception) {
            Timber.tag("CallManager").e(e, "Failed to hold call")
        }
    }

    fun unHoldCall() {
        try {
            currentCall?.unhold()
            Timber.tag("CallManager").d("Call unheld")
        } catch (e: Exception) {
            Timber.tag("CallManager").e(e, "Failed to unhold call")
        }
    }

    @Suppress("DEPRECATION")
    fun setSpeakerphone(enabled: Boolean) {
        try {
            callService?.let { service ->
                // Audio routes are defined in CallAudioState
                val route = if (enabled) {
                    CallAudioState.ROUTE_SPEAKER
                } else {
                    CallAudioState.ROUTE_EARPIECE
                }

                service.setAudioRoute(route)
                Timber.tag("CallManager").d("Speaker set to: %b", enabled)
            }
        } catch (e: Exception) {
            Timber.tag("CallManager").e(e, "Failed to set speakerphone")
        }
    }
}