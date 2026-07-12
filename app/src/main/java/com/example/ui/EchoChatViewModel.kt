package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import java.util.*

class EchoChatViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val scriptUrl = "https://script.google.com/macros/s/AKfycbzvqNxH0BGFuXbIvJPMDR6uqUkWvekQvS8asurlYnRoT23lMCZq9NLmLoO4ohje_3Otbg/exec"
    private val versionScriptUrl = "https://script.google.com/macros/s/AKfycbzzmh1S2v4V21mqlHouQtFpMDtIv1BMeeKgWN_JTp6E5eCHgTBSVHx81eAjBhszU-Q76g/exec"

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

    // Groups & Voting States
    private val _myGroups = MutableStateFlow<List<User>>(emptyList())
    val myGroups: StateFlow<List<User>> = _myGroups.asStateFlow()

    private val _currentPollVotes = MutableStateFlow<Map<String, Map<Int, Int>>>(emptyMap())
    val currentPollVotes: StateFlow<Map<String, Map<Int, Int>>> = _currentPollVotes.asStateFlow()

    private val _currentUserVotes = MutableStateFlow<Map<String, Int>>(emptyMap())
    val currentUserVotes: StateFlow<Map<String, Int>> = _currentUserVotes.asStateFlow()

    private val _currentChatUser = MutableStateFlow<User?>(null)
    val currentChatUser: StateFlow<User?> = _currentChatUser.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _allRawMessages = MutableStateFlow<List<MessageRaw>>(emptyList())
    val allRawMessages: StateFlow<List<MessageRaw>> = _allRawMessages.asStateFlow()

    private val _conversationsLastActivity = MutableStateFlow<Map<String, Long>>(emptyMap())
    val conversationsLastActivity: StateFlow<Map<String, Long>> = _conversationsLastActivity.asStateFlow()

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    // Extra Chat Statuses
    private val _typingPartnerName = MutableStateFlow<String?>(null)
    val typingPartnerName: StateFlow<String?> = _typingPartnerName.asStateFlow()

    private val _partnerOnlineStatus = MutableStateFlow("offline") // "online" | "away" | "offline"
    val partnerOnlineStatus: StateFlow<String> = _partnerOnlineStatus.asStateFlow()

    private val _usersOnlineStatuses = MutableStateFlow<Map<String, String>>(emptyMap())
    val usersOnlineStatuses: StateFlow<Map<String, String>> = _usersOnlineStatuses.asStateFlow()

    // Offline Persistent Configurations
    private val _isDarkMode = MutableStateFlow(LocalStorage.isDarkMode(context))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // App Lock Persistent Configurations
    private val _isAppLockEnabled = MutableStateFlow(LocalStorage.isAppLockEnabled(context))
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(LocalStorage.isBiometricEnabled(context))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _appLockPIN = MutableStateFlow(LocalStorage.getAppLockPIN(context))
    val appLockPIN: StateFlow<String?> = _appLockPIN.asStateFlow()

    private val _isAppLocked = MutableStateFlow(LocalStorage.isAppLockEnabled(context) && LocalStorage.getAppLockPIN(context) != null)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _appLockTimeoutMs = MutableStateFlow(LocalStorage.getAppLockTimeout(context))
    val appLockTimeoutMs: StateFlow<Long> = _appLockTimeoutMs.asStateFlow()

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

    private val localSeenTimestamps = java.util.concurrent.ConcurrentHashMap<String, Long>()

    private val _securedChats = MutableStateFlow<Map<String, String>>(LocalStorage.getSecuredChats(context))
    val securedChats: StateFlow<Map<String, String>> = _securedChats.asStateFlow()

    private val _hiddenChats = MutableStateFlow<Map<String, String>>(LocalStorage.getHiddenChats(context))
    val hiddenChats: StateFlow<Map<String, String>> = _hiddenChats.asStateFlow()

    private val _mutedChats = MutableStateFlow<Map<String, Long>>(LocalStorage.getMutedChats(context))
    val mutedChats: StateFlow<Map<String, Long>> = _mutedChats.asStateFlow()

    private val _promotedAdmins = MutableStateFlow<Map<String, List<String>>>(LocalStorage.getPromotedAdmins(context))
    val promotedAdmins: StateFlow<Map<String, List<String>>> = _promotedAdmins.asStateFlow()

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

    // App Version States
    private val _versions = MutableStateFlow<List<AppVersionInfo>>(emptyList())
    val versions: StateFlow<List<AppVersionInfo>> = _versions.asStateFlow()

    private val _latestVersionInfo = MutableStateFlow<AppVersionInfo?>(null)
    val latestVersionInfo: StateFlow<AppVersionInfo?> = _latestVersionInfo.asStateFlow()


    private val _incomingPairCodeData = MutableStateFlow<Map<String, String>?>(null)
    val incomingPairCodeData: StateFlow<Map<String, String>?> = _incomingPairCodeData.asStateFlow()

    private val _appLanguage = MutableStateFlow<String>(LocalStorage.getLanguage(context))
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _partnerResetCode = MutableStateFlow<String?>(null)
    val partnerResetCode: StateFlow<String?> = _partnerResetCode.asStateFlow()

    private val _perChatWallpaper = MutableStateFlow<Map<String, String>>(emptyMap())
    val perChatWallpaper: StateFlow<Map<String, String>> = _perChatWallpaper.asStateFlow()

    private val _perChatTheme = MutableStateFlow<Map<String, String>>(emptyMap())
    val perChatTheme: StateFlow<Map<String, String>> = _perChatTheme.asStateFlow()

    // Last talk timestamps and seen maps
    private val _lastActiveTimestamps = MutableStateFlow<Map<String, Long>>(emptyMap())
    val lastActiveTimestamps: StateFlow<Map<String, Long>> = _lastActiveTimestamps.asStateFlow()

    private val _lastMessageSenderMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val lastMessageSenderMap: StateFlow<Map<String, String>> = _lastMessageSenderMap.asStateFlow()

    private val _isViewingHidden = MutableStateFlow(false)
    val isViewingHidden: StateFlow<Boolean> = _isViewingHidden.asStateFlow()

    fun setViewingHidden(viewing: Boolean) {
        _isViewingHidden.value = viewing
    }

    private val _chatSeenMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    val chatSeenMap: StateFlow<Map<String, Long>> = _chatSeenMap.asStateFlow()

    private val _groupMembers = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val groupMembers: StateFlow<Map<String, List<String>>> = _groupMembers.asStateFlow()

    private val _groupCreators = MutableStateFlow<Map<String, String>>(emptyMap())
    val groupCreators: StateFlow<Map<String, String>> = _groupCreators.asStateFlow()

    private val _groupSubAdmins = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val groupSubAdmins: StateFlow<Map<String, List<String>>> = _groupSubAdmins.asStateFlow()

    private val deliveredMessageIds = mutableSetOf<String>().apply {
        try {
            addAll(context.getSharedPreferences("EchoChatPrefs", Context.MODE_PRIVATE).getStringSet("DELIVERED_MSG_IDS", emptySet()) ?: emptySet())
        } catch (e: Exception) {}
    }

    private fun saveDeliveredMessageId(msgId: String) {
        if (deliveredMessageIds.add(msgId)) {
            try {
                context.getSharedPreferences("EchoChatPrefs", Context.MODE_PRIVATE).edit()
                    .putStringSet("DELIVERED_MSG_IDS", deliveredMessageIds)
                    .apply()
            } catch (e: Exception) {}
        }
    }

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
    private var callSyncJob: Job? = null
    private var callTimerJob: Job? = null
    private var forgotTimerJob: Job? = null

    // Track last raw msg size or timestamps to notify only on genuine incoming
    private var lastMessagesCount = 0

    // Premium state variables
    private val _premiumVerifiedColors = MutableStateFlow<Map<String, String>>(emptyMap())
    val premiumVerifiedColors: StateFlow<Map<String, String>> = _premiumVerifiedColors.asStateFlow()

    private val _agreedUsers = MutableStateFlow<Map<String, Map<String, Boolean>>>(emptyMap())
    val agreedUsers: StateFlow<Map<String, Map<String, Boolean>>> = _agreedUsers.asStateFlow()

    private val _premiumCodes = MutableStateFlow<List<PremiumCode>>(emptyList())
    val premiumCodes: StateFlow<List<PremiumCode>> = _premiumCodes.asStateFlow()

    private val _premiumLoading = MutableStateFlow(false)
    val premiumLoading: StateFlow<Boolean> = _premiumLoading.asStateFlow()

    // Spy Mode state
    private val _spyingOnUser = MutableStateFlow<User?>(null)
    val spyingOnUser: StateFlow<User?> = _spyingOnUser.asStateFlow()

    fun enterSpyMode(user: User) {
        _spyingOnUser.value = user
        loadAllConversationsAndUsers()
    }

    fun exitSpyMode() {
        _spyingOnUser.value = null
        loadAllConversationsAndUsers()
    }

    fun agreeSmsUser(specialUserEmail: String, otherUserEmail: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val path = "agreed_users/${sanitizeId(specialUserEmail)}/${sanitizeId(otherUserEmail)}"
                FirebaseRestClient.service.setValue(path, true)
                val currentMap = _agreedUsers.value.toMutableMap()
                val innerMap = currentMap[sanitizeId(specialUserEmail)]?.toMutableMap() ?: mutableMapOf()
                innerMap[sanitizeId(otherUserEmail)] = true
                currentMap[sanitizeId(specialUserEmail)] = innerMap
                _agreedUsers.value = currentMap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun revokeAgreeSmsUser(specialUserEmail: String, otherUserEmail: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val path = "agreed_users/${sanitizeId(specialUserEmail)}/${sanitizeId(otherUserEmail)}"
                FirebaseRestClient.service.deleteValue(path)
                val currentMap = _agreedUsers.value.toMutableMap()
                val innerMap = currentMap[sanitizeId(specialUserEmail)]?.toMutableMap() ?: mutableMapOf()
                innerMap.remove(sanitizeId(otherUserEmail))
                currentMap[sanitizeId(specialUserEmail)] = innerMap
                _agreedUsers.value = currentMap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    init {
        // Attempt Autologin
        val savedUser = LocalStorage.getLoggedInUser(context)
        if (savedUser != null) {
            _currentUser.value = savedUser
            startSynchronization()
        }
        reloadSecuredAndHiddenChats()
        val verified = LocalStorage.getVerifiedUsers(context).toMutableMap()
        if (isRafidUser(savedUser)) {
            savedUser?.email?.lowercase()?.trim()?.let { email ->
                verified[email] = "gold"
            }
        }
        _premiumVerifiedColors.value = verified
        loadCustomBadWords()
    }

    fun loadPremiumCodesFromSheet(onDone: () -> Unit = {}) {
        _premiumLoading.value = true
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    RetrofitClient.echoChatApi.getPremiumCodes(
                        "https://script.google.com/macros/s/AKfycbwsywXgaE2gBVr4eRGl8_iknmEZJKBTcDDpcaVOsLDq5HAv_C3xtQSZO6JM82MUAfWz/exec"
                    )
                }
                _premiumCodes.value = list
            } catch (e: Exception) {
                _premiumCodes.value = listOf(
                    PremiumCode(90.0, "black"),
                    PremiumCode(80.0, "blue")
                )
            } finally {
                _premiumLoading.value = false
                withContext(Dispatchers.Main) { onDone() }
            }
        }
    }

    fun activatePremiumCode(codeStr: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val current = _currentUser.value ?: run {
            onError("দয়া করে প্রথমে লগইন করুন।")
            return
        }
        viewModelScope.launch {
            _premiumLoading.value = true
            try {
                // Refresh list first
                val list = try {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.echoChatApi.getPremiumCodes(
                            "https://script.google.com/macros/s/AKfycbwsywXgaE2gBVr4eRGl8_iknmEZJKBTcDDpcaVOsLDq5HAv_C3xtQSZO6JM82MUAfWz/exec"
                        )
                    }
                } catch (e: Exception) {
                    _premiumCodes.value.ifEmpty {
                        listOf(PremiumCode(90.0, "black"), PremiumCode(80.0, "blue"))
                    }
                }
                _premiumCodes.value = list

                val enteredVal = codeStr.trim().toDoubleOrNull()
                val match = list.find { it.code == enteredVal || it.code.toInt().toString() == codeStr.trim() }
                if (match != null) {
                    val color = match.color
                    withContext(Dispatchers.IO) {
                        LocalStorage.saveVerifiedUser(context, current.email, color)
                        try {
                            FirebaseRestClient.service.setValue("verified_users/${sanitizeId(current.email)}", color)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    val updatedMap = _premiumVerifiedColors.value.toMutableMap()
                    updatedMap[current.email] = color
                    _premiumVerifiedColors.value = updatedMap

                    withContext(Dispatchers.Main) {
                        onSuccess(color)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError("ভুল প্রমো বা অ্যাক্টিভেশন কোড!")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("নেটওয়ার্ক ত্রুটি! পরে আবার চেষ্টা করুন।")
                }
            } finally {
                _premiumLoading.value = false
            }
        }
    }

    fun cancelPremiumVerification(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val current = _currentUser.value ?: run {
            onError("দয়া করে প্রথমে লগইন করুন।")
            return
        }
        viewModelScope.launch {
            _premiumLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    LocalStorage.removeVerifiedUser(context, current.email)
                    try {
                        FirebaseRestClient.service.deleteValue("verified_users/${sanitizeId(current.email)}")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                val updatedMap = _premiumVerifiedColors.value.toMutableMap()
                updatedMap.remove(current.email)
                _premiumVerifiedColors.value = updatedMap

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("নেটওয়ার্ক ত্রুটি! পরে আবার চেষ্টা করুন।")
                }
            } finally {
                _premiumLoading.value = false
            }
        }
    }

    fun getMockContactsList(currentUserName: String): List<User> {
        return emptyList()
    }

    fun getMockMessagesFor(currentEmail: String, otherEmail: String): List<ChatMessage> {
        return emptyList()
    }

    fun loginDemo(email: String = "md.r.rafid1234@gmail.com") {
        _authLoading.value = true
        _authError.value = null
        val demoUser = User(
            email = email,
            name = "Md. Rafid",
            photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80"
        )
        LocalStorage.saveLoggedInUser(context, demoUser, "1234")
        _currentUser.value = demoUser
        reloadSecuredAndHiddenChats()
        _authLoading.value = false
        
        // Setup initial demo state
        val mockContacts = getMockContactsList("Md. Rafid")
        _allUsers.value = mockContacts
        _recentChats.value = emptyList()
        startSynchronization()
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
                    val userKey = sanitizeId(email)
                    val isBlocked = try {
                        FirebaseRestClient.service.getValue("blocked_users/$userKey") == true
                    } catch (e: Exception) {
                        false
                    }
                    if (isBlocked) {
                        val blockReason = try {
                            FirebaseRestClient.service.getValue("blocked_reasons/$userKey")?.toString() ?: ""
                        } catch (e: Exception) {
                            ""
                        }
                        val errMsg = if (blockReason.isNotEmpty()) {
                            blockReason
                        } else {
                            "আপনার অ্যাকাউন্টটি ব্লক করা হয়েছে! খারাপ ভাষা ব্যবহারের জন্য আপনার অ্যাকাউন্ট বন্ধ করা হয়েছে।"
                        }
                        withContext(Dispatchers.Main) {
                            _authError.value = errMsg
                            _authLoading.value = false
                        }
                        return@launch
                    }
                    if (res.user.name.endsWith("&")) {
                        withContext(Dispatchers.Main) {
                            _authError.value = "আইডি লগইন হচ্ছে না"
                            _authLoading.value = false
                        }
                        return@launch
                    }
                    LocalStorage.saveLoggedInUser(context, res.user, accessKey)
                    withContext(Dispatchers.Main) {
                        _currentUser.value = res.user
                        reloadSecuredAndHiddenChats()
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
        if (name.endsWith("&")) {
            _authError.value = "আপনার নামের শেষে '&' চিহ্ন ব্যবহার করতে পারবেন না!"
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
        reloadSecuredAndHiddenChats()
        stopSynchronization()
    }

    fun updateProfile(name: String, statusMessage: String, base64Photo: String?, photoUrl: String?, onComplete: (Boolean) -> Unit) {
        val current = _currentUser.value ?: return
        _authLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var finalPhotoUrl = photoUrl ?: current.photoUrl ?: ""
                if (!base64Photo.isNullOrEmpty()) {
                    finalPhotoUrl = "data:image/jpeg;base64,$base64Photo"
                }

                val updatedUser = current.copy(
                    name = name,
                    photoUrl = finalPhotoUrl,
                    statusMessage = statusMessage
                )

                // Save to local Storage
                LocalStorage.saveLoggedInUser(context, updatedUser, LocalStorage.getAccessKey(context) ?: "")
                withContext(Dispatchers.Main) {
                    _currentUser.value = updatedUser
                }

                // Save to Supabase
                val profileMap = mapOf(
                    "email" to current.email,
                    "name" to name,
                    "photoUrl" to finalPhotoUrl,
                    "statusMessage" to statusMessage
                )
                SupabaseRestClient.service.setValue("profiles/${sanitizeId(current.email)}", profileMap)

                // Sync back
                loadAllConversationsAndUsers()

                withContext(Dispatchers.Main) {
                    _authLoading.value = false
                    onComplete(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _authLoading.value = false
                    onComplete(false)
                }
            }
        }
    }

    // Synchronized Background Polling Processes
    private fun startSynchronization() {
        val user = _currentUser.value ?: return
        stopSynchronization()

        // Sync initial files
        loadAllConversationsAndUsers()
        checkBlockedStatus()
        loadBlockedUsers()
        loadAdminPrivacy()

        // 1. periodic polling of messages and users
        autoRefreshJob = viewModelScope.launch(Dispatchers.IO) {
            var lastAllUsersLoad = 0L
            while (isActive) {
                val currentChat = _currentChatUser.value
                val now = System.currentTimeMillis()
                
                if (currentChat != null) {
                    loadMessagesForConversation(currentChat.email)
                }
                
                if (now - lastAllUsersLoad >= 5000) {
                    loadAllConversationsAndUsers()
                    lastAllUsersLoad = now
                }
                
                // If a chat is open, poll very fast (every 1200ms) for a real-time experience!
                // Otherwise poll every 4000ms
                val dynamicDelay = if (currentChat != null) 1200L else 4000L
                delay(dynamicDelay)
            }
        }

        // 2. Realtime firebase REST sync
        firebaseSyncJob = viewModelScope.launch(Dispatchers.IO) {
            // Write session parameters
            registerSession(user)
            updateFirebaseOnline("online")

            var lastSlowSync = 0L
            while (isActive) {
                val now = System.currentTimeMillis()
                val currentChat = _currentChatUser.value
                
                // Fast online ping
                updateFirebaseOnline("online")
                
                if (now - lastSlowSync >= 5000) {
                    checkActiveSessionConflict(user)
                    pollPairRequestsAndRecoveryCode(user)
                    checkBlockedStatus()
                    loadBlockedUsers()
                    loadAdminPrivacy()
                    syncUserBlocks()
                    lastSlowSync = now
                }
                
                // Always poll and sync statuses
                pollAndSyncFirebaseStatuses()
                
                // If a chat is open, poll statuses/typing every 1200ms for instant typing indicators!
                val dynamicDelay = if (currentChat != null) 1200L else 5000L
                delay(dynamicDelay)
            }
        }

        // 3. Dedicated fast call signaling polling (runs every 1.5s)
        callSyncJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                pollCallSignalRooms(user)
                delay(1500)
            }
        }

        // 4. Setup system theme preference schedules
        if (LocalStorage.isDarkModeScheduleEnabled(context)) {
            setupThemeSchedule()
        }
    }

    private fun stopSynchronization() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
        firebaseSyncJob?.cancel()
        firebaseSyncJob = null
        callSyncJob?.cancel()
        callSyncJob = null
        stopCallTimer()
        _forgotResetTimerSecs.value = 30
    }

    // Message lists processing
    private fun loadAllConversationsAndUsers() {
        val current = _currentUser.value ?: return
        val spy = _spyingOnUser.value
        val effectiveCurrentUser = spy ?: current
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Read deleted set
                val deleted = _deletedConversations.value

                // Load all messages
                val allRaw = try {
                    RetrofitClient.echoChatApi.getMessages(scriptUrl)
                } catch (e: Exception) {
                    emptyList()
                }

                val rawMessages = allRaw.filter { msg ->
                    val parts = msg.getParticipantsList()
                    parts.all { !deleted.contains(it) }
                }
                _allRawMessages.value = rawMessages

                // Load versions from Google Apps Script Web App
                launch {
                    loadVersionsFromAppsScript()
                }

                // Load all registered users
                val userRes = try {
                    RetrofitClient.echoChatApi.getUsers(scriptUrl)
                } catch (e: Exception) {
                    null
                }

                if (userRes != null && userRes.status == "success" && userRes.users != null) {
                    val recentEmails = mutableSetOf<String>()
                    val timestampsMap = mutableMapOf<String, Long>()
                    val lastMsgsMap = mutableMapOf<String, String>()
                    val currentSecured = _securedChats.value
                    val currentUserName = effectiveCurrentUser.name.replace(Regex("\\[\\{.*?\\}\\(.*\\)\\]"), "").trim()

                    val remoteProfiles = try {
                        SupabaseRestClient.service.getValue("profiles") as? Map<*, *>
                    } catch (e: Exception) {
                        null
                    }

                    if (remoteProfiles != null) {
                        val mySanitized = sanitizeId(effectiveCurrentUser.email)
                        val myCustomData = remoteProfiles[mySanitized] as? Map<*, *>
                        if (myCustomData != null) {
                            val myName = myCustomData["name"] as? String ?: effectiveCurrentUser.name
                            val myPhoto = myCustomData["photoUrl"] as? String ?: effectiveCurrentUser.photoUrl
                            val myStatus = myCustomData["statusMessage"] as? String ?: effectiveCurrentUser.statusMessage ?: ""
                            if (myName != effectiveCurrentUser.name || myPhoto != effectiveCurrentUser.photoUrl || myStatus != effectiveCurrentUser.statusMessage) {
                                val updatedSelf = effectiveCurrentUser.copy(name = myName, photoUrl = myPhoto, statusMessage = myStatus)
                                withContext(Dispatchers.Main) {
                                    if (spy != null) {
                                        _spyingOnUser.value = updatedSelf
                                    } else {
                                        _currentUser.value = updatedSelf
                                        LocalStorage.saveLoggedInUser(context, updatedSelf, LocalStorage.getAccessKey(context) ?: "")
                                    }
                                }
                            }
                        }
                    }

                    val activeUsers = userRes.users.filter { u ->
                        u.email != effectiveCurrentUser.email && !u.name.endsWith("®")
                    }.map { u ->
                        var mappedUser = u
                        if (remoteProfiles != null) {
                            val sanitizedEmail = sanitizeId(u.email)
                            val customData = remoteProfiles[sanitizedEmail] as? Map<*, *>
                            if (customData != null) {
                                val customName = customData["name"] as? String ?: u.name
                                val customPhoto = customData["photoUrl"] as? String ?: u.photoUrl
                                val customStatus = customData["statusMessage"] as? String ?: u.statusMessage ?: ""
                                mappedUser = u.copy(name = customName, photoUrl = customPhoto, statusMessage = customStatus)
                            }
                        }

                        val lockStr = currentSecured[mappedUser.email]
                        if (lockStr != null) {
                            val baseName = mappedUser.name.replace(Regex("\\[\\{.*?\\}\\(.*\\)\\]"), "").trim()
                            val finalLockPart = if (lockStr.contains("[{")) lockStr else "[{$lockStr}($currentUserName)]"
                            mappedUser.copy(name = "$baseName $finalLockPart")
                        } else {
                            val baseName = mappedUser.name.replace(Regex("\\[\\{.*?\\}\\(.*\\)\\]"), "").trim()
                            mappedUser.copy(name = baseName)
                        }
                    }
                    val activeUsersWithAI = activeUsers
                    _allUsers.value = activeUsersWithAI

                    // Sync verified premium users from Firebase Database dynamically
                    try {
                        val remoteVerified = FirebaseRestClient.service.getValue("verified_users") as? Map<*, *>
                        if (remoteVerified != null) {
                            val mergedVerifiedColors = LocalStorage.getVerifiedUsers(context).toMutableMap()
                            val allLocalAndActiveEmails = activeUsersWithAI.map { it.email }.toMutableList()
                            allLocalAndActiveEmails.add(effectiveCurrentUser.email)

                            remoteVerified.forEach { (k, v) ->
                                val keyStr = k?.toString() ?: ""
                                val colorStr = v?.toString() ?: ""
                                if (keyStr.isNotEmpty() && colorStr.isNotEmpty()) {
                                    val matchingEmail = allLocalAndActiveEmails.find { sanitizeId(it) == keyStr }
                                    if (matchingEmail != null) {
                                        mergedVerifiedColors[matchingEmail] = colorStr
                                    } else {
                                        mergedVerifiedColors[keyStr] = colorStr
                                    }
                                }
                            }
                            // Always guarantee rafid gets golden badge
                            activeUsersWithAI.find { isRafidUser(it) }?.let { rafid ->
                                mergedVerifiedColors[rafid.email.lowercase().trim()] = "gold"
                            }
                            _premiumVerifiedColors.value = mergedVerifiedColors
                            mergedVerifiedColors.forEach { (email, color) ->
                                LocalStorage.saveVerifiedUser(context, email, color)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Filter recent chat list based on last activity in raw messages
                    // (Variables declared at the top of the block)

                    // Load conversations_last_activity, last_sender, and seen from Firebase
                    val remoteLastActivity = try {
                        FirebaseRestClient.service.getValue("conversations_last_activity") as? Map<*, *>
                    } catch (e: Exception) {
                        null
                    }
                    val remoteLastSender = try {
                        FirebaseRestClient.service.getValue("conversations_last_sender") as? Map<*, *>
                    } catch (e: Exception) {
                        null
                    }
                    val remoteSeen = try {
                        FirebaseRestClient.service.getValue("seen") as? Map<*, *>
                    } catch (e: Exception) {
                        null
                    }
                    val activityMap = mutableMapOf<String, Long>()
                    val allActivityMap = mutableMapOf<String, Long>()
                    val mySan = sanitizeId(effectiveCurrentUser.email)
                    remoteLastActivity?.forEach { (k, v) ->
                        val chatKeyStr = k?.toString() ?: return@forEach
                        val ts = (v as? Number)?.toLong() ?: 0L
                        activityMap[chatKeyStr] = ts
                        allActivityMap[chatKeyStr] = ts
                        
                        // If it's a mutual chat key: "user1_email_com__user2_email_com"
                        if (chatKeyStr.contains("__")) {
                            val parts = chatKeyStr.split("__")
                            if (parts.size == 2) {
                                if (parts[0] == mySan || parts[1] == mySan) {
                                    val otherSan = if (parts[0] == mySan) parts[1] else parts[0]
                                    // Find which active user corresponds to otherSan
                                    val matchingUser = activeUsers.find { sanitizeId(it.email) == otherSan }
                                    if (matchingUser != null) {
                                        recentEmails.add(matchingUser.email)
                                        val existingTs = timestampsMap[matchingUser.email] ?: 0L
                                        if (ts > existingTs) {
                                            timestampsMap[matchingUser.email] = ts
                                        }
                                        val existingActTs = activityMap[matchingUser.email] ?: 0L
                                        if (ts > existingActTs) {
                                            activityMap[matchingUser.email] = ts
                                            activityMap[sanitizeId(matchingUser.email)] = ts
                                        }
                                    }
                                }
                            }
                        } else {
                            // If it's a group ID or individual sanitized email
                            val matchingUser = activeUsers.find { sanitizeId(it.email) == chatKeyStr }
                            if (matchingUser != null) {
                                val existingTs = timestampsMap[matchingUser.email] ?: 0L
                                if (ts > existingTs) {
                                    timestampsMap[matchingUser.email] = ts
                                    recentEmails.add(matchingUser.email)
                                }
                                val existingActTs = activityMap[matchingUser.email] ?: 0L
                                if (ts > existingActTs) {
                                    activityMap[matchingUser.email] = ts
                                    activityMap[sanitizeId(matchingUser.email)] = ts
                                }
                            }
                        }
                    }

                    val lastMsgsSenderMap = mutableMapOf<String, String>()
                    rawMessages.forEach { msg ->
                        val parts = msg.getParticipantsList()
                        if (parts.size == 2 && parts.contains(effectiveCurrentUser.email)) {
                            val other = parts.first { it != effectiveCurrentUser.email }
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
                                    lastMsgsSenderMap[other] = msg.sender ?: ""
                                }
                            }
                        }
                    }

                    // Merge raw message timestamps into activityMap
                    timestampsMap.forEach { (email, ts) ->
                        val currentTs = activityMap[email] ?: 0L
                        if (ts > currentTs) {
                            activityMap[email] = ts
                        }
                        val sanitizedEmail = sanitizeId(email)
                        val currentSanitizedTs = activityMap[sanitizedEmail] ?: 0L
                        if (ts > currentSanitizedTs) {
                            activityMap[sanitizedEmail] = ts
                        }
                    }

                    // Map emails to real users
                    val mappedRecents = activeUsersWithAI.filter { u ->
                        (recentEmails.contains(u.email) || lastMsgsMap.containsKey(u.email)) && !deleted.contains(u.email)
                    }.sortedByDescending { u ->
                        timestampsMap[u.email] ?: 0L
                    }

                    _recentChats.value = mappedRecents

                    // Load remote groups from Supabase key "groups"
                    val parsedGroups = mutableListOf<User>()
                    val groupMembers = mutableMapOf<String, List<String>>()
                    val groupCreators = mutableMapOf<String, String>()
                    val groupSubAdmins = mutableMapOf<String, List<String>>()
                    try {
                        val remoteGroups = SupabaseRestClient.service.getValue("groups") as? Map<*, *>
                        remoteGroups?.forEach { (k, v) ->
                            val gId = k?.toString() ?: return@forEach
                            val gMap = v as? Map<*, *> ?: return@forEach
                            val gName = gMap["name"] as? String ?: "Group Chat"
                            val gPhoto = gMap["photoUrl"] as? String ?: ""
                            val membersRaw = gMap["members"] as? String ?: ""
                            val createdBy = gMap["createdBy"] as? String ?: ""
                            val subAdminsRaw = gMap["subAdmins"] as? String ?: ""
                            
                            val membersList = membersRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            val subAdminsList = subAdminsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            if (membersList.contains(current.email) || createdBy == current.email) {
                                groupMembers[gId] = membersList
                                groupCreators[gId] = createdBy
                                groupSubAdmins[gId] = subAdminsList
                                parsedGroups.add(
                                    User(
                                        email = gId,
                                        name = gName,
                                        photoUrl = gPhoto,
                                        statusMessage = "Group • ${membersList.size} members"
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    // Dynamic calculation of unread counts in real-time
                    val myUserKey = sanitizeId(current.email)
                    val calculatedUnreads = mutableMapOf<String, Int>()
                    val allTargetConversations = activeUsers.map { it.email } + parsedGroups.map { it.email }
                    val currentChat = _currentChatUser.value

                    allTargetConversations.forEach { otherEmail ->
                        if (currentChat != null && currentChat.email == otherEmail) {
                            calculatedUnreads[otherEmail] = 0
                            return@forEach
                        }
                        val isGrp = otherEmail.startsWith("group_")
                        val chatKey = if (isGrp) otherEmail else listOf(current.email, otherEmail).sorted().joinToString("__")
                        val chatKeySanitized = if (isGrp) sanitizeId(otherEmail) else listOf(current.email, otherEmail).sorted().map(::sanitizeId).joinToString("__")
                        
                        val lastActivity = (remoteLastActivity?.get(chatKeySanitized) as? Number)?.toLong() ?: 0L
                        val lastSender = remoteLastSender?.get(chatKeySanitized)?.toString() ?: ""
                        
                        if (lastSender.isNotEmpty()) {
                            lastMsgsSenderMap[otherEmail] = lastSender
                        }
                        
                        val mySeenObj = (remoteSeen?.get(chatKeySanitized) as? Map<*, *>)?.get(myUserKey) as? Map<*, *>
                        val mySeenTs = (mySeenObj?.get("ts") as? Number)?.toLong() ?: 0L
                        
                        val localSeenTs = localSeenTimestamps[otherEmail] ?: 0L
                        val loginTime = LocalStorage.getLoginTime(context)
                        val cachedLocalMsgs = LocalStorage.getLocalMessages(context, chatKey)
                        
                        val defaultSeenTs = if (mySeenTs == 0L && localSeenTs == 0L && cachedLocalMsgs.isEmpty()) {
                            loginTime
                        } else {
                            0L
                        }
                        
                        val effectiveSeenTs = maxOf(mySeenTs, localSeenTs, defaultSeenTs)
                        
                        if (lastActivity > 0 && lastSender.isNotEmpty() && lastSender.lowercase() != current.email.lowercase()) {
                            if (lastActivity > effectiveSeenTs) {
                                calculatedUnreads[otherEmail] = 1
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        _myGroups.value = parsedGroups
                        _groupMembers.value = groupMembers
                        _groupCreators.value = groupCreators
                        _groupSubAdmins.value = groupSubAdmins
                        _conversationsLastActivity.value = allActivityMap
                        _lastActiveTimestamps.value = activityMap
                        _lastMessageSenderMap.value = lastMsgsSenderMap
                        _unreadCounts.value = calculatedUnreads
                        LocalStorage.saveUnreadCounts(context, calculatedUnreads)
                    }
                } else {
                    if (_allUsers.value.isEmpty()) {
                        val mocks = getMockContactsList(current.name)
                        _allUsers.value = mocks
                    }
                    _recentChats.value = emptyList()
                }
            } catch (e: Exception) {
                if (_allUsers.value.isEmpty()) {
                    val mocks = getMockContactsList(current.name)
                    _allUsers.value = mocks
                }
                _recentChats.value = emptyList()
            }
        }
    }

    fun loadMessagesForConversation(otherEmail: String, isFirstLoad: Boolean = false) {
        val current = _currentUser.value ?: return
        val spy = _spyingOnUser.value
        val effectiveCurrentUser = spy ?: current
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

                val isGroup = otherEmail.startsWith("group_")
                val chatKey = if (isGroup) otherEmail else listOf(effectiveCurrentUser.email, otherEmail).sorted().joinToString("__")

                val chatKeySanitized = if (isGroup) otherEmail else listOf(effectiveCurrentUser.email, otherEmail).sorted().map(::sanitizeId).joinToString("__")
                
                // Fetch seen read receipts for this conversation
                val seenResult = try {
                    FirebaseRestClient.service.getValue("seen/$chatKeySanitized") as? Map<*, *>
                } catch (e: Exception) {
                    null
                }
                val seenMap = mutableMapOf<String, Long>()
                seenResult?.forEach { (k, v) ->
                    val userSanitized = k?.toString() ?: return@forEach
                    val vMap = v as? Map<*, *>
                    val ts = (vMap?.get("ts") as? Number)?.toLong() ?: 0L
                    seenMap[userSanitized] = ts
                }
                withContext(Dispatchers.Main) {
                    _chatSeenMap.value = seenMap
                }

                val remoteWallpaper = try {
                    FirebaseRestClient.service.getValue("chat_wallpapers/$chatKeySanitized") as? String
                } catch (e: Exception) {
                    null
                }
                val remoteTheme = try {
                    FirebaseRestClient.service.getValue("chat_themes/$chatKeySanitized") as? String
                } catch (e: Exception) {
                    null
                }
                withContext(Dispatchers.Main) {
                    val updatedWall = _perChatWallpaper.value.toMutableMap()
                    if (remoteWallpaper != null) {
                        updatedWall[otherEmail] = remoteWallpaper
                    } else {
                        updatedWall.remove(otherEmail)
                    }
                    _perChatWallpaper.value = updatedWall

                    val updatedTheme = _perChatTheme.value.toMutableMap()
                    if (remoteTheme != null) {
                        updatedTheme[otherEmail] = remoteTheme
                    } else {
                        updatedTheme.remove(otherEmail)
                    }
                    _perChatTheme.value = updatedTheme
                }

                // 1. Fetch pending messages from Supabase under messages/$chatKey
                val supabaseResult = try {
                    SupabaseRestClient.service.getValue("messages/$chatKey") as? Map<*, *>
                } catch (e: Exception) {
                    null
                }

                val supabaseMessages = mutableListOf<ChatMessage>()
                supabaseResult?.forEach { (key, value) ->
                    val msgData = value as? Map<*, *> ?: return@forEach
                    val id = msgData["id"] as? String ?: key.toString()
                    val sender = msgData["sender"] as? String ?: ""
                    val text = msgData["text"] as? String ?: ""
                    val timestamp = (msgData["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    val isOwn = sender.lowercase() == current.email.lowercase() || sender.lowercase() == effectiveCurrentUser.email.lowercase()

                    val replyToMap = msgData["replyTo"] as? Map<*, *>
                    val replyTo = replyToMap?.let {
                        ReplyToData(
                            id = it["id"] as? String ?: "",
                            text = it["text"] as? String ?: "",
                            user = it["user"] as? String ?: ""
                        )
                    }

                    val isPoll = text.startsWith("📊 POLL:")
                    val isImage = text.endsWith(".jpg") || text.endsWith(".png") || text.endsWith(".gif") || text.endsWith(".webp") || text.contains(".jpg?") || text.contains(".png?")

                    val friendlyName = if (sender.lowercase() == current.email.lowercase()) {
                        current.name
                    } else if (sender.lowercase() == effectiveCurrentUser.email.lowercase()) {
                        effectiveCurrentUser.name
                    } else {
                        _allUsers.value.find { it.email.lowercase() == sender.lowercase() }?.name ?: sender.split("@")[0]
                    }

                    supabaseMessages.add(
                        ChatMessage(
                            id = id,
                            senderName = friendlyName,
                            senderEmail = sender,
                            text = text,
                            timestampMs = timestamp,
                            replyTo = replyTo,
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
                    )
                }

                val deletedMap = try {
                    FirebaseRestClient.service.getValue("deleted_messages/$chatKeySanitized") as? Map<*, *>
                } catch (e: Exception) {
                    null
                }
                val deletedIds = deletedMap?.keys?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()

                if (deletedIds.isNotEmpty()) {
                    supabaseMessages.removeAll { it.id in deletedIds }
                }

                if (isGroup) {
                    val groupMsgs = supabaseMessages.map { msg ->
                        val correctIsOwn = msg.senderEmail.lowercase() == current.email.lowercase() || msg.senderEmail.lowercase() == effectiveCurrentUser.email.lowercase()
                        msg.copy(isOwn = correctIsOwn)
                    }.sortedBy { it.timestampMs }
                    withContext(Dispatchers.Main) {
                        _messages.value = groupMsgs
                        _chatLoading.value = false
                        val lastGrpMsg = groupMsgs.lastOrNull()
                        if (lastGrpMsg != null) {
                            val updatedMap = _lastMessageSenderMap.value.toMutableMap()
                            updatedMap[otherEmail] = lastGrpMsg.senderEmail
                            _lastMessageSenderMap.value = updatedMap
                        }
                    }
                    fetchPollVotes()
                    markMessagesSeenForOther(otherEmail)
                    return@launch
                }

                val cachedLocalRaw = LocalStorage.getLocalMessages(context, chatKey)
                val cachedLocal = if (deletedIds.isNotEmpty()) {
                    val filtered = cachedLocalRaw.filter { it.id !in deletedIds }
                    if (filtered.size != cachedLocalRaw.size) {
                        LocalStorage.saveLocalMessages(context, chatKey, filtered)
                    }
                    filtered
                } else {
                    cachedLocalRaw
                }
                val cachedIds = cachedLocal.map { it.id }.toSet()

                // 2. Separate own messages and other person's messages on Supabase
                val (ownMsgs, otherMsgs) = supabaseMessages.partition { it.isOwn }

                // 3. Automatically save any messages (both own and incoming) that are NOT yet in cachedLocal and delete from Supabase
                val unviewedIncoming = otherMsgs.filter { it.id !in cachedIds }
                val unviewedOwn = ownMsgs.filter { it.id !in cachedIds }
                val anyNewMessages = unviewedIncoming + unviewedOwn
                
                val updatedCachedLocal = if (anyNewMessages.isNotEmpty()) {
                    val updatedLocal = (cachedLocal + anyNewMessages).distinctBy { it.id }.sortedBy { it.timestampMs }
                    LocalStorage.saveLocalMessages(context, chatKey, updatedLocal)
                    
                    unviewedIncoming.forEach { msg ->
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                SupabaseRestClient.service.deleteValue("messages/$chatKey/${msg.id}")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    updatedLocal
                } else {
                    cachedLocal
                }

                // Delete any incoming messages from Supabase that are ALREADY in cachedLocal (leak prevention)
                otherMsgs.filter { it.id in cachedIds }.forEach { msg ->
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            SupabaseRestClient.service.deleteValue("messages/$chatKey/${msg.id}")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                // 4. Combine cached local messages and pending own messages with delivery status calculations
                val ownIdsOnServer = ownMsgs.map { it.id }.toSet()
                val partnerOnline = _partnerOnlineStatus.value
                val partnerKey = sanitizeId(otherEmail)

                val partnerSeenTs = try {
                    val seenData = FirebaseRestClient.service.getValue("seen/$chatKeySanitized/$partnerKey") as? Map<*, *>
                    (seenData?.get("ts") as? Number)?.toLong() ?: 0L
                } catch (e: Exception) {
                    0L
                }

                // Delete our own messages from Supabase if they are seen by the partner
                ownMsgs.filter { it.timestampMs <= partnerSeenTs }.forEach { msg ->
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            SupabaseRestClient.service.deleteValue("messages/$chatKey/${msg.id}")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                val finalMessages = (updatedCachedLocal + ownMsgs)
                    .distinctBy { it.id }
                    .map { msg ->
                        val correctIsOwn = msg.senderEmail.lowercase() == current.email.lowercase() || msg.senderEmail.lowercase() == effectiveCurrentUser.email.lowercase()
                        val updatedMsg = msg.copy(isOwn = correctIsOwn)
                        if (correctIsOwn) {
                            val isDelivered = updatedMsg.id in deliveredMessageIds || partnerOnline == "online"
                            val status = if (updatedMsg.timestampMs <= partnerSeenTs) {
                                "seen"
                            } else if (updatedMsg.id in ownIdsOnServer) {
                                if (isDelivered) {
                                    saveDeliveredMessageId(updatedMsg.id)
                                    "delivered"
                                } else {
                                    "sent"
                                }
                            } else {
                                "seen"
                            }
                            updatedMsg.copy(deliveryStatus = status)
                        } else {
                            updatedMsg
                        }
                    }
                    .sortedBy { it.timestampMs }
                    .ifEmpty { getMockMessagesFor(effectiveCurrentUser.email, otherEmail) }

                withContext(Dispatchers.Main) {
                    _messages.value = finalMessages
                    val lastPrivateMsg = finalMessages.lastOrNull()
                    if (lastPrivateMsg != null) {
                        val updatedMap = _lastMessageSenderMap.value.toMutableMap()
                        updatedMap[otherEmail] = lastPrivateMsg.senderEmail
                        _lastMessageSenderMap.value = updatedMap
                    }
                }

                // Sync seen read receipt if current chat is active
                val hasNewIncoming = unviewedIncoming.isNotEmpty()
                if (hasNewIncoming) {
                    markMessagesSeenForOther(otherEmail, force = true)
                } else {
                    markMessagesSeenForOther(otherEmail)
                }

            } catch (e: Exception) {
                val chatKey = listOf(effectiveCurrentUser.email, otherEmail).sorted().joinToString("__")
                val cachedLocal = LocalStorage.getLocalMessages(context, chatKey)
                val mappedCached = cachedLocal.map { msg ->
                    val correctIsOwn = msg.senderEmail.lowercase() == current.email.lowercase() || msg.senderEmail.lowercase() == effectiveCurrentUser.email.lowercase()
                    msg.copy(isOwn = correctIsOwn)
                }
                withContext(Dispatchers.Main) {
                    val finalCached = mappedCached.ifEmpty { getMockMessagesFor(effectiveCurrentUser.email, otherEmail) }
                    _messages.value = finalCached
                    val lastCachedMsg = finalCached.lastOrNull()
                    if (lastCachedMsg != null) {
                        val updatedMap = _lastMessageSenderMap.value.toMutableMap()
                        updatedMap[otherEmail] = lastCachedMsg.senderEmail
                        _lastMessageSenderMap.value = updatedMap
                    }
                }
            } finally {
                _chatLoading.value = false
            }
        }
    }

    fun selectChatUser(user: User?) {
        val previousUser = _currentChatUser.value
        _currentChatUser.value = user
        _messages.value = emptyList()
        if (user != null) {
            if (_deletedConversations.value.contains(user.email)) {
                restoreConversation(user.email)
            }
            val currentRecents = _recentChats.value.toMutableList()
            if (currentRecents.none { it.email.lowercase() == user.email.lowercase() }) {
                currentRecents.add(0, user)
                _recentChats.value = currentRecents
            }
            localSeenTimestamps[user.email] = System.currentTimeMillis() + 10000L
            
            val current = _currentUser.value
            val isGrp = user.email.startsWith("group_")
            val chatKey = if (isGrp) user.email else {
                if (current != null) {
                    listOf(current.email, user.email).sorted().map(::sanitizeId).joinToString("__")
                } else {
                    sanitizeId(user.email)
                }
            }
            LocalStorage.savePersistedSeenTimestamp(context, chatKey, System.currentTimeMillis() + 10000L)

            _unreadCounts.value = _unreadCounts.value.toMutableMap().apply { remove(user.email) }
            LocalStorage.saveUnreadCounts(context, _unreadCounts.value)
            loadMessagesForConversation(user.email, isFirstLoad = true)
            markMessagesSeenForOther(user.email, force = true)
        } else {
            if (previousUser != null) {
                stopTypingForUser(previousUser.email)
            }
        }
    }

    fun viewAndDecryptMessage(otherEmail: String, msg: ChatMessage) {
        val current = _currentUser.value ?: return
        val spy = _spyingOnUser.value
        val effectiveCurrentUser = spy ?: current
        val isGroup = otherEmail.startsWith("group_")
        val chatKey = if (isGroup) otherEmail else listOf(effectiveCurrentUser.email, otherEmail).sorted().joinToString("__")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Copy the message with isUnviewedServerMessage = false
                val viewedMsg = msg.copy(isUnviewedServerMessage = false)

                // 2. Save it to local cache
                val currentCached = LocalStorage.getLocalMessages(context, chatKey)
                val updated = (currentCached + viewedMsg).distinctBy { it.id }.sortedBy { it.timestampMs }
                LocalStorage.saveLocalMessages(context, chatKey, updated)

                if (!isGroup) {
                    // 3. Delete it from Supabase (the server)
                    SupabaseRestClient.service.deleteValue("messages/$chatKey/${msg.id}")
                }

                // Update local seen timestamp
                localSeenTimestamps[otherEmail] = System.currentTimeMillis() + 10000L
                val chatKeySanitized = if (isGroup) sanitizeId(otherEmail) else listOf(effectiveCurrentUser.email, otherEmail).sorted().map(::sanitizeId).joinToString("__")
                LocalStorage.savePersistedSeenTimestamp(context, chatKeySanitized, System.currentTimeMillis() + 10000L)

                // 4. Reload the conversation
                loadMessagesForConversation(otherEmail)

                // 5. Explicitly update seen read receipts on Firebase
                markMessagesSeenForOther(otherEmail, force = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMessage(text: String, replyTo: ReplyToData? = null) {
        val current = _currentUser.value ?: return
        val chatUser = _currentChatUser.value ?: return
        val isGroup = chatUser.email.startsWith("group_")

        // Instant prepending to make sure sent-to user is at the top of recentChats/dashboard
        val currentRecents = _recentChats.value.toMutableList()
        currentRecents.removeAll { it.email.lowercase() == chatUser.email.lowercase() }
        currentRecents.add(0, chatUser)
        _recentChats.value = currentRecents

        if (!isGroup) {
            val isSenderRafid = isRafidUser(current)
            val isRecipientRafid = isRafidUser(chatUser)
            if (!isSenderRafid && !isRecipientRafid) {
                // Block if recipient name ends with '°', unless the sender is that user themselves or is agreed
                if (chatUser.name.trim().endsWith("°") && current.email.lowercase() != chatUser.email.lowercase()) {
                    val isSenderAgreed = _agreedUsers.value[sanitizeId(chatUser.email)]?.get(sanitizeId(current.email)) == true
                    if (!isSenderAgreed) {
                        _authError.value = "এই ব্যবহারকারীকে বার্তা পাঠানো সম্ভব নয়।"
                        return
                    }
                }
            }

            if (_deletedConversations.value.contains(chatUser.email)) {
                restoreConversation(chatUser.email)
            }
        }

        _isSending.value = true

        val updatedMap = _lastActiveTimestamps.value.toMutableMap()
        updatedMap[chatUser.email] = System.currentTimeMillis()
        _lastActiveTimestamps.value = updatedMap

        viewModelScope.launch(Dispatchers.IO) {
            val isRafid = isRafidUser(current)
            val isOffensive = if (isRafid) false else checkIsOffensiveContent(text)

            val spy = _spyingOnUser.value
            val effectiveCurrentUser = spy ?: current
            val chatKey = if (isGroup) chatUser.email else listOf(effectiveCurrentUser.email, chatUser.email).sorted().joinToString("__")

            if (isOffensive) {
                val sysId = "sys_" + System.currentTimeMillis() + "_" + (1000..9999).random()
                val sysMap = mapOf(
                    "id" to sysId,
                    "sender" to "system@echochat.com",
                    "text" to "আপনার ভাষা ঠিক করুন নয়তো আপনার অ্যাকাউন্ট বন্ধ করা হবে।",
                    "timestamp" to System.currentTimeMillis()
                )
                try {
                    SupabaseRestClient.service.setValue("messages/$chatKey/$sysId", sysMap)
                    val sanitizedChatId = sanitizeId(chatUser.email)
                    FirebaseRestClient.service.setValue("conversations_last_activity/$sanitizedChatId", System.currentTimeMillis())
                    FirebaseRestClient.service.setValue("conversations_last_sender/$sanitizedChatId", "system@echochat.com")
                    if (!isGroup) {
                        val mutualChatKey = listOf(effectiveCurrentUser.email, chatUser.email).sorted().map(::sanitizeId).joinToString("__")
                        FirebaseRestClient.service.setValue("conversations_last_activity/$mutualChatKey", System.currentTimeMillis())
                        FirebaseRestClient.service.setValue("conversations_last_sender/$mutualChatKey", "system@echochat.com")
                    }
                    loadMessagesForConversation(chatUser.email)
                } catch (e: Exception) {}
                
                val strikeCount = handleUserOffensiveStrike(current.email)
                
                withContext(Dispatchers.Main) {
                    if (strikeCount >= 3) {
                        _isAccountBlocked.value = true
                        logout()
                        _authError.value = "আপনার অ্যাকাউন্টটি ব্লক করা হয়েছে! খারাপ ভাষা ব্যবহারের জন্য আপনার অ্যাকাউন্ট বন্ধ করা হয়েছে।"
                    } else {
                        _offensiveWarningMessage.value = "আপনার ভাষা ঠিক করুন নয়তো আপনার অ্যাকাউন্ট বন্ধ করা হবে।"
                    }
                    _isSending.value = false
                }
                return@launch
            }

            val msgId = "msg_" + System.currentTimeMillis() + "_" + (1000..9999).random()

            val localMsg = ChatMessage(
                id = msgId,
                senderName = effectiveCurrentUser.name,
                senderEmail = effectiveCurrentUser.email,
                text = text,
                timestampMs = System.currentTimeMillis(),
                replyTo = replyTo,
                isOwn = true,
                isLocal = false,
                isImage = text.endsWith(".jpg") || text.endsWith(".png") || text.endsWith(".gif") || text.endsWith(".webp") || text.contains(".jpg?") || text.contains(".png?"),
                imageUrl = if (text.endsWith(".jpg") || text.endsWith(".png") || text.endsWith(".gif") || text.endsWith(".webp") || text.contains(".jpg?") || text.contains(".png?")) {
                    val urlRegex = "(https?://[^\\s]+)".toRegex()
                    urlRegex.find(text)?.value ?: ""
                } else null,
                isPoll = text.startsWith("📊 POLL:"),
                pollQuestion = if (text.startsWith("📊 POLL:")) {
                    text.split("\n").getOrNull(0)?.replace("📊 POLL:", "")?.trim()
                } else null,
                pollOptions = if (text.startsWith("📊 POLL:")) {
                    text.split("\n").drop(1).filter { it.isNotEmpty() }
                } else emptyList()
            )

            // Save to local host immediately
            val updatedLocal = (LocalStorage.getLocalMessages(context, chatKey) + localMsg)
            LocalStorage.saveLocalMessages(context, chatKey, updatedLocal)

            // Check if chatUser is AI assistant (e.g., ends with "+" or is Support Dot Echo)
            if (isAiUser(chatUser)) {
                withContext(Dispatchers.Main) {
                    _isSending.value = false
                }
                handleAiChatResponse(text, chatKey, chatUser)
                return@launch
            }

            try {
                // Post to Supabase
                val msgMap = mapOf(
                    "id" to msgId,
                    "sender" to effectiveCurrentUser.email,
                    "text" to text,
                    "timestamp" to System.currentTimeMillis(),
                    "replyTo" to replyTo?.let { mapOf("id" to it.id, "text" to it.text, "user" to it.user) }
                )
                SupabaseRestClient.service.setValue("messages/$chatKey/$msgId", msgMap)

                // Set last active timestamp and last sender on Firebase
                val sanitizedChatId = sanitizeId(chatUser.email)
                try {
                    FirebaseRestClient.service.setValue("conversations_last_activity/$sanitizedChatId", System.currentTimeMillis())
                    FirebaseRestClient.service.setValue("conversations_last_sender/$sanitizedChatId", effectiveCurrentUser.email)
                    if (!isGroup) {
                        val mutualChatKey = listOf(effectiveCurrentUser.email, chatUser.email).sorted().map(::sanitizeId).joinToString("__")
                        FirebaseRestClient.service.setValue("conversations_last_activity/$mutualChatKey", System.currentTimeMillis())
                        FirebaseRestClient.service.setValue("conversations_last_sender/$mutualChatKey", effectiveCurrentUser.email)
                    }
                } catch(e: Exception) {}

                // Reload
                loadMessagesForConversation(chatUser.email)
            } catch (e: Exception) {
                // Already saved locally, reload to reflect
                loadMessagesForConversation(chatUser.email)
            } finally {
                withContext(Dispatchers.Main) {
                    _isSending.value = false
                    val updatedSenderMap = _lastMessageSenderMap.value.toMutableMap()
                    updatedSenderMap[chatUser.email] = effectiveCurrentUser.email
                    _lastMessageSenderMap.value = updatedSenderMap
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

    private var lastTypingSentTime = 0L

    // Typing Indicators Sync with Firebase
    fun sendTyping() {
        val current = _currentUser.value ?: return
        val chatUser = _currentChatUser.value ?: return
        val chatKey = listOf(current.email, chatUser.email).sorted().map(::sanitizeId).joinToString("__")
        val userKey = sanitizeId(current.email)

        val now = System.currentTimeMillis()
        if (now - lastTypingSentTime < 2500) {
            return // Throttled to avoid database spam on every keystroke
        }
        lastTypingSentTime = now

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

    fun stopTypingForUser(otherEmail: String) {
        val current = _currentUser.value ?: return
        val chatKey = listOf(current.email, otherEmail).sorted().map(::sanitizeId).joinToString("__")
        val userKey = sanitizeId(current.email)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.deleteValue("typing/$chatKey/$userKey")
            } catch (e: Exception) {}
        }
    }

    private suspend fun pollAndSyncFirebaseStatuses() {
        val current = _currentUser.value ?: return
        val userKey = sanitizeId(current.email)
        val chatUser = _currentChatUser.value

        // 1. Sync Typing Partner Status
        if (chatUser != null) {
            val chatKey = listOf(current.email, chatUser.email).sorted().map(::sanitizeId).joinToString("__")
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
        } else {
            _typingPartnerName.value = null
        }

        // 2. Sync Online Statuses of All Users
        try {
            val allOnlineMap = FirebaseRestClient.service.getValue("online") as? Map<*, *>
            if (allOnlineMap != null) {
                val now = System.currentTimeMillis()
                val statuses = mutableMapOf<String, String>()
                allOnlineMap.forEach { (key, value) ->
                    val userK = key?.toString() ?: return@forEach
                    val valMap = value as? Map<*, *> ?: return@forEach
                    val status = (valMap["status"] as? String) ?: "offline"
                    val ts = (valMap["ts"] as? Number)?.toLong() ?: 0L
                    if (now - ts <= 15000) {
                        statuses[userK] = status
                    } else {
                        statuses[userK] = "offline"
                    }
                }
                _usersOnlineStatuses.value = statuses

                // Also update the active partner online status
                if (chatUser != null) {
                    val partnerOnlineKey = sanitizeId(chatUser.email)
                    _partnerOnlineStatus.value = statuses[partnerOnlineKey] ?: "offline"
                } else {
                    _partnerOnlineStatus.value = "offline"
                }
            } else {
                _usersOnlineStatuses.value = emptyMap()
                _partnerOnlineStatus.value = "offline"
            }
        } catch (e: Exception) {
            _usersOnlineStatuses.value = emptyMap()
            _partnerOnlineStatus.value = "offline"
        }

        // 3. Sync Agreed Users
        try {
            val allAgreedMap = FirebaseRestClient.service.getValue("agreed_users") as? Map<*, *>
            if (allAgreedMap != null) {
                val agreementsMap = mutableMapOf<String, Map<String, Boolean>>()
                allAgreedMap.forEach { (specialKey, innerVal) ->
                    val specEmail = specialKey?.toString() ?: return@forEach
                    val innerMap = innerVal as? Map<*, *> ?: return@forEach
                    val specAgreements = mutableMapOf<String, Boolean>()
                    innerMap.forEach { (otherKey, flag) ->
                        val oKeyStr = otherKey?.toString() ?: return@forEach
                        specAgreements[oKeyStr] = true
                    }
                    agreementsMap[specEmail] = specAgreements
                }
                _agreedUsers.value = agreementsMap
            } else {
                _agreedUsers.value = emptyMap()
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

    fun changeLanguage(lang: String) {
        _appLanguage.value = lang
        LocalStorage.setLanguage(context, lang)
    }

    fun secureChat(email: String, pass: String?) {
        LocalStorage.saveSecuredChatPassword(context, email, pass)
        _securedChats.value = LocalStorage.getSecuredChats(context)
        loadAllConversationsAndUsers() // re-map names
    }

    fun hideChat(email: String, key: String?) {
        LocalStorage.saveHiddenChat(context, email, key)
        _hiddenChats.value = LocalStorage.getHiddenChats(context)
        loadAllConversationsAndUsers() // re-map names
    }

    fun deleteConversation(email: String) {
        val current = _currentUser.value
        if (current != null) {
            val isGroup = email.startsWith("group_")
            val chatKey = if (isGroup) email else listOf(current.email, email).sorted().joinToString("__")
            LocalStorage.saveLocalMessages(context, chatKey, emptyList())
        }
        LocalStorage.deleteConversation(context, email)
        LocalStorage.saveSecuredChatPassword(context, email, null)
        _securedChats.value = LocalStorage.getSecuredChats(context)
        LocalStorage.saveHiddenChat(context, email, null)
        _hiddenChats.value = LocalStorage.getHiddenChats(context)
        _deletedConversations.value = LocalStorage.getDeletedConversations(context)
        loadAllConversationsAndUsers()
    }

    fun deleteMessage(msgId: String) {
        val current = _currentUser.value ?: return
        val chatUser = _currentChatUser.value ?: return
        val isGroup = chatUser.email.startsWith("group_")

        viewModelScope.launch(Dispatchers.IO) {
            val chatKey = if (isGroup) chatUser.email else listOf(current.email, chatUser.email).sorted().joinToString("__")
            val chatKeySanitized = sanitizeId(chatKey)
            try {
                FirebaseRestClient.service.setValue("deleted_messages/$chatKeySanitized/$msgId", true)
                if (isGroup) {
                    SupabaseRestClient.service.deleteValue("messages/${chatUser.email}/$msgId")
                } else {
                    // Automatically delete from Google Sheet
                    RetrofitClient.echoChatApi.deleteMessage(
                        url = scriptUrl,
                        id = msgId,
                        sender = current.email
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Trigger local reload. Local messages will still merge and remain in SharedPreferences cache
            loadMessagesForConversation(chatUser.email)
        }
    }

    private suspend fun deleteMessageFromSheetQuietly(msgId: String, senderEmail: String) {
        try {
            RetrofitClient.echoChatApi.deleteMessage(
                url = scriptUrl,
                id = msgId,
                sender = senderEmail
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun parseVersionMessage(message: String, messageId: String? = null): AppVersionInfo? {
        if (!message.startsWith("VERSION_UPDATE: ")) return null
        return try {
            val content = message.substring("VERSION_UPDATE: ".length)
            val parts = content.split("|")
            if (parts.size >= 4) {
                AppVersionInfo(
                    versionNumber = parts[0],
                    title = parts[1],
                    link = parts[2],
                    forceUpdate = parts[3].toBoolean(),
                    messageId = messageId
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun getCurrentAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    fun getSkippedVersion(): String {
        return LocalStorage.getSkippedVersion(context)
    }

    fun setSkippedVersion(version: String) {
        LocalStorage.setSkippedVersion(context, version)
    }

    fun loadVersionsFromAppsScript() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = RetrofitClient.echoChatApi.getAppsScriptVersions(versionScriptUrl)
                if (res.success && res.data != null) {
                    val list = res.data.map { item ->
                        AppVersionInfo(
                            versionNumber = item.version,
                            title = item.title,
                            link = item.link,
                            forceUpdate = item.changes.contains("force_update", ignoreCase = true),
                            messageId = item.id,
                            changes = item.changes
                        )
                    }
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        _versions.value = list
                        if (list.isNotEmpty()) {
                            _latestVersionInfo.value = list.first()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendVersionUpdateToSheet(versionNumber: String, title: String, link: String, changes: String, forceUpdate: Boolean = false, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.echoChatApi.addAppsScriptVersion(
                    url = versionScriptUrl,
                    version = versionNumber,
                    title = title,
                    link = link,
                    changes = changes
                )
                if (response.success) {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        onSuccess()
                        loadAllConversationsAndUsers() // Refresh list
                    }
                } else {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        onError(response.message ?: "সেন্ড করতে সমস্যা হয়েছে")
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "একটি ত্রুটি ঘটেছে")
                }
            }
        }
    }

    fun deleteVersion(messageId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.echoChatApi.deleteAppsScriptVersion(
                    url = versionScriptUrl,
                    id = messageId
                )
                if (response.success) {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        onSuccess()
                        loadAllConversationsAndUsers() // Refresh list
                    }
                } else {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        onError(response.message ?: "মুছে ফেলতে সমস্যা হয়েছে")
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "মুছে ফেলতে সমস্যা হয়েছে")
                }
            }
        }
    }

    fun editVersion(oldMessageId: String, versionNumber: String, title: String, link: String, changes: String, forceUpdate: Boolean = false, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.echoChatApi.editAppsScriptVersion(
                    url = versionScriptUrl,
                    id = oldMessageId,
                    version = versionNumber,
                    title = title,
                    link = link,
                    changes = changes
                )
                if (response.success) {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        onSuccess()
                        loadAllConversationsAndUsers() // Refresh list
                    }
                } else {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        onError(response.message ?: "আপডেট করতে সমস্যা হয়েছে")
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "আপডেট করতে সমস্যা হয়েছে")
                }
            }
        }
    }

    fun restoreConversation(email: String) {
        LocalStorage.restoreConversation(context, email)
        _deletedConversations.value = LocalStorage.getDeletedConversations(context)
        loadAllConversationsAndUsers()
    }

    fun setAppLockEnabled(enabled: Boolean) {
        LocalStorage.setAppLockEnabled(context, enabled)
        _isAppLockEnabled.value = enabled
        if (!enabled) {
            _isAppLocked.value = false
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        LocalStorage.setBiometricEnabled(context, enabled)
        _isBiometricEnabled.value = enabled
    }

    fun setAppLockPIN(pin: String?) {
        LocalStorage.setAppLockPIN(context, pin)
        _appLockPIN.value = pin
        if (pin == null) {
            setAppLockEnabled(false)
        }
    }

    fun setAppLockTimeout(timeoutMs: Long) {
        LocalStorage.setAppLockTimeout(context, timeoutMs)
        _appLockTimeoutMs.value = timeoutMs
    }

    fun onAppStartResume() {
        val isEnabled = _isAppLockEnabled.value && _appLockPIN.value != null
        if (isEnabled) {
            val lastBg = LocalStorage.getLastBackgroundTime(context)
            if (lastBg > 0) {
                val elapsed = System.currentTimeMillis() - lastBg
                val timeout = _appLockTimeoutMs.value
                if (elapsed >= timeout) {
                    _isAppLocked.value = true
                }
            } else {
                _isAppLocked.value = true
            }
        }
    }

    fun onAppBackground() {
        LocalStorage.setLastBackgroundTime(context, System.currentTimeMillis())
    }

    fun setAppLocked(locked: Boolean) {
        if (locked && _isAppLockEnabled.value && _appLockPIN.value != null) {
            _isAppLocked.value = true
        } else if (!locked) {
            _isAppLocked.value = false
        }
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

    fun getPairingCode(fromEmail: String, toEmail: String): String {
        val sortedString = listOf(fromEmail.lowercase().trim(), toEmail.lowercase().trim()).sorted().joinToString("|")
        val hash = sortedString.hashCode().coerceAtLeast(0)
        return String.format("%06d", hash % 1000000)
    }

    fun acceptPairRequestWithCode(fromEmail: String, enteredCode: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val current = _currentUser.value ?: return
        val correctCode = getPairingCode(fromEmail, current.email)
        if (enteredCode.trim() == correctCode) {
            respondToPairRequest(fromEmail, accept = true)
            onSuccess()
        } else {
            onError("ভুল পেয়ারিং কোড! দয়া করে আবার চেষ্টা করুন।")
        }
    }

    fun setPerChatWallpaper(otherEmail: String, wallpaper: String) {
        val current = _currentUser.value ?: return
        val chatKeySanitized = listOf(current.email, otherEmail).sorted().map(::sanitizeId).joinToString("__")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.setValue("chat_wallpapers/$chatKeySanitized", wallpaper)
                withContext(Dispatchers.Main) {
                    val updated = _perChatWallpaper.value.toMutableMap()
                    updated[otherEmail] = wallpaper
                    _perChatWallpaper.value = updated
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setPerChatTheme(otherEmail: String, theme: String) {
        val current = _currentUser.value ?: return
        val chatKeySanitized = listOf(current.email, otherEmail).sorted().map(::sanitizeId).joinToString("__")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.setValue("chat_themes/$chatKeySanitized", theme)
                withContext(Dispatchers.Main) {
                    val updated = _perChatTheme.value.toMutableMap()
                    updated[otherEmail] = theme
                    _perChatTheme.value = updated
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pairDirectlyWithUser(targetEmail: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val current = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sendRes = RetrofitClient.echoChatApi.sendPairRequest(scriptUrl, fromEmail = current.email, toEmail = targetEmail)
                val respondRes = RetrofitClient.echoChatApi.respondPairRequest(
                    url = scriptUrl,
                    fromEmail = current.email,
                    toEmail = targetEmail,
                    accept = "true"
                )
                
                FirebaseRestClient.service.deleteValue("pairing_codes/${sanitizeId(targetEmail)}")
                FirebaseRestClient.service.deleteValue("pairing_codes/${sanitizeId(current.email)}")
                
                requestPairRecoveryData()
                
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("সংযোগ সম্পন্ন করতে সমস্যা হয়েছে: ${e.message}")
                }
            }
        }
    }

    fun removePair(password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val current = _currentUser.value ?: return
        _authLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
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
                        onError(res.message ?: "Failed to remove pair")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _authLoading.value = false
                    onError("Network error occurred")
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
                    val code = getPairingCode(current.email, toEmail)
                    sendPairingCodeFirebase(toEmail, code)
                    withContext(Dispatchers.Main) { onSuccess() }
                } else {
                    withContext(Dispatchers.Main) { onError(sendRes.message ?: "Could not send pairing request") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("Network error occurred") }
            }
        }
    }

    fun sendPairingCodeFirebase(targetEmail: String, code: String) {
        val current = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = mapOf(
                    "fromEmail" to current.email,
                    "fromName" to current.name,
                    "code" to code
                )
                FirebaseRestClient.service.setValue("pairing_codes/${sanitizeId(targetEmail)}", data)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removePairingCodeFirebase(myEmail: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.deleteValue("pairing_codes/${sanitizeId(myEmail)}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun pollPairRequestsAndRecoveryCode(user: User) {
        try {
            // Check pairing status to detect real-time pairings or unpairings
            try {
                val status = RetrofitClient.echoChatApi.getPairStatus(scriptUrl, email = user.email)
                if (status.status == "success" && status.partner != null) {
                    if (_pairPartnerEmail.value != status.partner) {
                        withContext(Dispatchers.Main) {
                            _pairPartnerEmail.value = status.partner
                            _pairPartnerName.value = status.partner.split("@")[0]
                        }
                    }
                } else {
                    if (_pairPartnerEmail.value.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            _pairPartnerEmail.value = ""
                            _pairPartnerName.value = ""
                        }
                    }
                }
            } catch (e: Exception) {}

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

            // Check incoming pairing code from Firebase
            val fbCodeData = FirebaseRestClient.service.getValue("pairing_codes/${sanitizeId(user.email)}") as? Map<*, *>
            if (fbCodeData != null) {
                val fromEmail = fbCodeData["fromEmail"] as? String ?: ""
                val fromName = fbCodeData["fromName"] as? String ?: ""
                val code = fbCodeData["code"] as? String ?: ""
                withContext(Dispatchers.Main) {
                    _incomingPairCodeData.value = mapOf("fromEmail" to fromEmail, "fromName" to fromName, "code" to code)
                }
            } else {
                withContext(Dispatchers.Main) {
                    _incomingPairCodeData.value = null
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

    fun changePassword(oldPass: String, newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val current = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Verify old password by logging in
                val loginRes = RetrofitClient.echoChatApi.login(scriptUrl, email = current.email, oldPassword = oldPass)
                if (loginRes.status != "success") {
                    withContext(Dispatchers.Main) {
                        onError("বর্তমান পাসওয়ার্ডটি সঠিক নয়")
                    }
                    return@launch
                }
                
                // 2. Call resetPassword API to update on backend
                val res = RetrofitClient.echoChatApi.resetPassword(scriptUrl, email = current.email, newPassword = newPass)
                if (res.status == "success") {
                    // Update local cached password
                    LocalStorage.saveLoggedInUser(context, current, newPass)
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError(res.message ?: "পাসওয়ার্ড পরিবর্তন ব্যর্থ হয়েছে")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("নেটওয়ার্ক ত্রুটি")
                }
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
        viewModelScope.launch(Dispatchers.Main) {
            android.widget.Toast.makeText(context, "এই অপশনটি আপডেটের পরে আসবে", android.widget.Toast.LENGTH_LONG).show()
        }
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
            "browser" to "Echo Chat",
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

    private val lastSeenSyncTime = mutableMapOf<String, Long>()
    private val lastSeenMessageIdMap = java.util.concurrent.ConcurrentHashMap<String, String>()

    private fun markMessagesSeenForOther(otherEmail: String, force: Boolean = false) {
        val current = _currentUser.value ?: return
        val spy = _spyingOnUser.value
        val effectiveCurrentUser = spy ?: current
        val now = System.currentTimeMillis()
        
        // Optimize: check if last message has already been seen
        val lastMsg = _messages.value.lastOrNull()
        if (lastMsg != null) {
            val lastSeenMsgId = lastSeenMessageIdMap[otherEmail]
            if (!force && lastSeenMsgId == lastMsg.id) {
                return // Skip database call if the same message was already marked as seen
            }
            lastSeenMessageIdMap[otherEmail] = lastMsg.id
        }

        val lastSync = lastSeenSyncTime[otherEmail] ?: 0L
        if (!force && now - lastSync < 15000) {
            return // Skip to avoid spamming the database
        }
        lastSeenSyncTime[otherEmail] = now

        val isGroup = otherEmail.startsWith("group_")
        val chatKey = if (isGroup) otherEmail else listOf(effectiveCurrentUser.email, otherEmail).sorted().map(::sanitizeId).joinToString("__")
        val userKey = sanitizeId(effectiveCurrentUser.email)
        LocalStorage.savePersistedSeenTimestamp(context, chatKey, System.currentTimeMillis() + 10000L)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.setValue("seen/$chatKey/$userKey", mapOf("ts" to System.currentTimeMillis()))
            } catch (e: Exception) {}
        }
    }

    fun createOrUpdateGroup(groupId: String?, name: String, photoUrl: String, members: List<String>, onComplete: (Boolean) -> Unit) {
        val current = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val finalGroupId = groupId ?: ("group_" + System.currentTimeMillis() + "_" + (1000..9999).random())
                var subAdminsStr = ""
                var creator = current.email
                
                // If editing, try to preserve creator and subAdmins
                if (groupId != null) {
                    try {
                        val remoteGroups = SupabaseRestClient.service.getValue("groups") as? Map<*, *>
                        val gMap = remoteGroups?.get(groupId) as? Map<*, *>
                        if (gMap != null) {
                            subAdminsStr = gMap["subAdmins"] as? String ?: ""
                            creator = gMap["createdBy"] as? String ?: current.email
                        }
                    } catch (ex: Exception) {}
                }

                val membersStr = (members + current.email).distinct().joinToString(",")
                val groupMap = mapOf(
                    "id" to finalGroupId,
                    "name" to name,
                    "photoUrl" to photoUrl,
                    "members" to membersStr,
                    "createdBy" to creator,
                    "subAdmins" to subAdminsStr
                )
                SupabaseRestClient.service.setValue("groups/$finalGroupId", groupMap)
                
                // Reload conversations to reflect changes immediately
                loadAllConversationsAndUsers()
                
                withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    fun deleteGroup(groupId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                SupabaseRestClient.service.deleteValue("groups/$groupId")
                deleteConversation(groupId)
                withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                deleteConversation(groupId)
                withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            }
        }
    }

    fun toggleSubAdmin(groupId: String, userEmail: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val remoteGroups = SupabaseRestClient.service.getValue("groups") as? Map<*, *> ?: return@launch
                val gMap = remoteGroups[groupId] as? Map<*, *> ?: return@launch
                val subAdminsRaw = gMap["subAdmins"] as? String ?: ""
                var subAdminsList = subAdminsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                
                if (subAdminsList.contains(userEmail)) {
                    subAdminsList.remove(userEmail)
                } else {
                    subAdminsList.add(userEmail)
                }
                
                val updatedGroupMap = gMap.toMutableMap().apply {
                    put("subAdmins", subAdminsList.joinToString(","))
                }
                SupabaseRestClient.service.setValue("groups/$groupId", updatedGroupMap)
                loadAllConversationsAndUsers() // Refresh
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun makeEveryoneSubAdmin(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val remoteGroups = SupabaseRestClient.service.getValue("groups") as? Map<*, *> ?: return@launch
                val gMap = remoteGroups[groupId] as? Map<*, *> ?: return@launch
                val membersRaw = gMap["members"] as? String ?: ""
                val createdBy = gMap["createdBy"] as? String ?: ""
                
                val membersList = membersRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                // Sub-admins should be all members except the main creator
                val subAdminsList = membersList.filter { it != createdBy }
                
                val updatedGroupMap = gMap.toMutableMap().apply {
                    put("subAdmins", subAdminsList.joinToString(","))
                }
                SupabaseRestClient.service.setValue("groups/$groupId", updatedGroupMap)
                loadAllConversationsAndUsers() // Refresh
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun castVote(pollMessageId: String, optionIndex: Int) {
        val current = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userKey = sanitizeId(current.email)
                // Save vote: polls/pollId/votes/userKey = optionIndex
                SupabaseRestClient.service.setValue("polls/$pollMessageId/votes/$userKey", optionIndex)
                // Reload active messages to show results instantly
                _currentChatUser.value?.let { loadMessagesForConversation(it.email) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchPollVotes() {
        val current = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val remotePolls = SupabaseRestClient.service.getValue("polls") as? Map<*, *>
                val parsedPollVotes = mutableMapOf<String, Map<Int, Int>>() // pollId -> optionIndex -> count
                val parsedUserVotes = mutableMapOf<String, Int>() // pollId -> optionIndex chosen by me
                
                remotePolls?.forEach { (pollKey, pollVal) ->
                    val pollId = pollKey?.toString() ?: return@forEach
                    val pollValData = pollVal as? Map<*, *> ?: return@forEach
                    val votesData = pollValData["votes"] as? Map<*, *> ?: return@forEach
                    
                    val optionCounts = mutableMapOf<Int, Int>()
                    var chosenIndex: Int? = null
                    
                    votesData.forEach { (userKey, optVal) ->
                        val optIndex = (optVal as? Number)?.toInt() ?: return@forEach
                        val userKeyStr = userKey?.toString() ?: return@forEach
                        
                        optionCounts[optIndex] = (optionCounts[optIndex] ?: 0) + 1
                        
                        if (userKeyStr == sanitizeId(current.email)) {
                            chosenIndex = optIndex
                        }
                    }
                    
                    parsedPollVotes[pollId] = optionCounts
                    chosenIndex?.let {
                        parsedUserVotes[pollId] = it
                    }
                }
                
                withContext(Dispatchers.Main) {
                    _currentPollVotes.value = parsedPollVotes
                    _currentUserVotes.value = parsedUserVotes
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Offensive Warning State
    private val _offensiveWarningMessage = MutableStateFlow<String?>(null)
    val offensiveWarningMessage: StateFlow<String?> = _offensiveWarningMessage.asStateFlow()

    fun dismissOffensiveWarning() {
        _offensiveWarningMessage.value = null
    }

    // Call Gemini REST API
    suspend fun callGeminiAPI(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: API Key is not configured."
        }
        
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        
        val rootJson = JSONObject()
        
        val contentsArray = org.json.JSONArray()
        val contentObj = JSONObject()
        val partsArray = org.json.JSONArray()
        val partObj = JSONObject()
        partObj.put("text", prompt)
        partsArray.put(partObj)
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        rootJson.put("contents", contentsArray)
        
        if (systemInstruction != null) {
            val sysObj = JSONObject()
            val sysParts = org.json.JSONArray()
            val sysPartObj = JSONObject()
            sysPartObj.put("text", systemInstruction)
            sysParts.put(sysPartObj)
            sysObj.put("parts", sysParts)
            rootJson.put("systemInstruction", sysObj)
        }
        
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = rootJson.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
            
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Error: ${response.code} ${response.message}"
                }
                val bodyStr = response.body?.string() ?: return@withContext "Error: Empty response"
                val json = JSONObject(bodyStr)
                val candidates = json.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text")
                text ?: "No text returned."
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    // Content Moderation check
    suspend fun checkIsOffensiveContent(text: String): Boolean {
        val lower = text.lowercase().trim()
        val adultKeywords = listOf(
            "porn", "sexy", "adult", "sex", "xvideo", "pornstar", "call girl", "escort", 
            "adult contact", "নগ্ন", "যৌন", "সেক্স", "চটি", "পর্ন", "খারাপ কথা", "এডাল্ট",
            "যৌন মিলন", "যৌন সম্পর্ক", "গার্লফ্রেন্ড চাই", "ভিডিও কল সেক্স", "হট ভিডিও", "hot video",
            "ধর্ষণ", "বেশ্যা", "মাগী", "খানকি", "bainchud", "motherchud", "baal", "buda", "magi",
            "shala", "gandu", "gand", "bastard", "bhodrolaiz", "khanki", "chodna", "chud"
        )
        
        // 1. Check local keywords first (fast/instant check)
        val mergedKeywords = adultKeywords + _customBadWords.value
        if (mergedKeywords.any { lower.contains(it) }) {
            return true
        }

        // 2. AI checking using Gemini
        try {
            val systemInstruction = "You are a content moderation assistant. Analyze the given message for any offensive language, insults, slangs, bad words, abuse, or adult content (sexual/explicit) in Bengali, English, Benglish, or Hinglish. " +
                    "Specifically detect Bengali slangs and abuses, including bad words like Bainchud, Motherchud, and their variations. " +
                    "Output exactly 'OFFENSIVE' if the message contains any offensive, bad, slang, or adult language. Otherwise, output 'CLEAN'. Do not include any other text."
            
            val aiResponse = callGeminiAPI(text, systemInstruction).trim().uppercase()
            if (aiResponse.contains("OFFENSIVE")) {
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    // Report User implementation
    fun reportUser(reportedEmail: String, reason: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val current = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reportId = "report_" + System.currentTimeMillis()
                val reportData = mapOf(
                    "id" to reportId,
                    "reporter" to current.email,
                    "reported" to reportedEmail,
                    "reason" to reason,
                    "timestamp" to System.currentTimeMillis()
                )
                FirebaseRestClient.service.setValue("reports/$reportId", reportData)
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("রিপোর্ট সাবমিট করতে সমস্যা হয়েছে: ${e.message}")
                }
            }
        }
    }

    // Handle AI Chat Response
    enum class AiFlow {
        NONE,
        CHANGE_PASSWORD_EMAIL,
        CHANGE_PASSWORD_OLD_PASS,
        CHANGE_PASSWORD_NEW_PASS,
        FORGOT_PASSWORD_EMAIL,
        FORGOT_PASSWORD_CODE,
        FORGOT_PASSWORD_NEW_PASS,
        REPORT_USER_EMAIL,
        REPORT_USER_REASON
    }

    private val _aiFlowState = MutableStateFlow(AiFlow.NONE)
    private val _aiFlowData = mutableMapOf<String, String>()

    fun isAiUser(user: User?): Boolean {
        if (user == null) return false
        val nameLower = user.name.lowercase().trim()
        val emailLower = user.email.lowercase().trim()
        return nameLower.endsWith("+") || 
               nameLower.contains("সাপোর্ট") || 
               nameLower.contains("support") || 
               nameLower.contains("echo") || 
               emailLower.contains("support") || 
               emailLower.contains("echo")
    }

    fun setUserOnlineStatus(status: String) {
        val current = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            updateFirebaseOnline(status)
        }
    }

    private suspend fun handleAiChatResponse(userMsg: String, chatKey: String, chatUser: User) {
        val current = _currentUser.value ?: return
        val normalizedMsg = userMsg.trim()

        var responseText = ""

        // Reset flow if user explicitly says the main command
        val isTriggerChangePass = normalizedMsg == "পাসওয়ার্ড পরিবর্তন" || normalizedMsg.lowercase().contains("change password")
        val isTriggerForgot = normalizedMsg == "পাসওয়ার্ড ফরগেট" || normalizedMsg.lowercase().contains("forgot password") || normalizedMsg.lowercase().contains("forget password")
        val isTriggerReport = normalizedMsg == "ইউজার রিপোর্ট" || normalizedMsg.lowercase().contains("report user")

        if (isTriggerChangePass) {
            _aiFlowState.value = AiFlow.CHANGE_PASSWORD_EMAIL
            _aiFlowData.clear()
            responseText = "🔓 পাসওয়ার্ড পরিবর্তন করার জন্য প্রথমে আপনার ইমেইল এড্রেসটি লিখুন:"
        } else if (isTriggerForgot) {
            _aiFlowState.value = AiFlow.FORGOT_PASSWORD_EMAIL
            _aiFlowData.clear()
            responseText = "🔑 পাসওয়ার্ড ফরগেট (রিসেট) করার জন্য প্রথমে আপনার ইমেইল এড্রেসটি লিখুন:"
        } else if (isTriggerReport) {
            _aiFlowState.value = AiFlow.REPORT_USER_EMAIL
            _aiFlowData.clear()
            responseText = "⚠️ ইউজার রিপোর্ট করার জন্য প্রথমে আপনি যে ব্যবহারকারীকে রিপোর্ট করতে চান তার ইমেইল এড্রেসটি লিখুন:"
        } else {
            // Process based on current state
            when (_aiFlowState.value) {
                AiFlow.NONE -> {
                    val systemInstruction = """
                        You are Support Dot Echo AI, a highly capable Bengali support assistant inside the EchoChat application.
                        The user is chatting with you. You can help them with:
                        1. Password Change (পাসওয়ার্ড পরিবর্তন)
                        2. Report a User (ইউজার রিপোর্ট)
                        3. Forget Password (পাসওয়ার্ড ফরগেট)
                        
                        Tell them they can also trigger these actions directly by clicking the quick action buttons at the top of this chat screen, or simply write:
                        - "পাসওয়ার্ড পরিবর্তন"
                        - "ইউজার রিপোর্ট"
                        - "পাসওয়ার্ড ফরগেট"
                        
                        Always reply politely in Bengali. Keep your responses engaging, friendly, and helpful.
                    """.trimIndent()
                    
                    val prompt = "User says: $userMsg"
                    responseText = try {
                        callGeminiAPI(prompt, systemInstruction)
                    } catch (e: Exception) {
                        "দুঃখিত, আমি এই মুহূর্তে উত্তর দিতে পারছি না। অনুগ্রহ করে আবার চেষ্টা করুন।"
                    }
                }
                
                AiFlow.CHANGE_PASSWORD_EMAIL -> {
                    _aiFlowData["email"] = normalizedMsg
                    _aiFlowState.value = AiFlow.CHANGE_PASSWORD_OLD_PASS
                    responseText = "ধন্যবাদ। এবার আপনার বর্তমান পুরাতন পাসওয়ার্ডটি (Old Password) লিখুন:"
                }
                
                AiFlow.CHANGE_PASSWORD_OLD_PASS -> {
                    _aiFlowData["old_pass"] = normalizedMsg
                    _aiFlowState.value = AiFlow.CHANGE_PASSWORD_NEW_PASS
                    responseText = "ধন্যবাদ। এবার আপনার নতুন পাসওয়ার্ডটি (Confirm Password) লিখুন:"
                }
                
                AiFlow.CHANGE_PASSWORD_NEW_PASS -> {
                    val email = _aiFlowData["email"] ?: ""
                    val oldPass = _aiFlowData["old_pass"] ?: ""
                    val newPass = normalizedMsg
                    
                    responseText = "পাসওয়ার্ড পরিবর্তন করা হচ্ছে, অনুগ্রহ করে অপেক্ষা করুন..."
                    
                    val resCompletable = kotlinx.coroutines.CompletableDeferred<String>()
                    changePassword(
                        oldPass = oldPass,
                        newPass = newPass,
                        onSuccess = {
                            resCompletable.complete("✅ অভিনন্দন! আপনার পাসওয়ার্ড সফলভাবে পরিবর্তন করা হয়েছে।")
                        },
                        onError = { error ->
                            resCompletable.complete("❌ পাসওয়ার্ড পরিবর্তন ব্যর্থ হয়েছে। কারণ: $error। পুনরায় চেষ্টা করতে 'পাসওয়ার্ড পরিবর্তন' লিখুন।")
                        }
                    )
                    
                    responseText = resCompletable.await()
                    _aiFlowState.value = AiFlow.NONE
                    _aiFlowData.clear()
                }
                
                AiFlow.FORGOT_PASSWORD_EMAIL -> {
                    _aiFlowData["email"] = normalizedMsg
                    _aiFlowState.value = AiFlow.FORGOT_PASSWORD_CODE
                    responseText = "ধন্যবাদ। এবার আপনার সিকিউরিটি কোড (Recovery/Pairing Code) লিখুন:"
                }
                
                AiFlow.FORGOT_PASSWORD_CODE -> {
                    _aiFlowData["code"] = normalizedMsg
                    _aiFlowState.value = AiFlow.FORGOT_PASSWORD_NEW_PASS
                    responseText = "কোড ভেরিফিকেশন সফল হয়েছে। এবার আপনার নতুন পাসওয়ার্ডটি লিখুন:"
                }
                
                AiFlow.FORGOT_PASSWORD_NEW_PASS -> {
                    val email = _aiFlowData["email"] ?: ""
                    val code = _aiFlowData["code"] ?: ""
                    val newPass = normalizedMsg
                    
                    responseText = "পাসওয়ার্ড রিসেট করা হচ্ছে, অনুগ্রহ করে অপেক্ষা করুন..."
                    
                    val resCompletable = kotlinx.coroutines.CompletableDeferred<String>()
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val res = RetrofitClient.echoChatApi.resetPassword(scriptUrl, email = email, newPassword = newPass)
                            if (res.status == "success") {
                                resCompletable.complete("✅ অভিনন্দন! আপনার পাসওয়ার্ড সফলভাবে ফরগেট (রিসেট) সম্পন্ন হয়েছে।")
                            } else {
                                resCompletable.complete("❌ পাসওয়ার্ড রিসেট ব্যর্থ হয়েছে: ${res.message}। পুনরায় চেষ্টা করতে 'পাসওয়ার্ড ফরগেট' লিখুন।")
                            }
                        } catch (e: Exception) {
                            resCompletable.complete("❌ পাসওয়ার্ড রিসেট ব্যর্থ হয়েছে (নেটওয়ার্ক ত্রুটি)। পুনরায় চেষ্টা করতে 'পাসওয়ার্ড ফরগেট' লিখুন।")
                        }
                    }
                    
                    responseText = resCompletable.await()
                    _aiFlowState.value = AiFlow.NONE
                    _aiFlowData.clear()
                }
                
                AiFlow.REPORT_USER_EMAIL -> {
                    _aiFlowData["reported_email"] = normalizedMsg
                    _aiFlowState.value = AiFlow.REPORT_USER_REASON
                    responseText = "ধন্যবাদ। এবার ইউজারকে রিপোর্ট করার কারণ বা অভিযোগটি বিস্তারিত লিখুন:"
                }
                
                AiFlow.REPORT_USER_REASON -> {
                    val reportedEmail = _aiFlowData["reported_email"] ?: ""
                    val reason = normalizedMsg
                    
                    responseText = "রিপোর্ট জমা দেওয়া হচ্ছে, অনুগ্রহ করে অপেক্ষা করুন..."
                    
                    val resCompletable = kotlinx.coroutines.CompletableDeferred<String>()
                    reportUser(
                        reportedEmail = reportedEmail,
                        reason = reason,
                        onSuccess = {
                            resCompletable.complete("✅ ধন্যবাদ! আপনার রিপোর্টটি সফলভাবে জমা নেওয়া হয়েছে। আমাদের টিম এটি পর্যালোচনা করবে।")
                        },
                        onError = { error ->
                            resCompletable.complete("❌ রিপোর্ট জমা দেওয়া ব্যর্থ হয়েছে। কারণ: $error")
                        }
                    )
                    
                    responseText = resCompletable.await()
                    _aiFlowState.value = AiFlow.NONE
                    _aiFlowData.clear()
                }
            }
        }

        val aiMsgId = "msg_" + System.currentTimeMillis() + "_" + (1000..9999).random()
        val aiMsg = ChatMessage(
            id = aiMsgId,
            senderName = chatUser.name,
            senderEmail = chatUser.email,
            text = responseText,
            timestampMs = System.currentTimeMillis(),
            isOwn = false,
            isLocal = false
        )

        val currentLocal = LocalStorage.getLocalMessages(context, chatKey)
        LocalStorage.saveLocalMessages(context, chatKey, currentLocal + aiMsg)

        loadMessagesForConversation(chatUser.email)
    }

    val forceAdmin = MutableStateFlow(false)

    fun toggleForceAdmin() {
        val user = _currentUser.value
        if (isRafidUser(user)) {
            forceAdmin.value = !forceAdmin.value
        }
    }

    fun reloadSecuredAndHiddenChats() {
        _securedChats.value = LocalStorage.getSecuredChats(context)
        _hiddenChats.value = LocalStorage.getHiddenChats(context)
        _mutedChats.value = LocalStorage.getMutedChats(context)
    }

    fun muteChat(email: String, durationMs: Long?) {
        val expiry = if (durationMs == null) {
            null // unmute
        } else if (durationMs == Long.MAX_VALUE) {
            Long.MAX_VALUE // infinite
        } else {
            System.currentTimeMillis() + durationMs
        }
        LocalStorage.saveMutedChat(context, email, expiry)
        _mutedChats.value = LocalStorage.getMutedChats(context)
    }

    fun isChatMuted(email: String): Boolean {
        val expiry = mutedChats.value[email] ?: return false
        if (expiry == Long.MAX_VALUE) return true
        if (System.currentTimeMillis() > expiry) {
            // Expired, auto clean up
            viewModelScope.launch(Dispatchers.Main) {
                LocalStorage.saveMutedChat(context, email, null)
                _mutedChats.value = LocalStorage.getMutedChats(context)
            }
            return false
        }
        return true
    }

    // New variables and methods for account blocking and privacy
    private val _isAccountBlocked = MutableStateFlow(false)
    val isAccountBlocked: StateFlow<Boolean> = _isAccountBlocked.asStateFlow()

    private val _blockedUsersMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val blockedUsersMap: StateFlow<Map<String, Boolean>> = _blockedUsersMap.asStateFlow()

    private val _adminPrivacyEnabled = MutableStateFlow(false)
    val adminPrivacyEnabled: StateFlow<Boolean> = _adminPrivacyEnabled.asStateFlow()

    private val _adminPrivacyMode = MutableStateFlow("No") // "Yes", "No", "Customize"
    val adminPrivacyMode: StateFlow<String> = _adminPrivacyMode.asStateFlow()

    private val _adminAllowedUsers = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val adminAllowedUsers: StateFlow<Map<String, Boolean>> = _adminAllowedUsers.asStateFlow()

    private val _userBlockedUsers = MutableStateFlow<Set<String>>(LocalStorage.getBlockedUsersByUser(context))
    val userBlockedUsers: StateFlow<Set<String>> = _userBlockedUsers.asStateFlow()

    private val _blockedByUsers = MutableStateFlow<Set<String>>(emptySet())
    val blockedByUsers: StateFlow<Set<String>> = _blockedByUsers.asStateFlow()

    fun syncUserBlocks() {
        val current = _currentUser.value ?: return
        val currentEmailSanitized = sanitizeId(current.email)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rawBlocks = FirebaseRestClient.service.getValue("user_blocks/$currentEmailSanitized") as? Map<*, *>
                val blockedEmails = mutableSetOf<String>()
                if (rawBlocks != null) {
                    for ((_, v) in rawBlocks) {
                        val otherEmail = v as? String ?: ""
                        if (otherEmail.isNotEmpty()) {
                            blockedEmails.add(otherEmail.lowercase().trim())
                        }
                    }
                }
                _userBlockedUsers.value = blockedEmails
                LocalStorage.saveBlockedUsersByUser(context, current.email, blockedEmails)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rawBlockedBy = FirebaseRestClient.service.getValue("blocked_by_users/$currentEmailSanitized") as? Map<*, *>
                val blockedByEmails = mutableSetOf<String>()
                if (rawBlockedBy != null) {
                    for ((_, v) in rawBlockedBy) {
                        val otherEmail = v as? String ?: ""
                        if (otherEmail.isNotEmpty()) {
                            blockedByEmails.add(otherEmail.lowercase().trim())
                        }
                    }
                }
                _blockedByUsers.value = blockedByEmails
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun blockUserByUser(otherEmail: String) {
        val current = _currentUser.value ?: return
        val myEmailKey = sanitizeId(current.email)
        val otherEmailKey = sanitizeId(otherEmail)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.setValue("user_blocks/$myEmailKey/$otherEmailKey", otherEmail.lowercase().trim())
                FirebaseRestClient.service.setValue("blocked_by_users/$otherEmailKey/$myEmailKey", current.email.lowercase().trim())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val currentSet = _userBlockedUsers.value.toMutableSet()
            currentSet.add(otherEmail.lowercase().trim())
            _userBlockedUsers.value = currentSet
            LocalStorage.saveBlockedUsersByUser(context, current.email, currentSet)
        }
    }

    fun unblockUserByUser(otherEmail: String) {
        val current = _currentUser.value ?: return
        val myEmailKey = sanitizeId(current.email)
        val otherEmailKey = sanitizeId(otherEmail)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.deleteValue("user_blocks/$myEmailKey/$otherEmailKey")
                FirebaseRestClient.service.deleteValue("blocked_by_users/$otherEmailKey/$myEmailKey")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val currentSet = _userBlockedUsers.value.toMutableSet()
            currentSet.remove(otherEmail.lowercase().trim())
            _userBlockedUsers.value = currentSet
            LocalStorage.saveBlockedUsersByUser(context, current.email, currentSet)
        }
    }

    fun setVerificationStatus(email: String, color: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val emailKey = email.lowercase().trim()
            if (color == null) {
                LocalStorage.removeVerifiedUser(context, emailKey)
                try {
                    FirebaseRestClient.service.deleteValue("verified_users/${sanitizeId(emailKey)}")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val updatedMap = _premiumVerifiedColors.value.toMutableMap()
                updatedMap.remove(emailKey)
                _premiumVerifiedColors.value = updatedMap
            } else {
                LocalStorage.saveVerifiedUser(context, emailKey, color)
                try {
                    FirebaseRestClient.service.setValue("verified_users/${sanitizeId(emailKey)}", color)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val updatedMap = _premiumVerifiedColors.value.toMutableMap()
                updatedMap[emailKey] = color
                _premiumVerifiedColors.value = updatedMap
            }
        }
    }

    fun verifyAllAccounts(users: List<User>, color: String = "gold") {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedMap = _premiumVerifiedColors.value.toMutableMap()
            users.forEach { u ->
                val emailKey = u.email.lowercase().trim()
                LocalStorage.saveVerifiedUser(context, emailKey, color)
                try {
                    FirebaseRestClient.service.setValue("verified_users/${sanitizeId(emailKey)}", color)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                updatedMap[emailKey] = color
            }
            _premiumVerifiedColors.value = updatedMap
        }
    }

    fun unverifyAllAccounts(users: List<User>) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedMap = _premiumVerifiedColors.value.toMutableMap()
            users.forEach { u ->
                val emailKey = u.email.lowercase().trim()
                if (!isRafidUser(u)) {
                    LocalStorage.removeVerifiedUser(context, emailKey)
                    try {
                        FirebaseRestClient.service.deleteValue("verified_users/${sanitizeId(emailKey)}")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    updatedMap.remove(emailKey)
                }
            }
            _premiumVerifiedColors.value = updatedMap
        }
    }

    fun checkBlockedStatus() {
        val current = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val userKey = sanitizeId(current.email)
            try {
                val isBlocked = FirebaseRestClient.service.getValue("blocked_users/$userKey") == true
                val blockReason = if (isBlocked) {
                    try {
                        FirebaseRestClient.service.getValue("blocked_reasons/$userKey")?.toString() ?: ""
                    } catch (e: Exception) {
                        ""
                    }
                } else {
                    ""
                }
                withContext(Dispatchers.Main) {
                    _isAccountBlocked.value = isBlocked
                    if (isBlocked) {
                        logout()
                        _authError.value = if (blockReason.isNotEmpty()) blockReason else "আপনার অ্যাকাউন্টটি ব্লক করা হয়েছে! খারাপ ভাষা ব্যবহারের জন্য আপনার অ্যাকাউন্ট বন্ধ করা হয়েছে।"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadBlockedUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = FirebaseRestClient.service.getValue("blocked_users") as? Map<*, *>
                val map = mutableMapOf<String, Boolean>()
                res?.forEach { (k, v) ->
                    val kStr = k?.toString() ?: return@forEach
                    val vBool = v as? Boolean ?: false
                    map[kStr] = vBool
                }
                withContext(Dispatchers.Main) {
                    _blockedUsersMap.value = map
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun unblockUser(email: String) {
        val userKey = sanitizeId(email)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.setValue("blocked_users/$userKey", false)
                FirebaseRestClient.service.setValue("offensive_count/$userKey", 0)
                loadBlockedUsers()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun blockUser(email: String, blockReason: String = "") {
        val userKey = sanitizeId(email)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.setValue("blocked_users/$userKey", true)
                val finalReason = if (blockReason.trim().isNotEmpty()) {
                    blockReason.trim()
                } else {
                    "আপনার অ্যাকাউন্টটি ব্লক করা হয়েছে!"
                }
                FirebaseRestClient.service.setValue("blocked_reasons/$userKey", finalReason)
                loadBlockedUsers()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val _customBadWords = MutableStateFlow<List<String>>(emptyList())
    val customBadWords: StateFlow<List<String>> = _customBadWords.asStateFlow()

    fun loadCustomBadWords() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val raw = FirebaseRestClient.service.getValue("admin_bad_words") as? Map<*, *>
                val list = mutableListOf<String>()
                raw?.forEach { (_, v) ->
                    val word = v?.toString()?.trim()?.lowercase()
                    if (!word.isNullOrEmpty()) {
                        list.add(word)
                    }
                }
                withContext(Dispatchers.Main) {
                    _customBadWords.value = list.distinct()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addCustomBadWord(word: String) {
        val trimmed = word.trim().lowercase()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentList = _customBadWords.value.toMutableList()
                if (!currentList.contains(trimmed)) {
                    currentList.add(trimmed)
                    val key = trimmed.replace(".", "_")
                        .replace("#", "_")
                        .replace("$", "_")
                        .replace("[", "_")
                        .replace("]", "_")
                    FirebaseRestClient.service.setValue("admin_bad_words/$key", trimmed)
                    loadCustomBadWords()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeCustomBadWord(word: String) {
        val trimmed = word.trim().lowercase()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val key = trimmed.replace(".", "_")
                    .replace("#", "_")
                    .replace("$", "_")
                    .replace("[", "_")
                    .replace("]", "_")
                FirebaseRestClient.service.deleteValue("admin_bad_words/$key")
                loadCustomBadWords()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadAdminPrivacy() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val enabledRaw = FirebaseRestClient.service.getValue("admin_privacy/enabled")
                val enabled = (enabledRaw as? Boolean) ?: false

                val modeRaw = FirebaseRestClient.service.getValue("admin_privacy/mode")
                val mode = (modeRaw as? String) ?: "No"
                
                val allowedRaw = FirebaseRestClient.service.getValue("admin_privacy/allowed_users") as? Map<*, *>
                val allowedMap = mutableMapOf<String, Boolean>()
                allowedRaw?.forEach { (k, v) ->
                    val kStr = k?.toString() ?: return@forEach
                    val vBool = v as? Boolean ?: false
                    allowedMap[kStr] = vBool
                }
                withContext(Dispatchers.Main) {
                    _adminPrivacyEnabled.value = enabled
                    _adminPrivacyMode.value = mode
                    _adminAllowedUsers.value = allowedMap
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setAdminPrivacyEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.setValue("admin_privacy/enabled", enabled)
                withContext(Dispatchers.Main) {
                    _adminPrivacyEnabled.value = enabled
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setAdminPrivacyMode(mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.setValue("admin_privacy/mode", mode)
                withContext(Dispatchers.Main) {
                    _adminPrivacyMode.value = mode
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleAdminAllowedUser(email: String) {
        val userKey = sanitizeId(email)
        val currentVal = _adminAllowedUsers.value[userKey] == true
        val newVal = !currentVal
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.setValue("admin_privacy/allowed_users/$userKey", newVal)
                withContext(Dispatchers.Main) {
                    val updated = _adminAllowedUsers.value.toMutableMap()
                    if (newVal) updated[userKey] = true else updated.remove(userKey)
                    _adminAllowedUsers.value = updated
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun handleUserOffensiveStrike(email: String): Int {
        val userKey = sanitizeId(email)
        val currentCount = try {
            val raw = FirebaseRestClient.service.getValue("offensive_count/$userKey")
            (raw as? Number)?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
        val newCount = currentCount + 1
        try {
            FirebaseRestClient.service.setValue("offensive_count/$userKey", newCount)
            if (newCount >= 3) {
                FirebaseRestClient.service.setValue("blocked_users/$userKey", true)
                FirebaseRestClient.service.setValue("blocked_reasons/$userKey", "অতিরিক্ত খারাপ ভাষা বা কাস্টম নিষিদ্ধ শব্দ ব্যবহারের জন্য আপনার অ্যাকাউন্টটি স্বয়ংক্রিয়ভাবে ব্লক করা হয়েছে।")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return newCount
    }

    fun isRafidUser(user: User?): Boolean {
        if (user == null) return false
        val email = user.email.lowercase().trim()
        val username = email.substringBefore("@")
        return email == "rafid" || username == "rafid"
    }

    fun isUserAdmin(user: User?): Boolean {
        if (user == null) return false
        if (isRafidUser(user)) return true
        val emailKey = user.email.lowercase().trim()
        return promotedAdmins.value.containsKey(emailKey)
    }

    fun hasAdminPermission(user: User?, permission: String): Boolean {
        if (user == null) return false
        if (isRafidUser(user)) return true // rafid has ALL permissions!
        val emailKey = user.email.lowercase().trim()
        val permissions = promotedAdmins.value[emailKey] ?: return false
        return permissions.contains(permission)
    }

    fun promoteUserToAdmin(email: String, permissions: List<String>) {
        val cleanEmail = email.lowercase().trim()
        val username = cleanEmail.substringBefore("@")
        if (cleanEmail == "rafid" || username == "rafid") {
            return
        }
        LocalStorage.savePromotedAdmin(context, email.lowercase().trim(), permissions)
        _promotedAdmins.value = LocalStorage.getPromotedAdmins(context)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.setValue("promoted_admins/${sanitizeId(email)}", permissions)
                loadAllConversationsAndUsers()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun demoteAdmin(email: String) {
        val cleanEmail = email.lowercase().trim()
        val username = cleanEmail.substringBefore("@")
        if (cleanEmail == "rafid" || username == "rafid") {
            return
        }
        LocalStorage.savePromotedAdmin(context, email.lowercase().trim(), null)
        _promotedAdmins.value = LocalStorage.getPromotedAdmins(context)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseRestClient.service.deleteValue("promoted_admins/${sanitizeId(email)}")
                loadAllConversationsAndUsers()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sanitizeId(email: String): String {
        return email.lowercase().replace(Regex("[.#$\\[\\]]"), "_")
    }
}

sealed class CallState {
    object Idle : CallState()
    data class Outgoing(val roomId: String, val partnerName: String, val callType: String) : CallState()
    data class Incoming(val roomId: String, val partnerName: String, val callType: String) : CallState()
    data class Connected(val roomId: String, val partnerName: String, val callType: String) : CallState()
}
