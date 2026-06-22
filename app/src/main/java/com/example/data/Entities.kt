package com.example.data

import com.squareup.moshi.JsonClass
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

@JsonClass(generateAdapter = true)
data class User(
    val email: String,
    val name: String,
    val photoUrl: String? = ""
)

@JsonClass(generateAdapter = true)
data class MessageRaw(
    val id: String? = null,
    val user: String? = null, // Holds string like "Md. Rafid"
    val sender: String? = null, // Holds sender's email
    val message: String,
    val timestamp: String,
    val participants: Any? = null, // can be List<String> or stringified JSON Array
    val replyTo: String? = null // optional stringified JSON
) {
    fun getParticipantsList(): List<String> {
        if (participants == null) return emptyList()
        if (participants is List<*>) {
            return participants.mapNotNull { it?.toString() }
        }
        if (participants is String) {
            try {
                val arr = JSONArray(participants)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                return list
            } catch (e: Exception) {
                // Not a valid JSON array, treat as single item or comma-separated
                if (participants.contains(",")) return participants.split(",")
            }
        }
        return emptyList()
    }

    fun getReplyToMessage(): ReplyToData? {
        if (replyTo.isNullOrEmpty()) return null
        return try {
            val obj = JSONObject(replyTo)
            ReplyToData(
                id = obj.optString("id"),
                text = obj.optString("text"),
                user = obj.optString("user")
            )
        } catch (e: Exception) {
            null
        }
    }
}

data class ReplyToData(
    val id: String,
    val text: String,
    val user: String
)

data class ChatMessage(
    val id: String,
    val senderName: String,
    val senderEmail: String,
    val text: String,
    val timestampMs: Long,
    val replyTo: ReplyToData? = null,
    val isOwn: Boolean = false,
    val isLocal: Boolean = false,
    val isImage: Boolean = false,
    val imageUrl: String? = null,
    val isPoll: Boolean = false,
    val pollQuestion: String? = null,
    val pollOptions: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val status: String,
    val message: String? = null,
    val user: User? = null
)

@JsonClass(generateAdapter = true)
data class CommonResponse(
    val status: String,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class GetUsersResponse(
    val status: String,
    val users: List<User>? = null
)

@JsonClass(generateAdapter = true)
data class PairRequest(
    val fromEmail: String,
    val fromName: String? = null
)

@JsonClass(generateAdapter = true)
data class CheckPairResponse(
    val status: String,
    val request: PairRequest? = null
)

@JsonClass(generateAdapter = true)
data class PartnerCodeResponse(
    val status: String,
    val code: String? = null,
    val forEmail: String? = null,
    val forName: String? = null
)

@JsonClass(generateAdapter = true)
data class PairStatusResponse(
    val status: String,
    val partner: String? = null
)

// Firebase Database REST entities
data class FirebaseOnlineStatus(
    val status: String, // "online" | "away" | "offline"
    val ts: Long
)

data class FirebaseTypingStatus(
    val name: String,
    val ts: Long
)

data class FirebaseSession(
    val sessionId: String,
    val userId: String,
    val deviceModel: String,
    val os: String,
    val browser: String,
    val deviceType: String,
    val loginAt: Long,
    val lastActive: Long,
    val active: Boolean,
    val terminated: Boolean? = false
)

data class FirebaseActiveSession(
    val sessionId: String,
    val loginAt: Long,
    val deviceModel: String,
    val os: String
)

data class FirebaseCallSignaling(
    val callerId: String? = null,
    val callerName: String? = null,
    val calleeId: String? = null,
    val callType: String? = null, // "audio" | "video"
    val status: String? = null, // "calling" | "accepted" | "rejected" | "ended"
    val ts: Long? = null,
    val offer: FirebaseSdp? = null,
    val answer: FirebaseSdp? = null,
    val candidates: FirebaseCandidates? = null
)

data class FirebaseSdp(
    val type: String,
    val sdp: String
)

data class FirebaseCandidates(
    val caller: Map<String, Map<String, Any>>? = null,
    val callee: Map<String, Map<String, Any>>? = null
)
