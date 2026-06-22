package com.example.data

import android.content.Context
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object LocalStorage {
    private const val PREFS_NAME = "EchoChatPrefs"
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveLoggedInUser(context: Context, user: User, accessKey: String) {
        val prefs = getPrefs(context)
        val json = moshi.adapter(User::class.java).toJson(user)
        prefs.edit()
            .putString("LOGGED_IN_USER_JSON", json)
            .putString("ACCESS_KEY", accessKey)
            .putLong("ECHO_LOGIN_TIME", System.currentTimeMillis())
            .apply()
    }

    fun getLoggedInUser(context: Context): User? {
        val json = getPrefs(context).getString("LOGGED_IN_USER_JSON", null) ?: return null
        return try {
            moshi.adapter(User::class.java).fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun getAccessKey(context: Context): String? {
        return getPrefs(context).getString("ACCESS_KEY", null)
    }

    fun clearLoggedInUser(context: Context) {
        getPrefs(context).edit()
            .remove("LOGGED_IN_USER_JSON")
            .remove("ACCESS_KEY")
            .remove("ECHO_LOGIN_TIME")
            .apply()
    }

    fun isDarkMode(context: Context): Boolean {
        // default to system, or saved value
        return getPrefs(context).getBoolean("DARK_MODE", false)
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("DARK_MODE", enabled).apply()
    }

    fun getChatTheme(context: Context): String {
        return getPrefs(context).getString("CHAT_THEME", "default") ?: "default"
    }

    fun setChatTheme(context: Context, theme: String) {
        getPrefs(context).edit().putString("CHAT_THEME", theme).apply()
    }

    fun getWallpaper(context: Context): String {
        return getPrefs(context).getString("CHAT_WALLPAPER", "none") ?: "none"
    }

    fun setWallpaper(context: Context, key: String) {
        getPrefs(context).edit().putString("CHAT_WALLPAPER", key).apply()
    }

    fun getFontSize(context: Context): Int {
        return getPrefs(context).getInt("FONT_SIZE", 15)
    }

    fun setFontSize(context: Context, size: Int) {
        getPrefs(context).edit().putInt("FONT_SIZE", size).apply()
    }

    fun isSoundEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("SOUND_ENABLED", true)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("SOUND_ENABLED", enabled).apply()
    }

    fun isAutoScrollLocked(context: Context): Boolean {
        return getPrefs(context).getBoolean("AUTO_SCROLL_LOCKED", false)
    }

    fun setAutoScrollLocked(context: Context, locked: Boolean) {
        getPrefs(context).edit().putBoolean("AUTO_SCROLL_LOCKED", locked).apply()
    }

    fun getSecurityPIN(context: Context): String? {
        return getPrefs(context).getString("CHAT_LOCK_PIN", null)
    }

    fun setSecurityPIN(context: Context, pin: String?) {
        getPrefs(context).edit().putString("CHAT_LOCK_PIN", pin).apply()
    }

    // Secured chats list (stores map of chat email to password lock)
    fun getSecuredChats(context: Context): Map<String, String> {
        val json = getPrefs(context).getString("SECURED_CHATS_JSON", null) ?: return emptyMap()
        return try {
            val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
            moshi.adapter<Map<String, String>>(type).fromJson(json) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveSecuredChatPassword(context: Context, email: String, pass: String?) {
        val current = getSecuredChats(context).toMutableMap()
        if (pass == null) {
            current.remove(email)
        } else {
            current[email] = pass
        }
        val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
        val json = moshi.adapter<Map<String, String>>(type).toJson(current)
        getPrefs(context).edit().putString("SECURED_CHATS_JSON", json).apply()
    }

    // Unread message counts map
    fun getUnreadCounts(context: Context): Map<String, Int> {
        val json = getPrefs(context).getString("UNREAD_COUNTS_JSON", null) ?: return emptyMap()
        return try {
            val type = Types.newParameterizedType(Map::class.java, String::class.java, Int::class.javaObjectType)
            moshi.adapter<Map<String, Int>>(type).fromJson(json) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveUnreadCounts(context: Context, counts: Map<String, Int>) {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Int::class.javaObjectType)
        val json = moshi.adapter<Map<String, Int>>(type).toJson(counts)
        getPrefs(context).edit().putString("UNREAD_COUNTS_JSON", json).apply()
    }

    // Starred Messages
    fun getStarredMessages(context: Context): Map<String, StarMessageData> {
        val json = getPrefs(context).getString("STARRED_MESSAGES_JSON", null) ?: return emptyMap()
        return try {
            val type = Types.newParameterizedType(Map::class.java, String::class.java, StarMessageData::class.java)
            moshi.adapter<Map<String, StarMessageData>>(type).fromJson(json) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveStarredMessages(context: Context, starred: Map<String, StarMessageData>) {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, StarMessageData::class.java)
        val json = moshi.adapter<Map<String, StarMessageData>>(type).toJson(starred)
        getPrefs(context).edit().putString("STARRED_MESSAGES_JSON", json).apply()
    }

    // Conversation Notes
    fun getConversationNotes(context: Context): Map<String, String> {
        val json = getPrefs(context).getString("CONVERSATION_NOTES_JSON", null) ?: return emptyMap()
        return try {
            val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
            moshi.adapter<Map<String, String>>(type).fromJson(json) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveConversationNotes(context: Context, notes: Map<String, String>) {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
        val json = moshi.adapter<Map<String, String>>(type).toJson(notes)
        getPrefs(context).edit().putString("CONVERSATION_NOTES_JSON", json).apply()
    }

    // Bookmark folder structure
    fun getBookmarkFolders(context: Context): Map<String, List<FolderBookmarkData>> {
        val json = getPrefs(context).getString("BOOKMARK_FOLDERS_JSON", null)
        if (json == null) {
            // Pre-seed default folders
            return mapOf("💼 Work" to emptyList(), "❤️ Important" to emptyList(), "😂 Funny" to emptyList())
        }
        return try {
            val type = Types.newParameterizedType(
                Map::class.java, String::class.java,
                Types.newParameterizedType(List::class.java, FolderBookmarkData::class.java)
            )
            moshi.adapter<Map<String, List<FolderBookmarkData>>>(type).fromJson(json) ?: emptyMap()
        } catch (e: Exception) {
            mapOf("💼 Work" to emptyList(), "❤️ Important" to emptyList(), "😂 Funny" to emptyList())
        }
    }

    fun saveBookmarkFolders(context: Context, folders: Map<String, List<FolderBookmarkData>>) {
        val type = Types.newParameterizedType(
            Map::class.java, String::class.java,
            Types.newParameterizedType(List::class.java, FolderBookmarkData::class.java)
        )
        val json = moshi.adapter<Map<String, List<FolderBookmarkData>>>(type).toJson(folders)
        getPrefs(context).edit().putString("BOOKMARK_FOLDERS_JSON", json).apply()
    }

    // Local Message Reactions
    fun getMessageReactions(context: Context): Map<String, Map<String, Int>> {
        val json = getPrefs(context).getString("MSG_REACTIONS_JSON2", null) ?: return emptyMap()
        return try {
            val type = Types.newParameterizedType(
                Map::class.java, String::class.java,
                Types.newParameterizedType(Map::class.java, String::class.java, Int::class.javaObjectType)
            )
            moshi.adapter<Map<String, Map<String, Int>>>(type).fromJson(json) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveMessageReactions(context: Context, reactions: Map<String, Map<String, Int>>) {
        val type = Types.newParameterizedType(
            Map::class.java, String::class.java,
            Types.newParameterizedType(Map::class.java, String::class.java, Int::class.javaObjectType)
        )
        val json = moshi.adapter<Map<String, Map<String, Int>>>(type).toJson(reactions)
        getPrefs(context).edit().putString("MSG_REACTIONS_JSON2", json).apply()
    }

    // Auto Dark schedule
    fun isDarkModeScheduleEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("DARK_MODE_SCHEDULE_ENABLED", false)
    }

    fun setDarkModeScheduleEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("DARK_MODE_SCHEDULE_ENABLED", enabled).apply()
    }

    // Device current sessionId
    fun getSessionId(context: Context): String {
        var sid = getPrefs(context).getString("SESSION_ID", null)
        if (sid == null) {
            sid = "sess_" + System.currentTimeMillis() + "_" + (1000..9999).random()
            getPrefs(context).edit().putString("SESSION_ID", sid).apply()
        }
        return sid
    }

    // Deleted conversations list support
    fun getDeletedConversations(context: Context): Set<String> {
        return getPrefs(context).getStringSet("DELETED_CONVERSATIONS", emptySet()) ?: emptySet()
    }

    fun deleteConversation(context: Context, email: String) {
        val current = getDeletedConversations(context).toMutableSet()
        current.add(email)
        getPrefs(context).edit().putStringSet("DELETED_CONVERSATIONS", current).apply()
    }

    fun restoreConversation(context: Context, email: String) {
        val current = getDeletedConversations(context).toMutableSet()
        current.remove(email)
        getPrefs(context).edit().putStringSet("DELETED_CONVERSATIONS", current).apply()
    }
}

@JsonClass(generateAdapter = true)
data class StarMessageData(
    val text: String,
    val ts: Long
)

@JsonClass(generateAdapter = true)
data class FolderBookmarkData(
    val id: String,
    val text: String,
    val ts: Long
)
