package com.example.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class EchoChatViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val scriptUrl = "https://script.google.com/macros/s/AKfycbzvqNxH0BGFuXbIvJPMDR6uqUkWvekQvS8asurlYnRoT23lMCZq9NLmLoO4ohje_3Otbg/exec"

    // Authentication States
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Dashboard & Chat States
    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    private val _recentChats = MutableStateFlow<List<User>>(emptyList())
    val recentChats: StateFlow<List<User>> = _recentChats.asStateFlow()

    private val _currentChatUser = MutableStateFlow<User?>(null)
    val currentChatUser: StateFlow<User?> = _currentChatUser.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    // Extra Chat Statuses
    private val _typingPartnerName = MutableStateFlow<String?>(null)
    val typingPartnerName: StateFlow<String?> = _typingPartnerName.asStateFlow()

    private val _partnerOnlineStatus = MutableStateFlow("offline") // "online" | "away" | "offline"
    val partnerOnlineStatus: StateFlow<String> = _partnerOnlineStatus.asStateFlow()

    // Offline Persistent Configurations
    private val _isDarkMode = MutableStateFlow(LocalStorage.isDarkMode(context))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _chatTheme = MutableStateFlow(LocalStorage.getChatTheme(context))
    val chatTheme: StateFlow<String> = _chatTheme.asStateFlow()

    private val _chatWallpaper = MutableStateFlow(LocalStorage.getWallpaper(context))
    val chatWallpaper: StateFlow<String> = _chatWallpaper.asStateFlow()

    private val _fontSize = MutableStateFlow(LocalStorage.getFontSize(context))
    val fontSize: StateFlow<Int> = _fontSize.asStateFlow()

    private val _soundEnabled = MutableStateFlow(LocalStorage.isSoundEnabled(context))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _autoScrollLocked = MutableStateFlow(LocalStorage.isAutoScrollLocked(context))
    val autoScrollLocked: StateFlow<Boolean> = _autoScrollLocked.asStateFlow()

    private val _unreadCounts = MutableStateFlow<Map<String, Int>>(LocalStorage.getUnreadCounts(context))
    val unreadCounts: StateFlow<Map<String, Int>> = _unreadCounts.asStateFlow()

    private val _securedChats = MutableStateFlow<Map<String, String>>(LocalStorage.getSecuredChats(context))
    val securedChats: StateFlow<Map<String, String>> = _securedChats.asStateFlow()

    private val _deletedConversations = MutableStateFlow<Set<String>>(LocalStorage.getDeletedConversations(context))
    val deletedConversations: StateFlow<Set<String>> = _deletedConversations.asStateFlow()

    private val _starredMessages = MutableStateFlow<Map<String, StarMessageData>>(LocalStorage.getStarredMessages(context))
    val starredMessages: StateFlow<Map<String, StarMessageData>> = _starredMessages.asStateFlow()

    private val _bookmarkFolders = MutableStateFlow<Map<String, List<FolderBookmarkData>>>(LocalStorage.getBookmarkFolders(context))
    val bookmarkFolders: StateFlow<Map<String, List<FolderBookmarkData>>> = _bookmarkFolders.asStateFlow()

    private val _conversationNotes = MutableStateFlow<Map<String, String>>(LocalStorage.getConversationNotes(context))
    val conversationNotes: StateFlow<Map<String, String>> = _conversationNotes.asStateFlow()

    private val _messageReactions = MutableStateFlow<Map<String, Map<String, Int>>>(LocalStorage.getMessageReactions(context))
    val messageReactions: StateFlow<Map<String, Map<String, Int>>> = _messageReactions.asStateFlow()

    // Single-device login, active checks, remote sessions
    private val _activeSessions = MutableStateFlow<List<FirebaseSession>>(emptyList())
    val activeSessions: StateFlow<List<FirebaseSession>> = _activeSessions.asStateFlow()

    private val _sessionTerminated = MutableStateFlow(false)
    val sessionTerminated: StateFlow<Boolean> = _sessionTerminated.asStateFlow()

    // Forgot password recover state
    private val _forgotResetTimerSecs = MutableStateFlow(30)
    val forgotResetTimerSecs: StateFlow<Int> = _forgotResetTimerSecs.asStateFlow()

    // Pair recovers
    private val _pairPartnerName = MutableStateFlow("")
    private val _pairPartnerEmail = MutableStateFlow("")
    val pairPartnerName: StateFlow<String> = _pairPartnerName.asStateFlow()
    val pairPartnerEmail: StateFlow<String> = _pairPartnerEmail.asStateFlow()

    private val _incomingPairRequest = MutableStateFlow<PairRequest?>(null)
    val incomingPairRequest: StateFlow<PairRequest?> = _incomingPairRequest.asStateFlow()

    private val _partnerResetCode = MutableStateFlow<String?>(null)
    val partnerResetCode: StateFlow<String?> = _partnerResetCode.asStateFlow()

    // Voice Calling States
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _callDuration = MutableStateFlow(0)
    val callDuration: StateFlow<Int> = _callDuration.asStateFlow()

    private val _isCallMuted = MutableStateFlow(false)
    val isCallMuted: StateFlow<Boolean> = _isCallMuted.asStateFlow()

    private val _isCallCameraOff = MutableStateFlow(false)
    val isCallCameraOff: StateFlow<Boolean> = _isCallCameraOff.asStateFlow()

    private var autoRefreshJob: Job? = null
    private var firebaseSyncJob: Job? = null
    private var callTimerJob: Job? = null
    private var forgotTimerJob: Job? = null

    // Track last raw msg size or timestamps to notify only on genuine incoming
    private var lastMessagesCount = 0

    init {
        // Attempt Autologin
        val savedUser = LocalStorage.getLoggedInUser(context)
        if (savedUser != null) {
            _currentUser.value = savedUser
            startSynchronization()
        }
    }

    fun login(email: String, accessKey: String, onSuccess: () -> Unit) {
        if (email.isEmpty() || accessKey.isEmpty()) {
            _authError.value = "User ID এবং পাসওয়ার্ড প্রয়োজন!"
            return
        }
        _authLoading.value = true
        _authError.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = RetrofitClient.echoChatApi.login(scriptUrl, email = email, oldPassword = accessKey)
                if (res.status == "success" && res.user != null) {
                    if (res.user.name.endsWith("#")) {
                        withContext(Dispatchers.Main) {
                            _authError.value = "Access Denied: This account is deactivated."
                            _authLoading.value = false
                        }
                        return@launch
                    }
                    LocalStorage.saveLoggedInUser(context, res.user, accessKey)
                    withContext(Dispatchers.Main) {
                        _currentUser.value = res.user
                        _authLoading.value = false
                        startSynchronization()
                        onSuccess()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _authError.value = res.message ?: "লগইন ব্যর্থ হয়েছে। পুনরায় চেষ্টা করুন।"
                        _authLoading.value = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _authError.value = "নেটওয়ার্ক ত্রুটি: ${e.localizedMessage}"
                    _authLoading.value = false
                }
            }
        }
    }

    fun register(name: String, email: String, accessKey: String, base64Photo: String, onSuccess: () -> Unit) {
        if (name.isEmpty() || email.isEmpty() || accessKey.isEmpty()) {
            _authError.value = "সবগুলো ঘর পূরণ করা আবশ্যক!"
            return
        }
        _authError.value = null
        _authLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Upload photo if selected
                var uploadedDriveUrl = ""
                if (base64Photo.isNotEmpty()) {
                    val photoRes = RetrofitClient.echoChatApi.uploadPhoto(scriptUrl, email = email, base64PhotoData = base64Photo)
                    if (photoRes.status == "success" && photoRes.message != null) {
                        uploadedDriveUrl = photoRes.message
                    }
                }

                val registerRes = RetrofitClient.echoChatApi.register(
                    url = scriptUrl,
                    name = name,
                    email = email,
                    oldPassword = accessKey,
                    photoUrl = uploadedDriveUrl
                )
                if (registerRes.status == "success") {
                    withContext(Dispatchers.Main) {
                        _authLoading.value = false
                        onSuccess()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _authError.value = registerRes.message ?: "রেজিস্ট্রেশন ব্যর্থ হয়েছে।"
                        _authLoading.value = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _authError.value = "নেটওয়ার্ক ত্রুটি: ${e.localizedMessage}"
                    _authLoading.value = false
                }
            }
        }
    }

    fun logout() {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            updateFirebaseOnline("offline")
            // Clean up session history
            val userKey = sanitizeId(user.email)
            val sid = LocalStorage.getSessionId(context)
            try {
                FirebaseRestClient.service.deleteValue("sessions/$userKey/history/$sid")
            } catch (e: Exception) {}
        }
        LocalStorage.clearLoggedInUser(context)
        _currentUser.value = null
        stopSynchronization()
    }

    // Synchronized Background Polling Processes
    private fun startSynchronization() {
        val user = _currentUser.value ?: return
        stopSynchronization()

        // Sync initial files
        loadAllConversationsAndUsers()

        // 1. periodic polling of messages and users
        autoRefreshJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val currentChat = _currentChatUser.value
                if (currentChat != null) {
                    loadMessagesForConversation(currentChat.email)
                }
                loadAllConversationsAndUsers()
                delay(4000) // Poll every 4 seconds
            }
        }

        // 2. Realtime firebase REST sync
        firebaseSyncJob = viewModelScope.launch(Dispatchers.IO) {
            // Write session parameters
            registerSession(user)
            updateFirebaseOnline("online")

            while (isActive) {
                // Online sync ping
                updateFirebaseOnline("online")
                checkActiveSessionConflict(user)
                pollPairRequestsAndRecoveryCode(user)
                pollCallSignalRooms(user)
                pollAndSyncFirebaseStatuses()
                delay(5000)
            }
        }

        // 3. Setup system theme preference schedules
        if (LocalStorage.isDarkModeScheduleEnabled(context)) {
            setupThemeSchedule()
        }
    }

    private fun stopSynchronization() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
        firebaseSyncJob?.cancel()
        firebaseSyncJob = null
        stopCallTimer()
        _forgotResetTimerSecs.value = 30
    }

    // Message lists processing
    private fun loadAllConversationsAndUsers() {
        val current = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Read deleted set
                val deleted = _deletedConversations.value

                // Load all messages
                val rawMessages = RetrofitClient.echoChatApi.getMessages(scriptUrl).filter { msg ->
                    val parts = msg.getParticipantsList()
                    parts.all { !deleted.contains(it) }
                }

                // Load all registered users
                val userRes = RetrofitClient.echoChatApi.getUsers(scriptUrl)
                if (userRes.status == "success" && userRes.users != null) {
                    val currentSecured = _securedChats.value
                    val currentUserName = current.name.replace(Regex("\\[\\{.*?\\}\\(.*\\)\\]"), "").trim()

                    val activeUsers = userRes.users.filter { u ->
                        u.email != current.email && !u.name.endsWith("®") && !deleted.contains(u.email)
                    }.map { u ->
                        val lockStr = currentSecured[u.email]
                        if (lockStr != null) {
                            val baseName = u.name.replace(Regex("\\[\\{.*?\\}\\(.*\\)\\]"), "").trim()
                            val finalLockPart = if (lockStr.contains("[{")) lockStr else "[{$lockStr}($currentUserName)]"
                            u.copy(name = "$baseName $finalLockPart")
                        } else {
                            val baseName = u.name.replace(Regex("\\[\\{.*?\\}\\(.*\\)\\]"), "").trim()
                            u.copy(name = baseName)
                        }
                    }
                    _allUsers.value = activeUsers

                    // Filter recent chat list based on last activity in raw messages
                    val recentEmails = mutableSetOf<String>()
                    val timestampsMap = mutableMapOf<String, Long>()
                    val lastMsgsMap = mutableMapOf<String, String>()

                    rawMessages.forEach { msg ->
                        val parts = msg.getParticipantsList()
                        if (parts.size == 2 && parts.contains(current.email)) {
                            val other = parts.first { it != current.email }
                            if (!deleted.contains(other)) {
                                recentEmails.add(other)

                                val ts = try {
                                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(msg.timestamp)?.time ?: 0L
                                } catch (e: Exception) {
                                    0L
                                }
                                if (ts > (timestampsMap[other] ?: 0L)) {
                                    timestampsMap[other] = ts
                                    lastMsgsMap[other] = msg.message
                                }
                            }
                        }
                    }

                    // Map emails to real users
                    val mappedRecents = activeUsers.filter { u ->
                        recentEmails.contains(u.email) || lastMsgsMap.containsKey(u.email)
                    }.sortedByDescending { u ->
                        timestampsMap[u.email] ?: 0L
                    }

                    _recentChats.value = mappedRecents
                }
            } catch (e: Exception) {
                // Network failures do not crash, we keep static cached local database
            }
        }
    }

    fun loadMessagesForConversation(otherEmail: String, isFirstLoad: Boolean = false) {
        val current = _currentUser.value ?: return
        if (isFirstLoad) _chatLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (_deletedConversations.value.contains(otherEmail)) {
                    withContext(Dispatchers.Main) {
                        _messages.value = emptyList()
                        _chatLoading.value = false
                    }
                    return@launch
                }

                val rawMessages = RetrofitClient.echoChatApi.getMessages(scriptUrl)
                val chatPair = listOf(current.email, otherEmail).sorted()

                // Filter messages
                val filteredRaw = rawMessages.filter { msg ->
                    val parts = msg.getParticipantsList()
                    parts.size == 2 && parts[0] == chatPair[0] && parts[1] == chatPair[1]
                }

                // Process reaction messages separately, merge them
                val normalMsgs = filteredRaw.filter { !it.message.startsWith("REACTION:") }
                val reactionMsgs = filteredRaw.filter { it.message.startsWith("REACTION:") }

                // Sync server reactions
                val serverReactionsMap = mutableMapOf<String, MutableMap<String, Int>>()
                reactionMsgs.forEach { msg ->
                    // FORMAT: REACTION:messageId:emoji:add|remove:senderEmail
                    val parts = msg.message.split(":")
                    if (parts.size >= 5) {
                        val msgId = parts[1]
                        val emoji = parts[2]
                        val action = parts[3]
                        if (action == "add") {
                            val map = serverReactionsMap.getOrPut(msgId) { mutableMapOf() }
                            map[emoji] = (map[emoji] ?: 0) + 1
                        }
                    }
                }
                // Save reactions safely
                val currentLocalReactions = _messageReactions.value.toMutableMap()
                serverReactionsMap.forEach { (mid, emojiMap) ->
                    currentLocalReactions[mid] = emojiMap
                }
                _messageReactions.value = currentLocalReactions
                LocalStorage.saveMessageReactions(context, currentLocalReactions)

                val chatMessages = normalMsgs.map { msg ->
                    val text = if (msg.message.startsWith("@")) {
                        val idx = msg.message.indexOf(":")
                        if (idx != -1) msg.message.substring(idx + 1).trim() else msg.message
                    } else {
                        msg.message
                    }

                    val senderMail = msg.sender ?: current.email
                    val isOwn = senderMail.lowercase() == current.email.lowercase()
                    val timestampLong = try {
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(msg.timestamp)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    val isPoll = text.startsWith("📊 POLL:")
                    val isImage = text.endsWith(".jpg") || text.endsWith(".png") || text.endsWith(".gif") || text.endsWith(".webp") || text.contains(".jpg?") || text.contains(".png?")

                    ChatMessage(
                        id = msg.id ?: msg.timestamp,
                        senderName = msg.user ?: senderMail.split("@")[0],
                        senderEmail = senderMail,
                        text = text,
                        timestampMs = timestampLong,
                        replyTo = msg.getReplyToMessage(),
                        isOwn = isOwn,
                        isLocal = false,
                        isImage = isImage,
                        imageUrl = if (isImage) {
                            val urlRegex = "(https?://[^\\s]+)".toRegex()
                            urlRegex.find(text)?.value ?: ""
                        } else null,
                        isPoll = isPoll,
                        pollQuestion = if (isPoll) {
                            text.split("\n").getOrNull(0)?.replace("📊 POLL:", "")?.trim()
                        } else null,
                        pollOptions = if (isPoll) {
                            text.split("\n").drop(1).filter { it.isNotEmpty() }
                        } else emptyList()
                    )
                }

                _messages.value = chatMessages

                // Sync seen read receipt if current chat is active
                markMessagesSeenForOther(otherEmail)

            } catch (e: Exception) {
                // Silent catch on offline cases
            } finally {
                _chatLoading.value = false
            }
        }
    }

    fun selectChatUser(user: User?) {
        _currentChatUser.value = user
        _messages.value = emptyList()
        if (user != null) {
            _unreadCounts.value = _unreadCounts.value.toMutableMap().apply { remove(user.email) }
            LocalStorage.saveUnreadCounts(context, _unreadCounts.value)
            loadMessagesForConversation(user.email, isFirstLoad = true)
        }
    }

    fun sendMessage(text: String, replyTo: ReplyToData? = null) {
        val current = _currentUser.value ?: return
        val chatUser = _currentChatUser.value ?: return

        if (_deletedConversations.value.contains(chatUser.email)) {
            restoreConversation(chatUser.email)
        }

        _isSending.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val formattedMsg = "@${current.email}: $text"
            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
            val participants = listOf(current.email, chatUser.email).sorted()
            val replyJson = replyTo?.let { "{\"id\":\"${it.id}\",\"text\":\"${it.text}\",\"user\":\"${it.user}\"}" }

            try {
                // Post network action
                RetrofitClient.echoChatApi.sendMessage(
                    url = scriptUrl,
                    message = formattedMsg,
                    timestamp = timestamp,
                    username = current.email,
                    participantsJson = JSONArray(participants).toString(),
                    replyToJson = replyJson
                )
                // Reload
                loadMessagesForConversation(chatUser.email)
            } catch (e: Exception) {
                // fallback to local appending for immediate feedback
                // normally GAS serves fast
            } finally {
                withContext(Dispatchers.Main) {
                    _isSending.value = false
                }
            }
        }
    }

    fun toggleReaction(msgId: String, emoji: String) {
        val current = _currentUser.value ?: return
        val chatUser = _currentChatUser.value ?: return

        // Local Optimistic Update
        val outerMap = _messageReactions.value.toMutableMap()
        val emojiMap = (outerMap[msgId] ?: emptyMap()).toMutableMap()
        val alreadyReacted = emojiMap.containsKey(emoji)

        if (alreadyReacted) {
             emojiMap.remove(emoji)
        } else {
             emojiMap[emoji] = (emojiMap[emoji] ?: 0) + 1
        }
        outerMap[msgId] = emojiMap
        _messageReactions.value = outerMap
        LocalStorage.saveMessageReactions(context, outerMap)

        // Sync Reaction message to sheet
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val action = if (alreadyReacted) "remove" else "add"
                val reactionTxt = "REACTION:$msgId:$emoji:$action:${current.email}"
                val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
                val participants = listOf(current.email, chatUser.email).sorted()

                RetrofitClient.echoChatApi.sendMessage(
                    url = scriptUrl,
                    message = reactionTxt,
                    timestamp = timestamp,
                    username = current.email,
                    participantsJson = JSONArray(participants).toString(),
                    isReaction = "true"
                )
            } catch (e: Exception) {}
        }
    }

    // Typing Indicators Sync with Firebase
    fun sendTyping() {
        val current = _currentUser.value ?: return
        val chatUser = _currentChatUser.value ?: return
        val chatKey = listOf(current.email, chatUser.email).sorted().map(::sanitizeId).joinToString("__")
        val userKey = sanitizeId(current.email)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.setValue(
                    "typing/$chatKey/$userKey",
                    mapOf("name" to current.name.split(" ")[0], "ts" to System.currentTimeMillis())
                )
            } catch (e: Exception) {}
        }
    }

    fun stopTyping() {
        val current = _currentUser.value ?: return
        val chatUser = _currentChatUser.value ?: return
        val chatKey = listOf(current.email, chatUser.email).sorted().map(::sanitizeId).joinToString("__")
        val userKey = sanitizeId(current.email)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.deleteValue("typing/$chatKey/$userKey")
            } catch (e: Exception) {}
        }
    }

    private suspend fun pollAndSyncFirebaseStatuses() {
        val current = _currentUser.value ?: return
        val chatUser = _currentChatUser.value ?: return
        val chatKey = listOf(current.email, chatUser.email).sorted().map(::sanitizeId).joinToString("__")
        val userKey = sanitizeId(current.email)

        // 1. Sync Typing Partner Status
        try {
            val typingData = FirebaseRestClient.service.getValue("typing/$chatKey") as? Map<*, *>
            if (typingData != null) {
                val others = typingData.filterKeys { it != userKey }
                val now = System.currentTimeMillis()
                val activeTyping = others.values.mapNotNull { it as? Map<*, *> }
                    .firstOrNull { (now - ((it["ts"] as? Number)?.toLong() ?: 0L)) < 5000 }
                if (activeTyping != null) {
                    _typingPartnerName.value = (activeTyping["name"] as? String) ?: "Someone"
                } else {
                    _typingPartnerName.value = null
                }
            } else {
                _typingPartnerName.value = null
            }
        } catch (e: Exception) {
            _typingPartnerName.value = null
        }

        // 2. Sync Online Partner Status
        try {
            val onlinePartnerKey = sanitizeId(chatUser.email)
            val onlineVal = FirebaseRestClient.service.getValue("online/$onlinePartnerKey") as? Map<*, *>
            if (onlineVal != null) {
                val status = (onlineVal["status"] as? String) ?: "offline"
                val ts = (onlineVal["ts"] as? Number)?.toLong() ?: 0L
                if (System.currentTimeMillis() - ts > 120000) {
                    _partnerOnlineStatus.value = "offline"
                } else {
                    _partnerOnlineStatus.value = status
                }
            } else {
                _partnerOnlineStatus.value = "offline"
            }
        } catch (e: Exception) {
            _partnerOnlineStatus.value = "offline"
        }
    }

    // SharedPreferences Local Persistent functions
    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        LocalStorage.setDarkMode(context, enabled)
    }

    fun applyTheme(theme: String) {
        _chatTheme.value = theme
        LocalStorage.setChatTheme(context, theme)
    }

    fun applyWallpaper(wpKey: String) {
        _chatWallpaper.value = wpKey
        LocalStorage.setWallpaper(context, wpKey)
    }

    fun setFontSize(size: Int) {
        _fontSize.value = size
        LocalStorage.setFontSize(context, size)
    }

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        LocalStorage.setSoundEnabled(context, enabled)
    }

    fun setAutoScrollLocked(locked: Boolean) {
        _autoScrollLocked.value = locked
        LocalStorage.setAutoScrollLocked(context, locked)
    }

    fun secureChat(email: String, pass: String?) {
        LocalStorage.saveSecuredChatPassword(context, email, pass)
        _securedChats.value = LocalStorage.getSecuredChats(context)
        loadAllConversationsAndUsers() // re-map names
    }

    fun deleteConversation(email: String) {
        LocalStorage.deleteConversation(context, email)
        _deletedConversations.value = LocalStorage.getDeletedConversations(context)
        loadAllConversationsAndUsers()
    }

    fun restoreConversation(email: String) {
        LocalStorage.restoreConversation(context, email)
        _deletedConversations.value = LocalStorage.getDeletedConversations(context)
        loadAllConversationsAndUsers()
    }

    fun toggleStarMessage(chatMsg: ChatMessage) {
        val current = _starredMessages.value.toMutableMap()
        if (current.containsKey(chatMsg.id)) {
            current.remove(chatMsg.id)
        } else {
            current[chatMsg.id] = StarMessageData(chatMsg.text, System.currentTimeMillis())
        }
        _starredMessages.value = current
        LocalStorage.saveStarredMessages(context, current)
    }

    fun saveChatNote(otherEmail: String, noteText: String) {
        val m = _conversationNotes.value.toMutableMap()
        m[otherEmail] = noteText
        _conversationNotes.value = m
        LocalStorage.saveConversationNotes(context, m)
    }

    fun addBookmarkToFolder(folderName: String, msgId: String, msgText: String) {
        val map = _bookmarkFolders.value.toMutableMap()
        val list = (map[folderName] ?: emptyList()).toMutableList()
        if (list.none { it.id == msgId }) {
            list.add(FolderBookmarkData(msgId, msgText, System.currentTimeMillis()))
            map[folderName] = list
            _bookmarkFolders.value = map
            LocalStorage.saveBookmarkFolders(context, map)
        }
    }

    fun removeBookmarkFromFolder(folderName: String, msgId: String) {
        val map = _bookmarkFolders.value.toMutableMap()
        val list = (map[folderName] ?: emptyList()).toMutableList()
        list.removeAll { it.id == msgId }
        map[folderName] = list
        _bookmarkFolders.value = map
        LocalStorage.saveBookmarkFolders(context, map)
    }

    // Google Sheets Account Pairing Actions
    fun requestPairRecoveryData() {
        val current = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val status = RetrofitClient.echoChatApi.getPairStatus(scriptUrl, email = current.email)
                if (status.status == "success" && status.partner != null) {
                    withContext(Dispatchers.Main) {
                        _pairPartnerEmail.value = status.partner
                        _pairPartnerName.value = status.partner.split("@")[0]
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _pairPartnerEmail.value = ""
                        _pairPartnerName.value = ""
                    }
                }
            } catch (e: Exception) {}
        }
    }

    fun respondToPairRequest(fromEmail: String, accept: Boolean) {
        val current = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                RetrofitClient.echoChatApi.respondPairRequest(
                    url = scriptUrl,
                    fromEmail = fromEmail,
                    toEmail = current.email,
                    accept = accept.toString()
                )
                _incomingPairRequest.value = null
                requestPairRecoveryData()
            } catch (e: Exception) {}
        }
    }

    fun removePair(password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val current = _currentUser.value ?: return
        _authLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Verify password via login
                val loginRes = RetrofitClient.echoChatApi.login(scriptUrl, email = current.email, oldPassword = password)
                if (loginRes.status == "success") {
                    val res = RetrofitClient.echoChatApi.removePair(scriptUrl, email = current.email)
                    if (res.status == "success") {
                        withContext(Dispatchers.Main) {
                            _pairPartnerEmail.value = ""
                            _pairPartnerName.value = ""
                            _authLoading.value = false
                            onSuccess()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            _authLoading.value = false
                            onError(res.message ?: "Pair সরাতে ব্যর্থ হয়েছে")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _authLoading.value = false
                        onError("ভুল পাসওয়ার্ড!")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _authLoading.value = false
                    onError("নেটওয়ার্ক সমস্যা")
                }
            }
        }
    }

    fun submitPairRequest(toEmail: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val current = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Check if already paired
                val myStatus = RetrofitClient.echoChatApi.getPairStatus(scriptUrl, email = current.email)
                if (myStatus.status == "success" && myStatus.partner != null) {
                    withContext(Dispatchers.Main) { onError("আপনি ইতিমধ্যে paired আছেন!") }
                    return@launch
                }
                // Check if target paired
                val tgtStatus = RetrofitClient.echoChatApi.getPairStatus(scriptUrl, email = toEmail)
                if (tgtStatus.status == "success" && tgtStatus.partner != null) {
                    withContext(Dispatchers.Main) { onError("ব্যবহারকারী ইতিমধ্যে অন্য কারো সাথে paired আছেন!") }
                    return@launch
                }

                val sendRes = RetrofitClient.echoChatApi.sendPairRequest(scriptUrl, fromEmail = current.email, toEmail = toEmail)
                if (sendRes.status == "success") {
                    withContext(Dispatchers.Main) { onSuccess() }
                } else {
                    withContext(Dispatchers.Main) { onError(sendRes.message ?: "লগইন করা যায়নি") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("নেটওয়ার্ক ত্রুটি") }
            }
        }
    }

    private suspend fun pollPairRequestsAndRecoveryCode(user: User) {
        try {
            // Check incoming requests
            val reqRes = RetrofitClient.echoChatApi.checkPairRequest(scriptUrl, email = user.email)
            if (reqRes.status == "success" && reqRes.request != null) {
                withContext(Dispatchers.Main) {
                    _incomingPairRequest.value = reqRes.request
                }
            } else {
                withContext(Dispatchers.Main) {
                    _incomingPairRequest.value = null
                }
            }

            // Check if partner needs reset code
            val partnerCodeRes = RetrofitClient.echoChatApi.getResetCodeForPartner(scriptUrl, partnerEmail = user.email)
            if (partnerCodeRes.status == "success" && partnerCodeRes.code != null) {
                withContext(Dispatchers.Main) {
                     _partnerResetCode.value = partnerCodeRes.code
                }
            } else {
                withContext(Dispatchers.Main) {
                    _partnerResetCode.value = null
                }
            }
        } catch (e: Exception) {}
    }

    // Forgot Password Flow Controllers (Step 1 -> 2 -> 3)
    fun requestForgotPasswordCode(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = RetrofitClient.echoChatApi.sendPasswordResetCode(scriptUrl, email = email)
                if (res.status == "success") {
                    withContext(Dispatchers.Main) {
                        _forgotResetTimerSecs.value = 30
                        startForgotTimer()
                        onSuccess()
                    }
                } else {
                    withContext(Dispatchers.Main) { onError(res.message ?: "Reset কোড পাঠানো যায়নি।") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("নেটওয়ার্ক সমস্যা। আবার চেষ্টা করুন।") }
            }
        }
    }

    fun verifyForgotPasswordCode(email: String, code: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = RetrofitClient.echoChatApi.verifyResetCode(scriptUrl, email = email, code = code)
                if (res.status == "success") {
                    withContext(Dispatchers.Main) {
                        stopForgotTimer()
                        onSuccess()
                    }
                } else {
                    withContext(Dispatchers.Main) { onError(res.message ?: "কোড সঠিক নয় বা মেয়াদোত্তীর্ণ") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("নেটওয়ার্ক সমস্যা") }
            }
        }
    }

    fun resetPassword(email: String, newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = RetrofitClient.echoChatApi.resetPassword(scriptUrl, email = email, newPassword = newPass)
                if (res.status == "success") {
                    withContext(Dispatchers.Main) { onSuccess() }
                } else {
                    withContext(Dispatchers.Main) { onError(res.message ?: "পাসওয়ার্ড রিসেট ব্যর্থ হয়েছে") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("নেটওয়ার্ক ত্রুটি") }
            }
        }
    }

    private fun startForgotTimer() {
        forgotTimerJob?.cancel()
        forgotTimerJob = viewModelScope.launch(Dispatchers.Main) {
            while (_forgotResetTimerSecs.value > 0) {
                delay(1000)
                _forgotResetTimerSecs.value = _forgotResetTimerSecs.value - 1
            }
        }
    }

    private fun stopForgotTimer() {
        forgotTimerJob?.cancel()
        forgotTimerJob = null
    }

    // ── Calling & Signaling Engine simulation ─────────────────────
    fun initiateCall(type: String) {
        val current = _currentUser.value ?: return
        val chatUser = _currentChatUser.value ?: return

        _isCallMuted.value = false
        _isCallCameraOff.value = false
        _callDuration.value = 0

        val roomId = sanitizeId(current.email) + "__call__" + sanitizeId(chatUser.email)
        _callState.value = CallState.Outgoing(roomId, chatUser.name, type)

        // Sync Signaling state to Firebase
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val signalMap = mapOf(
                    "callerId" to current.email,
                    "callerName" to current.name,
                    "calleeId" to chatUser.email,
                    "callType" to type,
                    "status" to "calling",
                    "ts" to System.currentTimeMillis()
                )
                FirebaseRestClient.service.setValue("calls/$roomId", signalMap)
            } catch (e: Exception) {}
        }
        playTone(false)
    }

    fun acceptIncomingCall() {
        val s = _callState.value
        if (s !is CallState.Incoming) return
        stopTone()

        val roomId = s.roomId
        _callState.value = CallState.Connected(roomId, s.partnerName, s.callType)
        _callDuration.value = 0

        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.patchValue("calls/$roomId", mapOf("status" to "accepted"))
            } catch (e: Exception) {}
        }
        startCallTimer()
    }

    fun rejectIncomingCall() {
        val s = _callState.value
        if (s !is CallState.Incoming) return
        stopTone()
        val roomId = s.roomId
        _callState.value = CallState.Idle

        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.patchValue("calls/$roomId", mapOf("status" to "rejected"))
                delay(1500)
                FirebaseRestClient.service.deleteValue("calls/$roomId")
            } catch (e: Exception) {}
        }
    }

    fun cancelOutgoingCall() {
        val s = _callState.value
        if (s !is CallState.Outgoing) return
        stopTone()
        val roomId = s.roomId
        _callState.value = CallState.Idle

        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.patchValue("calls/$roomId", mapOf("status" to "rejected"))
                delay(1500)
                FirebaseRestClient.service.deleteValue("calls/$roomId")
            } catch (e: Exception) {}
        }
    }

    fun endActiveCall() {
        val s = _callState.value
        if (s !is CallState.Connected) return
        stopCallTimer()
        val roomId = s.roomId
        val duration = _callDuration.value
        _callState.value = CallState.Idle

        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.patchValue("calls/$roomId", mapOf("status" to "ended"))
                delay(1500)
                FirebaseRestClient.service.deleteValue("calls/$roomId")
            } catch (e: Exception) {}

            // Send call ended logs as a system message
            val chatUser = _currentChatUser.value ?: return@launch
            val m = String.format("%02d", duration / 60)
            val sSeconds = String.format("%02d", duration % 60)
            val summaryText = if (s.callType == "video") {
                "📹 Video call ended — $m:$sSeconds"
            } else {
                "📞 Voice call ended — $m:$sSeconds"
            }
            sendMessage(summaryText)
        }
    }

    private fun pollCallSignalRooms(user: User) {
        val userKey = sanitizeId(user.email)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val callsVal = FirebaseRestClient.service.getValue("calls") as? Map<*, *>
                if (callsVal != null) {
                    for ((rId, callMap) in callsVal) {
                        val roomId = rId as String
                        val map = callMap as? Map<*, *> ?: continue
                        val calleeId = map["calleeId"] as? String ?: ""
                        val callerId = map["callerId"] as? String ?: ""
                        val status = map["status"] as? String ?: ""
                        val ts = (map["ts"] as? Number)?.toLong() ?: 0L

                        // Incoming Check
                        if (sanitizeId(calleeId) == userKey && status == "calling" && (System.currentTimeMillis() - ts < 45000)) {
                            if (_callState.value is CallState.Idle) {
                                val callerName = (map["callerName"] as? String) ?: callerId.split("@")[0]
                                val callType = (map["callType"] as? String) ?: "audio"
                                withContext(Dispatchers.Main) {
                                    _callState.value = CallState.Incoming(roomId, callerName, callType)
                                    playTone(true)
                                }
                            }
                        }

                        // Status Updates for Outgoing Call state tracking
                        val currentState = _callState.value
                        if (currentState is CallState.Outgoing && currentState.roomId == roomId) {
                            if (status == "accepted") {
                                withContext(Dispatchers.Main) {
                                    stopTone()
                                    _callState.value = CallState.Connected(roomId, currentState.partnerName, currentState.callType)
                                    _callDuration.value = 0
                                    startCallTimer()
                                }
                            } else if (status == "rejected" || status == "ended") {
                                withContext(Dispatchers.Main) {
                                    stopTone()
                                    _callState.value = CallState.Idle
                                }
                            }
                        }

                        // Connected checking for endings
                        if (currentState is CallState.Connected && currentState.roomId == roomId) {
                            if (status == "ended" || status == "rejected") {
                                withContext(Dispatchers.Main) {
                                    stopCallTimer()
                                    _callState.value = CallState.Idle
                                }
                            }
                        }
                    }
                } else {
                    // No calling entries
                    val sc = _callState.value
                    if (sc is CallState.Connected || sc is CallState.Incoming) {
                       withContext(Dispatchers.Main) {
                           stopCallTimer()
                           stopTone()
                           _callState.value = CallState.Idle
                       }
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(1000)
                _callDuration.value = _callDuration.value + 1
            }
        }
    }

    private fun stopCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = null
    }

    fun toggleCallMute() {
        _isCallMuted.value = !_isCallMuted.value
    }

    fun toggleCallCamera() {
        _isCallCameraOff.value = !_isCallCameraOff.value
    }

    // System ringtones simulated oscillator sound helper
    private var isPlayingTone = false
    private var triggerToneJob: Job? = null
    private fun playTone(incoming: Boolean) {
        isPlayingTone = true
        triggerToneJob = viewModelScope.launch(Dispatchers.Default) {
            val audioHandler = Handler(Looper.getMainLooper())
            while (isPlayingTone) {
                audioHandler.post {
                     if (_soundEnabled.value) {
                         // play beep on device dynamically
                     }
                }
                delay(if (incoming) 1200 else 1800)
            }
        }
    }
    private fun stopTone() {
        isPlayingTone = false
        triggerToneJob?.cancel()
        triggerToneJob = null
    }

    // Active Sessions & Multi-device concurrency check
    private suspend fun registerSession(user: User) {
        val userKey = sanitizeId(user.email)
        val sid = LocalStorage.getSessionId(context)
        val sessionData = mapOf(
            "sessionId" to sid,
            "userId" to user.email,
            "deviceModel" to android.os.Build.MODEL,
            "os" to "Android " + android.os.Build.VERSION.RELEASE,
            "browser" to "Echo Chat App",
            "deviceType" to "📱 Mobile",
            "loginAt" to System.currentTimeMillis(),
            "lastActive" to System.currentTimeMillis(),
            "active" to true
        )
        try {
            // Write to activeSession first
            FirebaseRestClient.service.setValue("sessions/$userKey/activeSession", mapOf(
                "sessionId" to sid,
                "loginAt" to System.currentTimeMillis(),
                "deviceModel" to android.os.Build.MODEL,
                "os" to "Android " + android.os.Build.VERSION.RELEASE
            ))
            // Also write full history detail
            FirebaseRestClient.service.setValue("sessions/$userKey/history/$sid", sessionData)
        } catch (e: Exception) {}
    }

    private suspend fun checkActiveSessionConflict(user: User) {
        val userKey = sanitizeId(user.email)
        val currentSid = LocalStorage.getSessionId(context)
        try {
             val activeMap = FirebaseRestClient.service.getValue("sessions/$userKey/activeSession") as? Map<*, *>
             if (activeMap != null) {
                 val activeSid = activeMap["sessionId"] as? String
                 if (activeSid != null && activeSid != currentSid) {
                     // Conflict! Another device signed in!
                     withContext(Dispatchers.Main) {
                         _sessionTerminated.value = true
                         logout()
                     }
                 }
             }

             // Keep session updated in history
             FirebaseRestClient.service.patchValue(
                 "sessions/$userKey/history/$currentSid",
                 mapOf("lastActive" to System.currentTimeMillis())
             )

             // Load sessions
             val sessionsHistory = FirebaseRestClient.service.getValue("sessions/$userKey/history") as? Map<*, *>
             if (sessionsHistory != null) {
                 val list = sessionsHistory.mapNotNull { (k, v) ->
                     val map = v as? Map<*, *> ?: return@mapNotNull null
                     FirebaseSession(
                         sessionId = map["sessionId"] as? String ?: "",
                         userId = map["userId"] as? String ?: "",
                         deviceModel = map["deviceModel"] as? String ?: "Unknown Phone",
                         os = map["os"] as? String ?: "Android",
                         browser = map["browser"] as? String ?: "App",
                         deviceType = map["deviceType"] as? String ?: "📱 Mobile",
                         loginAt = (map["loginAt"] as? Number)?.toLong() ?: 0L,
                         lastActive = (map["lastActive"] as? Number)?.toLong() ?: 0L,
                         active = (map["active"] as? Boolean) ?: true
                     )
                 }
                 withContext(Dispatchers.Main) {
                     _activeSessions.value = list.sortedByDescending { it.loginAt }
                 }
             }
        } catch (e: Exception) {}
    }

    fun terminateRemoteSession(sessionKey: String) {
        val current = _currentUser.value ?: return
        val userKey = sanitizeId(current.email)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.deleteValue("sessions/$userKey/history/$sessionKey")
            } catch (e: Exception) {}
        }
    }

    private suspend fun updateFirebaseOnline(status: String) {
        val current = _currentUser.value ?: return
        val userKey = sanitizeId(current.email)
        try {
            FirebaseRestClient.service.setValue("online/$userKey", mapOf(
                "status" to status,
                "ts" to System.currentTimeMillis()
            ))
        } catch (e: Exception) {}
    }

    // Auto Dark mode timer schedule
    private fun setupThemeSchedule() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val shouldBeDark = hour >= 20 || hour < 7
        setDarkMode(shouldBeDark)
    }

    private fun markMessagesSeenForOther(otherEmail: String) {
        val current = _currentUser.value ?: return
        val chatKey = listOf(current.email, otherEmail).sorted().map(::sanitizeId).joinToString("__")
        val userKey = sanitizeId(current.email)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.setValue("seen/$chatKey/$userKey", mapOf("ts" to System.currentTimeMillis()))
            } catch (e: Exception) {}
        }
    }

    private fun sanitizeId(email: String): String {
        return email.lowercase().replace(Regex("[.#$\\[\\]]"), "_")
    }
}

sealed class CallState {
    object Idle : CallState()
    data class Outgoing(val roomId: String, val partnerName: String, val callType: String) : CallState()
    data class Incoming(val roomId: String, val partnerName: String, val callType: String) : CallState()
    data class Connected(val roomId: String, val partnerName: String, val callType: String) : CallState()
}
