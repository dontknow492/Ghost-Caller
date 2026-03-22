package com.ghost.caller.viewmodel

// --- STATE MANAGEMENT ---
sealed class CallState {
    object Idle : CallState()
    data class Outgoing(val number: String, val name: String?) : CallState()
    data class Incoming(val number: String, val name: String?) : CallState()
    data class Active(val number: String, val name: String?) : CallState()
}


enum class CallType {
    INCOMING, OUTGOING, MISSED, REJECTED, BLOCKED, VOICEMAIL, UNKNOWN
}

// 2. Add the new fields to your Data Class
data class CallLogEntry(
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