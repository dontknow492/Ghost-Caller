package com.ghost.caller.service

import android.annotation.SuppressLint
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log


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
            // e.g., currentCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
            currentCall?.answer(0)
            Log.d("CallManager", "Call answered")
        } catch (e: Exception) {
            Log.e("CallManager", "Failed to answer call", e)
        }
    }

    fun rejectCall() {
        try {
            currentCall?.reject(false, null)
            Log.d("CallManager", "Call rejected")
        } catch (e: Exception) {
            Log.e("CallManager", "Failed to reject call", e)
        }
    }

    fun endCall() {
        try {
            currentCall?.disconnect()
            Log.d("CallManager", "Call ended")
        } catch (e: Exception) {
            Log.e("CallManager", "Failed to end call", e)
        }
    }

    fun muteCall() {
        try {
            // Muting is handled by the InCallService, not the individual Call
            callService?.setMuted(true)
            Log.d("CallManager", "Call muted")
        } catch (e: Exception) {
            Log.e("CallManager", "Failed to mute call", e)
        }
    }

    fun unMuteCall() {
        try {
            // Unmuting is handled by the InCallService
            callService?.setMuted(false)
            Log.d("CallManager", "Call unmuted")
        } catch (e: Exception) {
            Log.e("CallManager", "Failed to unmute call", e)
        }
    }

    fun holdCall() {
        try {
            currentCall?.hold()
            Log.d("CallManager", "Call held")
        } catch (e: Exception) {
            Log.e("CallManager", "Failed to hold call", e)
        }
    }

    fun unHoldCall() {
        try {
            currentCall?.unhold()
            Log.d("CallManager", "Call unheld")
        } catch (e: Exception) {
            Log.e("CallManager", "Failed to unhold call", e)
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
                Log.d("CallManager", "Speaker set to: $enabled")
            }
        } catch (e: Exception) {
            Log.e("CallManager", "Failed to set speakerphone", e)
        }
    }
}