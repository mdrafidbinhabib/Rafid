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
        // Keep running until explicitly stopped
        return START_STICKY
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
                delay(3000) // Poll every 3 seconds for fast alerts
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
        val myEmail = user.email.lowercase()
        val rawMessages = try {
            RetrofitClient.echoChatApi.getMessages(scriptUrl)
        } catch (e: Exception) {
            null
        }

        if (rawMessages != null) {
            val isFirstInstall = LocalStorage.getNotifiedMessageIds(applicationContext).isEmpty()

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

            for (msg in rawMessages) {
                val msgId = msg.id ?: msg.timestamp ?: continue
                val senderEmail = msg.sender?.lowercase() ?: ""
                val participants = msg.getParticipantsList().map { it.lowercase() }

                if (participants.contains(myEmail) && senderEmail != myEmail && !blockedUsers.contains(senderEmail)) {
                    if (!notifiedMessages.contains(msgId)) {
                        notifiedMessages.add(msgId)
                        LocalStorage.addNotifiedMessageId(applicationContext, msgId)
                        
                        // If it's the very first poll ever (after installation / clean cache),
                        // we just record the messages to avoid a notification flood.
                        // Otherwise, we notify!
                        if (!isFirstInstall) {
                            // Check if muted
                            val muteExpiry = mutedChats[senderEmail] ?: mutedChats[msg.sender?.lowercase()] ?: 0L
                            val isMuted = if (muteExpiry == Long.MAX_VALUE) {
                                true
                            } else {
                                System.currentTimeMillis() < muteExpiry
                            }
                            
                            if (isMuted) {
                                continue // Suppress notifications completely if muted
                            }

                            val isSenderHidden = hiddenChats.keys.any { it.equals(senderEmail, ignoreCase = true) }
                            if (isSenderHidden) {
                                showChatNotification("নিউ এসএমএস", "নিউ এসএমএস", isHidden = true)
                            } else {
                                val senderName = msg.user ?: senderEmail.split("@")[0]
                                showChatNotification(senderName, msg.message ?: "", isHidden = false)
                            }
                        }
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
        notificationManager.notify(content.hashCode() + title.hashCode(), notification)
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
    }
}
