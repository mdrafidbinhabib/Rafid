package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

@android.annotation.SuppressLint("MissingPermission", "NotificationPermission")
class EchoNotificationService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private val scriptUrl = "https://script.google.com/macros/s/AKfycbzvqNxH0BGFuXbIvJPMDR6uqUkWvekQvS8asurlYnRoT23lMCZq9NLmLoO4ohje_3Otbg/exec"

    private val notifiedCalls = mutableSetOf<String>()
    private val notifiedMessages = mutableSetOf<String>()
    private var isFirstMessagePoll = true
    private var serviceWakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_SERVICE_ID = "echo_service_channel"
        const val CHANNEL_CHATS_ID = "echo_chats_channel"
        const val CHANNEL_CALLS_ID = "echo_calls_channel"
        const val NOTIFICATION_SERVICE_ID = 9999
    }

    override fun onCreate() {
        super.onCreate()
        
        // Load notified messages from storage
        try {
            notifiedMessages.addAll(LocalStorage.getNotifiedMessageIds(applicationContext))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Acquire CPU wake lock to ensure polling works even when screen is off / locked
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            serviceWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EchoChat:ServiceWakeLock").apply {
                acquire()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        createNotificationChannels()
        startServiceInForeground()
        
        // Start background polling
        startPollingLoop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startServiceInForeground()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // 1. Direct Foreground Service Restart Intent (Highly reliable on API 26+)
        try {
            val serviceIntent = Intent(applicationContext, EchoNotificationService::class.java)
            val pendingServiceIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(
                    applicationContext,
                    2,
                    serviceIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getService(
                    applicationContext,
                    2,
                    serviceIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1000,
                pendingServiceIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Broadcast Receiver Fallback
        try {
            val restartIntent = Intent(applicationContext, EchoServiceRestartReceiver::class.java)
            val pendingBroadcastIntent = PendingIntent.getBroadcast(
                applicationContext,
                1,
                restartIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1500,
                pendingBroadcastIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Foreground Service Channel
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "EchoChat Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps EchoChat active to receive real-time notifications"
            }
            notificationManager.createNotificationChannel(serviceChannel)

            // 2. Chats Channel
            val chatsChannel = NotificationChannel(
                CHANNEL_CHATS_ID,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time incoming chat message notifications"
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(chatsChannel)

            // 3. Calls Channel
            val callsChannel = NotificationChannel(
                CHANNEL_CALLS_ID,
                "Voice & Video Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming voice and video call notifications"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build()
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(callsChannel)
        }
    }

    private fun startServiceInForeground() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_SERVICE_ID)
            .setContentTitle("EchoChat Active")
            .setContentText("Listening for real-time messages & calls...")
            .setSmallIcon(com.example.R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_SERVICE_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_SERVICE_ID, notification)
        }
    }

    private val userRegistry = mutableMapOf<String, String>() // email -> name
    private var lastUserRegistryFetch = 0L

    private suspend fun refreshUserRegistry() {
        if (System.currentTimeMillis() - lastUserRegistryFetch > 300000) { // every 5 minutes
            try {
                val userRes = RetrofitClient.echoChatApi.getUsers(scriptUrl)
                if (userRes != null && userRes.status == "success" && userRes.users != null) {
                    userRes.users.forEach { u ->
                        userRegistry[u.email.lowercase().trim()] = u.name
                    }
                    lastUserRegistryFetch = System.currentTimeMillis()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isMuted(senderEmail: String, mutedChats: Map<String, Long>): Boolean {
        val cleanEmail = senderEmail.lowercase().trim()
        val muteExpiry = mutedChats[cleanEmail] ?: 0L
        return if (muteExpiry == Long.MAX_VALUE) {
            true
        } else {
            System.currentTimeMillis() < muteExpiry
        }
    }

    private fun getUserNameFromRegistry(email: String): String {
        val cleanEmail = email.lowercase().trim()
        return userRegistry[cleanEmail] ?: cleanEmail.split("@")[0]
    }

    private fun getGroupName(chatKey: String, remoteGroups: Map<*, *>?): String {
        if (remoteGroups == null) return "গ্রুপ চ্যাট"
        val gMap = remoteGroups[chatKey] as? Map<*, *> ?: remoteGroups[chatKey.removeSuffix(".json")] as? Map<*, *>
        return gMap?.get("name") as? String ?: "গ্রুপ চ্যাট"
    }

    private fun startPollingLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val user = LocalStorage.getLoggedInUser(applicationContext)
                    if (user != null) {
                        pollIncomingCalls(user)
                        pollNewMessages(user)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(1500) // Poll every 1.5 seconds for instant alerts
            }
        }
    }

    private suspend fun pollIncomingCalls(user: User) {
        val userKey = sanitizeId(user.email)
        val callsVal = try {
            FirebaseRestClient.service.getValue("calls") as? Map<*, *>
        } catch (e: Exception) {
            null
        }

        if (callsVal != null) {
            val hiddenChats = try {
                LocalStorage.getHiddenChats(applicationContext)
            } catch (e: Exception) {
                emptyMap<String, String>()
            }
            for ((rId, callMap) in callsVal) {
                val roomId = rId as? String ?: continue
                val map = callMap as? Map<*, *> ?: continue
                val calleeId = map["calleeId"] as? String ?: ""
                val callerId = map["callerId"] as? String ?: ""
                val callerName = map["callerName"] as? String ?: "Someone"
                val status = map["status"] as? String ?: ""
                val ts = (map["ts"] as? Number)?.toLong() ?: 0L
                val callType = map["callType"] as? String ?: "audio"

                // Check calls active within 5 minutes of clock skew to be extremely robust
                if (sanitizeId(calleeId) == userKey && status == "calling" && (Math.abs(System.currentTimeMillis() - ts) < 300000)) {
                    if (!notifiedCalls.contains(roomId)) {
                        notifiedCalls.add(roomId)
                        val isCallerHidden = hiddenChats.keys.any { it.equals(callerId, ignoreCase = true) }
                        showCallNotification(roomId, callerName, callType, isHidden = isCallerHidden)
                    }
                }
            }
        }
    }

    private suspend fun pollNewMessages(user: User) {
        val myEmail = user.email.lowercase().trim()
        val mySanitized = sanitizeId(myEmail)
        
        // Refresh our user registry of email to names
        refreshUserRegistry()

        // Fetch overall conversations metadata in a single, fast request
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

        val remoteGroups = try {
            SupabaseRestClient.service.getValue("groups") as? Map<*, *>
        } catch (e: Exception) {
            null
        }

        if (remoteLastActivity == null || remoteLastSender == null) {
            return
        }

        val isFirstPoll = isFirstMessagePoll
        if (isFirstMessagePoll) {
            isFirstMessagePoll = false
        }

        // Parse user's groups to identify which group conversations they belong to
        val userGroupKeys = mutableSetOf<String>()
        remoteGroups?.forEach { (k, v) ->
            val gId = k?.toString() ?: return@forEach
            val gMap = v as? Map<*, *> ?: return@forEach
            val membersRaw = gMap["members"] as? String ?: ""
            val createdBy = gMap["createdBy"] as? String ?: ""
            val membersList = membersRaw.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            if (membersList.contains(myEmail) || createdBy.lowercase() == myEmail) {
                userGroupKeys.add(sanitizeId(gId))
            }
        }

        val hiddenChats = try {
            LocalStorage.getHiddenChats(applicationContext)
        } catch (e: Exception) {
            emptyMap<String, String>()
        }

        val mutedChats = try {
            LocalStorage.getMutedChats(applicationContext)
        } catch (e: Exception) {
            emptyMap<String, Long>()
        }

        val blockedUsers = try {
            LocalStorage.getBlockedUsersByUser(applicationContext)
        } catch (e: Exception) {
            emptySet<String>()
        }

        // Loop through all active conversations from the server
        remoteLastActivity.forEach { (key, value) ->
            val chatKey = key?.toString() ?: return@forEach
            val lastActivity = (value as? Number)?.toLong() ?: 0L
            val lastSender = remoteLastSender[chatKey]?.toString() ?: ""

            if (lastActivity <= 0 || lastSender.isEmpty() || lastSender.lowercase().trim() == myEmail) {
                return@forEach
            }

            val lastSenderClean = lastSender.lowercase().trim()
            if (blockedUsers.contains(lastSenderClean)) {
                return@forEach
            }

            // Determine if the user is a participant of this conversation
            var isParticipant = false
            var isGroup = false
            if (chatKey.contains("__")) {
                val parts = chatKey.split("__")
                if (parts.size == 2 && (parts[0] == mySanitized || parts[1] == mySanitized)) {
                    isParticipant = true
                }
            } else if (userGroupKeys.contains(chatKey)) {
                isParticipant = true
                isGroup = true
            }

            if (!isParticipant) {
                return@forEach
            }

            // Get seen read-receipt for this chat for the user
            val mySeenObj = (remoteSeen?.get(chatKey) as? Map<*, *>)?.get(mySanitized) as? Map<*, *>
            val mySeenTs = (mySeenObj?.get("ts") as? Number)?.toLong() ?: 0L
            val persistedSeenTs = LocalStorage.getPersistedSeenTimestamp(applicationContext, chatKey)
            val effectiveSeenTs = maxOf(mySeenTs, persistedSeenTs)

            // If the last activity is greater than what we've seen, it's a new unread message
            if (lastActivity > effectiveSeenTs) {
                val uniqueNotificationKey = "${chatKey}_${lastActivity}"
                
                if (!notifiedMessages.contains(uniqueNotificationKey)) {
                    // Mark as notified so we don't trigger again
                    notifiedMessages.add(uniqueNotificationKey)
                    LocalStorage.addNotifiedMessageId(applicationContext, uniqueNotificationKey)

                    val ageMs = Math.abs(System.currentTimeMillis() - lastActivity)
                    
                    // Only display notification if this is NOT the very first poll,
                    // AND the message is relatively new (sent within the last 10 minutes)
                    if (!isFirstPoll && ageMs < 600000) {
                        // Check if muted
                        val isChatMuted = isMuted(lastSender, mutedChats)
                        if (isChatMuted) {
                            return@forEach
                        }

                        // Fetch the actual latest message content from Supabase
                        val messagesResult = try {
                            SupabaseRestClient.service.getValue("messages/$chatKey") as? Map<*, *>
                        } catch (e: Exception) {
                            null
                        }

                        var senderName = "নতুন বার্তা"
                        var messageText = "নিউ এসএমএস"

                        if (messagesResult != null) {
                            var newestMsg: Map<*, *>? = null
                            var newestTs = 0L
                            messagesResult.forEach { (_, v) ->
                                val mData = v as? Map<*, *> ?: return@forEach
                                val ts = (mData["timestamp"] as? Number)?.toLong() ?: 0L
                                if (ts > newestTs) {
                                    newestTs = ts
                                    newestMsg = mData
                                }
                            }

                            if (newestMsg != null) {
                                messageText = newestMsg!!["text"] as? String ?: "নিউ এসএমএস"
                                val senderEmail = newestMsg!!["sender"] as? String ?: ""
                                
                                senderName = if (isGroup) {
                                    val gName = getGroupName(chatKey, remoteGroups)
                                    val sName = getUserNameFromRegistry(senderEmail)
                                    "$gName: $sName"
                                } else {
                                    getUserNameFromRegistry(senderEmail)
                                }
                            }
                        }

                        val isSenderHidden = hiddenChats.keys.any { sanitizeId(it).equals(sanitizeId(lastSender), ignoreCase = true) }
                        showChatNotification(senderName, messageText, isHidden = isSenderHidden)
                    }
                }
            }
        }
    }

    private fun showCallNotification(roomId: String, callerName: String, callType: String, isHidden: Boolean) {
        // Wake lock to turn on the screen for incoming calls
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "EchoChat:CallWakeLock"
        )
        wakeLock.acquire(10000)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val typeText = if (callType == "video") "ভিডিও কল" else "ভয়েস কল"
        val titleText = "📞 ইনকামিং কল"
        val contentText = if (isHidden) "ইনকামিং $typeText" else "$callerName আপনাকে $typeText দিচ্ছেন"

        val notification = NotificationCompat.Builder(this, CHANNEL_CALLS_ID)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setSmallIcon(com.example.R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pendingIntent, true) // Show as full screen alert
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(roomId.hashCode(), notification)
    }

    private fun showChatNotification(senderName: String, text: String, isHidden: Boolean) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            2,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (isHidden) "নতুন বার্তা" else senderName
        val content = if (isHidden) "নিউ এসএমএস" else text

        val notification = NotificationCompat.Builder(this, CHANNEL_CHATS_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(com.example.R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val uniqueId = (System.currentTimeMillis() % 100000000).toInt() + java.util.Random().nextInt(1000)
        notificationManager.notify(uniqueId, notification)
    }

    private fun sanitizeId(email: String): String {
        return email.replace(".", "_").replace("@", "_")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (serviceWakeLock?.isHeld == true) {
                serviceWakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serviceJob.cancel() // Cancel all polling tasks

        // Fire broadcast to restart the service so it keeps polling
        val restartIntent = Intent(applicationContext, EchoServiceRestartReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            1,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 2000,
                pendingIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
