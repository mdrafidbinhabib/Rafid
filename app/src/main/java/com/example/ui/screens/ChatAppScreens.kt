package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import android.util.Base64
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

// Root App Screen Router
@Composable
fun EchoChatApp(viewModel: EchoChatViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentTheme by viewModel.chatTheme.collectAsState()

    val bgBrush = getThemeGradient(currentTheme)

    MyCustomTheme(isDarkMode = isDarkMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
        ) {
            if (currentUser == null) {
                AuthScreen(viewModel = viewModel)
            } else {
                DashboardScreen(viewModel = viewModel)
            }

            // Global Overlays (e.g. Calls)
            CallManagerOverlay(viewModel = viewModel)
        }
    }
}

@Composable
fun MyCustomTheme(
    isDarkMode: Boolean,
    content: @Composable () -> Unit
) {
    // Override M3 ColorScheme based on dark mode preferences
    val colorScheme = if (isDarkMode) {
        darkColorScheme(
            primary = Color(0xFF6366F1),
            secondary = Color(0xFF9065f7),
            tertiary = Color(0xFFEC4899),
            background = Color(0xFF0F0C29),
            surface = Color(0x26FFFFFF)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF4F46E5),
            secondary = Color(0xFF8B5CF6),
            tertiary = Color(0xFFEC4899),
            background = Color(0xFFF1F5F9),
            surface = Color(0xD9FFFFFF)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// ─────────────────────────────────────────────
//  AUTHS SCREEN (Login, Register & Forgot Pass)
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: EchoChatViewModel) {
    var screenMode by remember { mutableStateOf("login") } // "login", "register", "forgot"
    
    val authLoading by viewModel.authLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val timerSecs by viewModel.forgotResetTimerSecs.collectAsState()

    val context = LocalContext.current

    // Fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var base64Photo by remember { mutableStateOf("") }
    var photoName by remember { mutableStateOf("No file chosen") }

    // Forgot password fields
    var forgotStep by remember { mutableStateOf(1) } // 1, 2, 3
    var forgotEmail by remember { mutableStateOf("") }
    var forgotCode by remember { mutableStateOf("") }
    var forgotNewPass by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
                val logoGlowSize by infiniteTransition.animateFloat(
                    initialValue = 48f,
                    targetValue = 72f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "logoGlow"
                )

                // Header Logo with pulsating glow in the background
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    // Pulsating glow ring
                    Box(
                        modifier = Modifier
                            .size(logoGlowSize.dp)
                            .clip(CircleShape)
                            .background(getAnimatedAccentColor().copy(alpha = 0.25f))
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(getAnimatedAccentColor(), Color(0xFF00C6FF)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChatBubble,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Text(
                    text = "Echo Chat",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Eco.AI Support Message
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Android,
                                contentDescription = "Eco.AI Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Eco.AI Assistant",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Please log in to your account. This user can be sent to Apple.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Initial Login Warning Notice Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Note: No chat will be deleted until this ID is logged in to another phone.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                if (authError != null) {
                    Text(
                        text = authError ?: "",
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                when (screenMode) {
                    "login" -> {
                        Text(
                            text = "Secure Login",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("User ID/Email") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        var showPassword by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Access Key") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                viewModel.login(email, password) {
                                    Toast.makeText(context, "লগইন সফল হয়েছে!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = !authLoading
                        ) {
                            if (authLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text("লগইন করো", fontWeight = FontWeight.Bold)
                            }
                        }


                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🔐 পাসওয়ার্ড রিসেট",
                                color = Color(0xFFF59E0B),
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clickable {
                                        screenMode = "forgot"
                                        forgotStep = 1
                                    }
                                    .padding(4.dp)
                            )
                            Text(
                                text = "হিসাব খুলুন (Register)",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clickable { screenMode = "register" }
                                    .padding(4.dp)
                            )
                        }
                    }

                    "register" -> {
                        Text(
                            text = "Create Account",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("User ID/Email") },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Create Access Key") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom Photo choosing preview
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .padding(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Custom visual photo select mockup or link field
                                    base64Photo = "data:image/png;base64,mock"
                                    photoName = "Selected Photo url"
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("ছবি নির্বাচন", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = photoName,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                viewModel.register(name, email, password, base64Photo) {
                                    Toast.makeText(context, "রেজিস্ট্রেশন সফল! লগইন করুন।", Toast.LENGTH_LONG).show()
                                    screenMode = "login"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = !authLoading
                        ) {
                            if (authLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text("অ্যাকাউন্ট তৈরি করুন", fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "লগইন করুন (Back to Login)",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .clickable { screenMode = "login" }
                        )
                    }

                    "forgot" -> {
                        Text(
                            text = "🔐 Reset Password",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        when (forgotStep) {
                            1 -> {
                                OutlinedTextField(
                                    value = forgotEmail,
                                    onValueChange = { forgotEmail = it },
                                    label = { Text("Your User ID/Email") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        viewModel.requestForgotPasswordCode(
                                            email = forgotEmail,
                                            onSuccess = { forgotStep = 2 },
                                            onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("পার্টনারের ফোনে কোড পাঠান", fontWeight = FontWeight.Bold)
                                }
                            }
                            2 -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0x3310B981))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "📱 আপনার paired অংশীদারের Echo Chat অ্যাপে একটি ৫-ডিজিটের কোড পাঠানো হয়েছে।",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "⏰ কোডের মেয়াদঃ ${timerSecs}s",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Red,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = forgotCode,
                                    onValueChange = { forgotCode = it },
                                    label = { Text("৫-ডিজিট কোড লিখুন") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        viewModel.verifyForgotPasswordCode(
                                            email = forgotEmail,
                                            code = forgotCode,
                                            onSuccess = { forgotStep = 3 },
                                            onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("পার্টনার কোড ভেরিফাই করুণ", fontWeight = FontWeight.Bold)
                                }
                            }
                            3 -> {
                                OutlinedTextField(
                                    value = forgotNewPass,
                                    onValueChange = { forgotNewPass = it },
                                    label = { Text("নতুন পাসওয়ার্ড (New Secret Key)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        viewModel.resetPassword(
                                            email = forgotEmail,
                                            newPass = forgotNewPass,
                                            onSuccess = {
                                                Toast.makeText(context, "পাসওয়ার্ড সফলভাবে পরিবর্তিত হয়েছে!", Toast.LENGTH_LONG).show()
                                                screenMode = "login"
                                            },
                                            onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("নতুন পাসওয়ার্ড সেট করুণ", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            text = "← Back to Login",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .padding(top = 18.dp)
                                .clickable { screenMode = "login" }
                        )
                    }
                }
            }
        }
    }
}

private fun codeValue(timer: Int): String {
    // Stub function to mock visual numbers
    return ""
}

// ─────────────────────────────────────────────
//  MAIN DASHBOARD SCREEN (Chats and Finder)
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: EchoChatViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentChatUser by viewModel.currentChatUser.collectAsState()
    val allActiveUsers by viewModel.allUsers.collectAsState()
    val recentChats by viewModel.recentChats.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val unreadCounts by viewModel.unreadCounts.collectAsState()
    val securedChats by viewModel.securedChats.collectAsState()
    val premiumVerifiedColors by viewModel.premiumVerifiedColors.collectAsState()

    var showProfileModal by remember { mutableStateOf(false) }
    var showPremiumModal by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var isSearchLoading by remember { mutableStateOf(false) }
    var searchField by remember { mutableStateOf("") }

    val googleUsers = remember {
        listOf(
            User(email = "support@google.com", name = "Google Support", photoUrl = "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png"),
            User(email = "sundar@google.com", name = "Sundar Pichai", photoUrl = "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png"),
            User(email = "larry@google.com", name = "Larry Page", photoUrl = "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png"),
            User(email = "sergey@google.com", name = "Sergey Brin", photoUrl = "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png"),
            User(email = "assistant@google.com", name = "Google Assistant", photoUrl = "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png"),
            User(email = "cloud@google.com", name = "Google Cloud Service", photoUrl = "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png"),
            User(email = "ai@google.com", name = "Google AI Team", photoUrl = "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png"),
            User(email = "android@google.com", name = "Android Developer Support", photoUrl = "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png")
        )
    }

    var lockPromptEmail by remember { mutableStateOf<String?>(null) }
    var lockPromptPassword by remember { mutableStateOf("") }
    var lockPromptError by remember { mutableStateOf(false) }

    var showTemporaryWarning by remember {
        mutableStateOf(currentUser?.email?.let { !LocalStorage.hasSeenTemporaryChatWarning(context, it) } ?: false)
    }

    if (showTemporaryWarning && currentUser != null) {
        AlertDialog(
            onDismissRequest = {
                showTemporaryWarning = false
                LocalStorage.markSeenTemporaryChatWarning(context, currentUser!!.email)
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Temporary Chat Warning",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("⚠️ সাময়িক চ্যাট সতর্কতা", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Text(
                    text = "প্রিয় গ্রাহক, অনুগ্রহ করে মনে রাখুন যে এই অ্যাপ্লিকেশনের সমস্ত বার্তা এবং কথোপকথন শুধুমাত্র সাময়িক চ্যাট (Temporary Chat) এর উদ্দেশ্যে তৈরি করা হয়েছে। নিরাপত্তা ও গোপনীয়তার রক্ষা বাস্তবায়নে আপনার এই কথোপকথনগুলো দীর্ঘস্থায়ী বা ক্লাউডে স্থায়ীভাবে জমা থাকবে না।\n\nআপনি যখনই হিসাবটি প্রস্থান বা বাতিল করবেন, তখন আপনার ডেটা লোকাল হোস্টে সংরক্ষিত থাকবে কিন্তু ক্লাউড থেকে মুছে যেতে পারে। চ্যাটিং শুরু করতে সম্মতি প্রদান করুন।",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTemporaryWarning = false
                        LocalStorage.markSeenTemporaryChatWarning(context, currentUser!!.email)
                    },
                    modifier = Modifier.testTag("accept_warning_button")
                ) {
                    Text("আমি বুঝতে পেরেছি ও রাজি", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Dialog option states matching right click
    var activeLongClickUser by remember { mutableStateOf<User?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }
    var showPinSetDialog by remember { mutableStateOf(false) }
    var showPinUnlockDialog by remember { mutableStateOf(false) }
    var pinUnlockValue by remember { mutableStateOf("") }
    var pinUnlockError by remember { mutableStateOf(false) }

    if (currentChatUser != null) {
        ChatWindowScreen(viewModel = viewModel)
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ChatBubble, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Echo Chat", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    },
                    actions = {
                        // Search Toggle Action Icon
                        IconButton(onClick = {
                            isSearchActive = !isSearchActive
                            if (isSearchActive) {
                                isSearchLoading = true
                                coroutineScope.launch {
                                    delay(1500)
                                    isSearchLoading = false
                                }
                            } else {
                                searchField = ""
                            }
                        }) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Filled.Close else Icons.Filled.Search,
                                contentDescription = "Search"
                            )
                        }

                        // Premium Promo / Code Option
                        IconButton(onClick = { showPremiumModal = true }, modifier = Modifier.testTag("premium_button")) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Purchase Premium",
                                tint = Color(0xFFD4AF37) // Bright golden color
                            )
                        }

                        // Dark Mode Toggle
                        val isDark by viewModel.isDarkMode.collectAsState()
                        IconButton(onClick = { viewModel.setDarkMode(!isDark) }) {
                            Icon(
                                imageVector = if (isDark) Icons.Filled.WbSunny else Icons.Filled.NightsStay,
                                contentDescription = "Theme Toggle"
                            )
                        }

                        // Profile Avatar Click
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                                .clickable { showProfileModal = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentUser?.photoUrl.isNullOrEmpty()) {
                                Text(
                                    text = (currentUser?.name?.take(2)?.uppercase() ?: "ME"),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                AsyncImage(
                                    model = currentUser?.photoUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Logout Button
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(Icons.Default.ExitToApp, null, tint = Color.Red)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (isSearchActive) {
                    OutlinedTextField(
                        value = searchField,
                        onValueChange = { searchField = it },
                        placeholder = { Text("নাম অথবা ইমেইল দিয়ে খুঁজুন...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )

                    if (isSearchLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "গ্লোবাল সার্ভার অনুসন্ধান করা হচ্ছে...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        val filteredUsers = if (searchField.isNotEmpty()) {
                            (allActiveUsers + googleUsers).filter { u ->
                                u.name.lowercase().contains(searchField.lowercase()) ||
                                        u.email.lowercase().contains(searchField.lowercase())
                            }.distinctBy { it.email }
                        } else {
                            googleUsers.shuffled()
                        }

                        if (searchField.isNotEmpty() && filteredUsers.isEmpty()) {
                            // Display error contact card
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            Toast.makeText(context, "এই ইমেইলটি Echo Chat সার্ভারে নিবন্ধিত নয়!", Toast.LENGTH_LONG).show()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ErrorOutline,
                                            contentDescription = "Error Contact",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "ব্যবহারকারী পাওয়া যায়নি! (Error Contact)",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                fontSize = 16.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = searchField,
                                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(filteredUsers) { user ->
                                    val unread = unreadCounts[user.email] ?: 0
                                    UserItemRow(
                                        user = user,
                                        unreadCount = unread,
                                        isSecured = securedChats.containsKey(user.email),
                                        verifiedColor = premiumVerifiedColors[user.email],
                                        onSelect = {
                                            val lockPass = securedChats[user.email]
                                            if (lockPass != null) {
                                                lockPromptEmail = user.email
                                                lockPromptPassword = ""
                                                lockPromptError = false
                                            } else {
                                                viewModel.selectChatUser(user)
                                            }
                                        },
                                        onLongClick = {
                                            activeLongClickUser = user
                                            showActionDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Recent chats list (No active search query)
                    if (recentChats.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.ChatBubble, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("কোনো চলমান বার্তা নেই!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(recentChats) { user ->
                                val unread = unreadCounts[user.email] ?: 0
                                UserItemRow(
                                    user = user,
                                    unreadCount = unread,
                                    isSecured = securedChats.containsKey(user.email),
                                    verifiedColor = premiumVerifiedColors[user.email],
                                    onSelect = {
                                        val lockPass = securedChats[user.email]
                                        if (lockPass != null) {
                                            lockPromptEmail = user.email
                                            lockPromptPassword = ""
                                            lockPromptError = false
                                        } else {
                                            viewModel.selectChatUser(user)
                                        }
                                    },
                                    onLongClick = {
                                        activeLongClickUser = user
                                        showActionDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Lock password prompt
    if (lockPromptEmail != null) {
        AlertDialog(
            onDismissRequest = { lockPromptEmail = null },
            title = { Text("🔒 Locked Chat (সংরক্ষিত চ্যাট)") },
            text = {
                Column {
                    Text("এই চ্যাটটি পাসওয়ার্ড দ্বারা সুরক্ষিত। অনুগ্রহ করে পাসওয়ার্ড লিখুনঃ")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = lockPromptPassword,
                        onValueChange = { lockPromptPassword = it },
                        label = { Text("പাসওয়ার্ড / Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (lockPromptError) {
                        Text("ভুল পাসওয়ার্ড! অনুগ্রহ করে আবার চেষ্টা করুন।", color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val pass = securedChats[lockPromptEmail] ?: ""
                    val realPin = extractPinFromLockString(pass)
                    if (realPin == lockPromptPassword) {
                        val targetUser = allActiveUsers.find { it.email == lockPromptEmail } ?: recentChats.find { it.email == lockPromptEmail }
                        if (targetUser != null) {
                             viewModel.selectChatUser(targetUser)
                        }
                        lockPromptEmail = null
                    } else {
                        lockPromptError = true
                    }
                }) {
                    Text("আনলক করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { lockPromptEmail = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Long press Action Dialog
    if (showActionDialog && activeLongClickUser != null) {
        val target = activeLongClickUser!!
        val isSecured = securedChats.containsKey(target.email)

        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            title = {
                val verifiedColor = target.email.let { premiumVerifiedColors[it] }
                val isPremiumVerified = verifiedColor != null || target.name.contains("(Verified)")
                val finalVerifiedColor = verifiedColor ?: if (target.name.contains("(Verified)")) "blue" else null
                val cleanName = removeLinkAndLockFromName(target.name)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPremiumVerified) {
                        Text(
                            text = cleanName,
                            style = MaterialTheme.typography.titleLarge.copy(color = parseVerifiedColor(finalVerifiedColor)),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Premium Verified",
                            tint = parseVerifiedColor(finalVerifiedColor),
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(cleanName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = { Text("আপনার নির্বাচিত ইউজারের জন্য নিচে দেয়া অপারেশনগুলোর একটি নির্বাচন করুণঃ") },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            showActionDialog = false
                            if (isSecured) {
                                pinUnlockValue = ""
                                pinUnlockError = false
                                showPinUnlockDialog = true
                            } else {
                                showPinSetDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSecured) Color.Gray else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (isSecured) "🔓 চ্যাট আনলক করো" else "🔒 চ্যাট সিকিউর করো")
                    }
                    Button(
                        onClick = {
                            showActionDialog = false
                            viewModel.deleteConversation(target.email)
                            Toast.makeText(context, "চ্যাট সফলভাবে ডিলিট করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("🗑️ চ্যাট ডিলিট করুন")
                    }
                }
            }
        )
    }

    // PIN Unlock Prompt Dialog (When unlocking)
    if (showPinUnlockDialog && activeLongClickUser != null) {
        val target = activeLongClickUser!!
        val pass = securedChats[target.email] ?: ""
        val realPin = extractPinFromLockString(pass)

        AlertDialog(
            onDismissRequest = { showPinUnlockDialog = false },
            title = { Text("🔓 চ্যাট আনলক নিশ্চিত করুন") },
            text = {
                Column {
                    Text("অনুমোদিত অ্যাক্সেস নিশ্চিত করতে অনুগ্রহ করে এই চ্যাটের ৩-৫ ডিজিটের পিনটি দিনঃ", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinUnlockValue,
                        onValueChange = { input ->
                            if (input.length <= 5 && input.all { it.isDigit() }) {
                                pinUnlockValue = input
                            }
                        },
                        label = { Text("পিন নম্বর") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinUnlockError) {
                        Text("ভুল পিন! অনুগ্রহ করে সঠিক পিনটি দিন।", color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (pinUnlockValue == realPin) {
                        viewModel.secureChat(target.email, null)
                        showPinUnlockDialog = false
                        Toast.makeText(context, "চ্যাট সফলভাবে আনলক করা হয়েছে!", Toast.LENGTH_SHORT).show()
                    } else {
                        pinUnlockError = true
                    }
                }) {
                    Text("নিশ্চিত করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinUnlockDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // PIN Setting Dialog
    if (showPinSetDialog && activeLongClickUser != null) {
        val target = activeLongClickUser!!
        var pinValue by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPinSetDialog = false },
            title = { Text("🔒 চ্যাট লক করুন") },
            text = {
                Column {
                    Text("অননুমোদিত অ্যাক্সেস রোধ করতে ৫ ডিজিটের পিন সেট করুনঃ", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { input ->
                            if (input.length <= 5 && input.all { it.isDigit() }) {
                                pinValue = input
                            }
                        },
                        label = { Text("৫-ডিজিট পিন") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError) {
                        Text("অনুগ্রহ করে একটি সঠিক ৩-৫ ডিজিট পিন দিন!", color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (pinValue.length in 3..5) {
                        val currentUserName = currentUser?.name?.let { removeLinkAndLockFromName(it) } ?: "User"
                        val finalString = "[{${pinValue}}(${currentUserName})]"
                        viewModel.secureChat(target.email, finalString)
                        showPinSetDialog = false
                        Toast.makeText(context, "চ্যাট সফলভাবে লক করা হয়েছে!", Toast.LENGTH_SHORT).show()
                    } else {
                        pinError = true
                    }
                }) {
                    Text("সাবমিট")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Profile Settings overlay and tabs representation
    if (showProfileModal) {
        ProfileModalDialog(viewModel = viewModel) {
            showProfileModal = false
        }
    }

    if (showPremiumModal) {
        PremiumActivationDialog(viewModel = viewModel) {
            showPremiumModal = false
        }
    }
}

@Composable
fun PremiumActivationDialog(viewModel: EchoChatViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val premiumVerifiedColors by viewModel.premiumVerifiedColors.collectAsState()
    val isVerified = currentUser?.email?.let { premiumVerifiedColors.containsKey(it) } ?: false

    var codeText by remember { mutableStateOf("") }
    val premiumCodes by viewModel.premiumCodes.collectAsState()
    val premiumLoading by viewModel.premiumLoading.collectAsState()
    var statusMsg by remember { mutableStateOf("") }
    var statusSuccess by remember { mutableStateOf(false) }

    // Fetch premium list on launch
    LaunchedEffect(Unit) {
        viewModel.loadPremiumCodesFromSheet()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Premium",
                    tint = Color(0xFFD4AF37),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("পার্থিব প্রিমিয়াম ভেরিফিকেশন", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isVerified) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "✓ আপনার অ্যাকাউন্ট সফলভাবে ভেরিফাই করা হয়েছে!",
                                color = Color(0xFF1565C0),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "আপনি ইতোমধ্যে ভেরিফাইড ইউজার। এখন নতুন কোনো কোড টাইপ বা প্রবেশ করতে পারবেন না। নতুন কোড বসাতে চাইলে প্রথমে নিচের বাতিল করুন বাটনে চাপ দিতে হবে।",
                                color = Color(0xFF1565C0),
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = "প্রিমিয়ার সুবিধা বা ভেরিফিকেশন কোড সক্রিয় করুন। আপনার কোডটি নিচে সাবমিট করুন:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }

                OutlinedTextField(
                    value = if (isVerified) "ভেরিফাইড (Locked)" else codeText,
                    onValueChange = { if (!isVerified) codeText = it },
                    label = { Text("অ্যাক্টিভেশন কোড লিখুন") },
                    singleLine = true,
                    enabled = !isVerified,
                    modifier = Modifier.fillMaxWidth().testTag("premium_code_input")
                )

                if (premiumLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (statusMsg.isNotEmpty()) {
                    Text(
                        text = statusMsg,
                        color = if (statusSuccess) Color(0xFF4CAF50) else Color(0xFFE53935),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        },
        confirmButton = {
            if (isVerified) {
                // Cancel verification option
                Button(
                    onClick = {
                        viewModel.cancelPremiumVerification(
                            onSuccess = {
                                statusSuccess = true
                                codeText = ""
                                statusMsg = "পূর্বে ভেরিফিকেশন সফলভাবে বাতিল করা হয়েছে! এখন আপনি নতুন কোড প্রবেশ করাতে পারবেন।"
                                Toast.makeText(context, "ভেরিফিকেশন সফলভাবে বাতিল সম্পন্ন হয়েছে!", Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                statusSuccess = false
                                statusMsg = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    modifier = Modifier.testTag("cancel_code_button")
                ) {
                    Text("ভেরিফিকেশন বাতিল করুন", color = Color.White)
                }
            } else {
                Button(
                    onClick = {
                        if (codeText.trim().isEmpty()) {
                            statusMsg = "অ্যাক্টিভেশন কোড প্রবেশ করান!"
                            statusSuccess = false
                        } else {
                            viewModel.activatePremiumCode(
                                codeText,
                                onSuccess = { color ->
                                    statusSuccess = true
                                    statusMsg = "সফলভাবে প্রিমিয়াম সংস্করণ সক্রিয় হয়েছে! কালার আইডি: ${color.uppercase()}"
                                    Toast.makeText(context, "অভিনন্দন! প্রিমিয়াম সার্ভিস চালু হয়েছে!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    statusSuccess = false
                                    statusMsg = err
                                }
                            )
                        }
                    },
                    modifier = Modifier.testTag("activate_code_button")
                ) {
                    Text("অ্যাক্টিভেট")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন")
            }
        }
    )
}

@Composable
fun getAnimatedAccentColor(): Color {
    val infiniteTransition = rememberInfiniteTransition(label = "accent_color_transition")
    val animatedColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF00FFCC), // Neon Aqua/Teal
        targetValue = Color(0xFFFF007F),  // Electric Pink / Rose
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "global_animated_accent"
    )
    return animatedColor
}

@Composable
fun getRisingGradientBrush(): Brush {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_gold_white")
    val animOffset = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_shift"
    )

    val colors = listOf(
        Color.White,
        Color(0xFFD4AF37), // Golden
        Color.White,
        Color(0xFFD4AF37),
        Color.White
    )

    return Brush.verticalGradient(
        colors = colors,
        startY = -animOffset.value,
        endY = 500f - animOffset.value,
        tileMode = TileMode.Repeated
    )
}

fun parseVerifiedColor(colorStr: String?): Color {
    if (colorStr == null) return Color(0xFFD4AF37)
    return when (colorStr.lowercase().trim()) {
        "black" -> Color.Black
        "blue" -> Color(0xFF1E88E5)
        "green" -> Color(0xFF4CAF50)
        "red" -> Color(0xFFE53935)
        "yellow" -> Color(0xFFFFEB3B)
        "orange" -> Color(0xFFFF9800)
        "purple" -> Color(0xFF9C27B0)
        "white" -> Color.White
        else -> {
            try {
                if (colorStr.startsWith("#")) {
                    Color(android.graphics.Color.parseColor(colorStr))
                } else {
                    Color(0xFFD4AF37)
                }
            } catch (e: Exception) {
                Color(0xFFD4AF37)
            }
        }
    }
}

// Single User list Item layout
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserItemRow(
    user: User,
    unreadCount: Int,
    isSecured: Boolean,
    verifiedColor: String? = null,
    onSelect: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onSelect,
                onLongClick = onLongClick
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User Photo
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CircleGradBrush)
        ) {
            if (user.photoUrl.isNullOrEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(2).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            } else {
                AsyncImage(
                    model = user.photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Online status indicator dot representation
            // We simulate online with our getOnlineStatus logic matching DOM hashes
            val status = getSimulatedOnlineStatus(user.email)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        when (status) {
                            "online" -> Color.Green
                            "away" -> Color(0xFFF59E0B)
                            else -> Color.Gray
                        }
                    )
                    .align(Alignment.BottomEnd)
                    .border(1.5.dp, Color.White, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            val hasHighlight = hasPhotoLink(user.name)
            val cleanName = removeLinkAndLockFromName(user.name)
            val extractedImageUrl = extractLinkFromName(user.name)

            val isPremiumVerified = verifiedColor != null || user.name.contains("(Verified)")
            val finalVerifiedColor = verifiedColor ?: if (user.name.contains("(Verified)")) "blue" else null

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPremiumVerified) {
                    Text(
                        text = cleanName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = parseVerifiedColor(finalVerifiedColor),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                } else {
                    val staticColor = if (hasHighlight) Color(0xFFD4AF37) else MaterialTheme.colorScheme.onSurface
                    Text(
                        text = cleanName,
                        fontSize = 16.sp,
                        fontWeight = if (hasHighlight) FontWeight.Bold else FontWeight.SemiBold,
                        color = staticColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                if (isPremiumVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Premium Verified",
                        tint = parseVerifiedColor(finalVerifiedColor),
                        modifier = Modifier.size(16.dp)
                    )
                } else if (hasHighlight) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Highlighted",
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (!extractedImageUrl.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    AsyncImage(
                        model = extractedImageUrl,
                        contentDescription = "Contact Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0xFFD4AF37), CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            if (!user.statusMessage.isNullOrEmpty()) {
                Text(
                    text = "💬 " + user.statusMessage,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = user.email,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isSecured) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Secured",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 4.dp)
            )
        }

        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Red),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun getSimulatedOnlineStatus(email: String): String {
    val hash = email.sumOf { it.code }
    val statuses = listOf("online", "online", "online", "away", "offline")
    return statuses[hash % statuses.size]
}

// ─────────────────────────────────────────────
//  CHAT WINDOW VIEW (Chat Screen Layout)
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatWindowScreen(viewModel: EchoChatViewModel) {
    val chatUser by viewModel.currentChatUser.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val typingPartner by viewModel.typingPartnerName.collectAsState()
    val partnerOnline by viewModel.partnerOnlineStatus.collectAsState()
    val premiumVerifiedColors by viewModel.premiumVerifiedColors.collectAsState()

    val chatWallpaper by viewModel.chatWallpaper.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val autoScrollLocked by viewModel.autoScrollLocked.collectAsState()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.applyWallpaper(uri.toString())
        }
    }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var isMenuExpanded by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var isChatSearchVisible by remember { mutableStateOf(false) }
    var chatSearchQuery by remember { mutableStateOf("") }

    // Extra modal toggles matching DOM options
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showPollDialog by remember { mutableStateOf(false) }

    // Custom wallpapers
    val wallpaperModifier = Modifier.drawWithContent {
        drawContent()
        clipRect {
            when (chatWallpaper) {
                "dots" -> {
                    for (x in 0..size.width.toInt() step 50) {
                        for (y in 0..size.height.toInt() step 50) {
                            drawCircle(Color(0x1F6366F1), radius = 3f, center = Offset(x.toFloat(), y.toFloat()))
                        }
                    }
                }
                "grid" -> {
                    for (x in 0..size.width.toInt() step 60) {
                        drawLine(Color(0x0E6366F1), start = Offset(x.toFloat(), 0f), end = Offset(x.toFloat(), size.height))
                    }
                    for (y in 0..size.height.toInt() step 60) {
                        drawLine(Color(0x0E6366F1), start = Offset(0f, y.toFloat()), end = Offset(size.width, y.toFloat()))
                    }
                }
                "waves" -> {
                    val path = Path()
                    var yOffset = 0f
                    while (yOffset < size.height) {
                        path.reset()
                        path.moveTo(0f, yOffset)
                        for (x in 0..size.width.toInt() step 100) {
                            path.quadraticTo(
                                x.toFloat() + 50f, yOffset + 20f,
                                x.toFloat() + 100f, yOffset
                            )
                        }
                        drawPath(path, Color(0x0A6366F1), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
                        yOffset += 150f
                    }
                }
                "stars" -> {
                    // Draw stars
                    for (i in 0..40) {
                        val rx = (0..size.width.toInt()).random().toFloat()
                        val ry = (0..size.height.toInt()).random().toFloat()
                        drawCircle(Color(0x26FFD700), radius = 4f, center = Offset(rx, ry))
                    }
                }
            }
        }
    }

    // Scroll to latest message automatically on load or new message
    LaunchedEffect(messages.size) {
        if (!autoScrollLocked && messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { viewModel.selectChatUser(null) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        val verifiedColor = chatUser?.email?.let { premiumVerifiedColors[it] }
                        val isPremiumVerified = verifiedColor != null || chatUser?.name?.contains("(Verified)") == true
                        val finalVerifiedColor = verifiedColor ?: if (chatUser?.name?.contains("(Verified)") == true) "blue" else null
                        val cleanName = removeLinkFromName(chatUser?.name ?: "")

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isPremiumVerified) {
                                Text(
                                    text = cleanName,
                                    style = MaterialTheme.typography.titleMedium.copy(color = parseVerifiedColor(finalVerifiedColor)),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = "Premium Verified",
                                    tint = parseVerifiedColor(finalVerifiedColor),
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = cleanName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = (when (partnerOnline) {
                                "online" -> "🟢 Online"
                                "away" -> "🟡 Away"
                                else -> "⚫ Offline"
                            }) + if (!chatUser?.statusMessage.isNullOrEmpty()) " | \"${chatUser?.statusMessage}\"" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Video Call Trigger
                    IconButton(onClick = { viewModel.initiateCall("video") }) {
                        Icon(Icons.Filled.Videocam, null)
                    }

                    // Audio Call Trigger
                    IconButton(onClick = { viewModel.initiateCall("audio") }) {
                        Icon(Icons.Filled.Call, null)
                    }

                    // Search Message button
                    IconButton(onClick = {
                        isChatSearchVisible = !isChatSearchVisible
                        if (!isChatSearchVisible) {
                            chatSearchQuery = ""
                        }
                    }) {
                        Icon(
                            imageVector = if (isChatSearchVisible) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = "Search Messages"
                        )
                    }

                    // Dropdown menu trigger
                    IconButton(onClick = { isMenuExpanded = !isMenuExpanded }) {
                        Icon(Icons.Filled.MoreVert, "More Options")
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("📁 Chat Statistics") },
                            onClick = { isMenuExpanded = false; showStatsDialog = true },
                            leadingIcon = { Icon(Icons.Filled.BarChart, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("🖼️ Custom") },
                            onClick = { isMenuExpanded = false; galleryLauncher.launch("image/*") },
                            leadingIcon = { Icon(Icons.Filled.Image, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("📝 Private Chat Note") },
                            onClick = { isMenuExpanded = false; showNoteDialog = true },
                            leadingIcon = { Icon(Icons.Filled.StickyNote2, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("📊 Create Poll") },
                            onClick = { isMenuExpanded = false; showPollDialog = true },
                            leadingIcon = { Icon(Icons.Filled.Poll, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (soundEnabled) "🔕 Sound Off" else "🔔 Sound On") },
                            onClick = { isMenuExpanded = false; viewModel.setSoundEnabled(!soundEnabled) },
                            leadingIcon = { Icon(if (soundEnabled) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (autoScrollLocked) "🔓 Auto Scroll Off" else "🔒 Auto Scroll On") },
                            onClick = { isMenuExpanded = false; viewModel.setAutoScrollLocked(!autoScrollLocked) },
                            leadingIcon = { Icon(if (autoScrollLocked) Icons.Filled.LockOpen else Icons.Filled.Lock, null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Chat message searching bar
            if (isChatSearchVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = chatSearchQuery,
                        onValueChange = { chatSearchQuery = it },
                        placeholder = { Text("বার্তা খুঁজুন (Search Message)...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (chatSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { chatSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = getAnimatedAccentColor(),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
            // Typing indicator overlay
            if (typingPartner != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(getAnimatedAccentColor().copy(alpha = 0.15f))
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$typingPartner টাইপ করছে...",
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = getAnimatedAccentColor()
                    )
                }
            }

            // Message List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(Modifier.drawBehind {
                        // Support custom wall color if needed
                    })
                    .then(Modifier.background(MaterialTheme.colorScheme.background))
            ) {
                // If it is a custom wallpaper, render it with Coil AsyncImage as a background
                if (chatWallpaper.isNotEmpty() && chatWallpaper != "none" && chatWallpaper != "dots" && chatWallpaper != "grid" && chatWallpaper != "waves" && chatWallpaper != "stars") {
                    AsyncImage(
                        model = chatWallpaper,
                        contentDescription = "Custom Wallpaper",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        alpha = 0.35f,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(Modifier.drawBehind {
                            // Any wallpaper styling
                        }),
                    state = lazyListState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    items(messages.size) { i ->
                        val msg = messages[i]
                        MessageBubble(
                            msg = msg,
                            searchQuery = chatSearchQuery,
                            partnerOnline = partnerOnline,
                            onCopy = {
                                clipboardCopy(msg.text)
                            },
                            onDelete = {
                                viewModel.deleteMessage(msg.id)
                            },
                            onView = {
                                chatUser?.email?.let { otherMail ->
                                    viewModel.viewAndDecryptMessage(otherMail, msg)
                                }
                            }
                        )
                    }
                }
                
                // Wallpaper support overlays
                if (chatWallpaper != "none") {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Drawing overlays
                    }
                }
            }

            // Message Input bar layout matching premium glassmorphism
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sticker Emojis trigger
                IconButton(onClick = {
                    textInput = "😂"
                    viewModel.sendMessage("😂")
                }) {
                    Icon(Icons.Filled.Mood, null, tint = MaterialTheme.colorScheme.primary)
                }

                // Attach/Add Trigger for GPS and files
                IconButton(onClick = {
                    showPollDialog = true
                }) {
                    Icon(Icons.Filled.AddCircle, null, tint = MaterialTheme.colorScheme.primary)
                }

                OutlinedTextField(
                    value = textInput,
                    onValueChange = {
                        textInput = it
                        if (it.isEmpty()) {
                            viewModel.stopTyping()
                        } else {
                            // send typing heartbeat
                            viewModel.sendTyping()
                        }
                    },
                    placeholder = { Text("বার্তা লিখুন...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (textInput.isNotEmpty()) {
                            viewModel.sendMessage(textInput)
                            textInput = ""
                            viewModel.stopTyping()
                        }
                    },
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    // Wallpaper configuration dialog representation
    if (showWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            title = { Text("🖼️ Chat Wallpaper") },
            text = {
                Column {
                    listOf("none" to "None", "dots" to "Dots", "grid" to "Grid", "waves" to "Waves", "stars" to "Stars").forEach { (k, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.applyWallpaper(k)
                                    showWallpaperDialog = false
                                }
                                .padding(12.dp)
                        ) {
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Statistcs Dialog
    if (showStatsDialog) {
        AlertDialog(
            onDismissRequest = { showStatsDialog = false },
            title = { Text("📊 Chat Statistics") },
            text = {
                Column {
                    Text("মোট বার্তা সংখ্যাঃ ${messages.size}")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("আমার বার্তা সংখ্যাঃ ${messages.filter { it.isOwn }.size}")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("অনলাইন স্থিতিঃ ${partnerOnline}")
                }
            },
            confirmButton = {
                Button(onClick = { showStatsDialog = false }) { Text("Ok") }
            }
        )
    }

    if (showNoteDialog) {
        // Chat notes loader
        var textNote by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("📝 Private Chat Note") },
            text = {
                Column {
                    Text("এই চ্যাট সম্পর্কে নিজের ব্যক্তিগত নোট রাখুন (শুধু আপনি দেখতে পাবেন):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = textNote,
                        onValueChange = { textNote = it },
                        modifier = Modifier.height(100.dp).fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showNoteDialog = false }) { Text("Save") }
            }
        )
    }

    if (showPollDialog) {
        var pollQ by remember { mutableStateOf("") }
        var opt1 by remember { mutableStateOf("") }
        var opt2 by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPollDialog = false },
            title = { Text("📊 Create Poll") },
            text = {
                Column {
                    OutlinedTextField(value = pollQ, onValueChange = { pollQ = it }, label = { Text("Poll Question") })
                    OutlinedTextField(value = opt1, onValueChange = { opt1 = it }, label = { Text("Option 1") })
                    OutlinedTextField(value = opt2, onValueChange = { opt2 = it }, label = { Text("Option 2") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (pollQ.isNotEmpty() && opt1.isNotEmpty() && opt2.isNotEmpty()) {
                        viewModel.sendMessage("📊 POLL: $pollQ\n$opt1\n$opt2")
                        showPollDialog = false
                    }
                }) { Text("Create") }
            }
        )
    }
}

// Helper for translation lookups
fun getLocalTranslation(original: String): String {
    val clean = original.trim().lowercase()
    
    // Check some common English phrases
    if (clean.contains("hello") || clean.contains("hi") || clean.contains("hey")) return "হ্যালো! কেমন আছেন?"
    if (clean.contains("how are you")) return "আপনি কেমন আছেন?"
    if (clean.contains("what are you doing")) return "আপনি কি করছেন?"
    if (clean.contains("good morning")) return "শুভ সকাল!"
    if (clean.contains("good night")) return "শুভ রাত্রি!"
    if (clean.contains("i am fine")) return "আমি ভালো আছি।"
    if (clean.contains("thank you") || clean.contains("thanks")) return "আপনাকে ধন্যবাদ!"
    if (clean.contains("where are you")) return "আপনি কোথায় আছেন?"
    if (clean.contains("i love you")) return "আমি তোমাকে ভালোবাসি।"
    if (clean.contains("ok") || clean.contains("okay")) return "ঠিক আছে।"
    if (clean.contains("yes")) return "হ্যাঁ।"
    if (clean.contains("no")) return "না।"
    if (clean.contains("sorry")) return "দুঃখিত।"
    if (clean.contains("please")) return "দয়া করে।"
    
    // Check some common Bengali phrases
    if (clean.contains("কেমন আছ") || clean.contains("কেমন আছেন")) return "How are you?"
    if (clean.contains("হ্যালো") || clean.contains("হাই")) return "Hello!"
    if (clean.contains("কি কর") || clean.contains("কি করছেন")) return "What are you doing?"
    if (clean.contains("ভালো আছি") || clean.contains("অনেক ভালো")) return "I am doing well."
    if (clean.contains("কোথায়")) return "Where?"
    if (clean.contains("ধন্যবাদ")) return "Thank you!"
    if (clean.contains("শুভ সকাল")) return "Good morning."
    if (clean.contains("শুভ রাত্রি")) return "Good night."
    if (clean.contains("ঠিক আছে") || clean.contains("আচ্ছা")) return "Alright / Okay."
    if (clean.contains("দুঃখিত")) return "I am sorry."
    if (clean.contains("দয়া করে") || clean.contains("দয়া করে")) return "Please."
    if (clean.contains("ভালোবাসি")) return "I love you."
    
    // If it's emoji, return as is
    if (original.all { it.isSurrogate() || it.isWhitespace() || !it.isLetterOrDigit() }) return original
    
    // General simulated high-fidelity translation
    val hasBengali = original.any { it.code in 0x0980..0x09FF }
    return if (hasBengali) {
        "Translation: \"$original\""
    } else {
        "অনুবাদঃ \"$original\""
    }
}

// Single message item bubble
@Composable
fun MessageBubble(
    msg: ChatMessage,
    searchQuery: String = "",
    partnerOnline: String = "offline",
    onCopy: () -> Unit = {},
    onDelete: () -> Unit = {},
    onView: () -> Unit = {}
) {
    val align = if (msg.isOwn) Alignment.End else Alignment.Start
    val hasSearchMatch = searchQuery.isNotEmpty() && msg.text.lowercase().contains(searchQuery.lowercase())
    
    val bg = if (msg.isOwn) {
        Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7)))
    } else {
        if (hasSearchMatch) {
            Brush.linearGradient(listOf(Color(0xFFFEF08A), Color(0xFFFDE047))) // Highlighted matching message
        } else {
            Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
        }
    }
    
    val contentColor = if (msg.isOwn) {
        Color.White
    } else {
        if (hasSearchMatch) Color(0xFF854D0E) else Color.Black
    }

    var isTranslated by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("বার্তা ডিলিট করুন", fontWeight = FontWeight.Bold) },
            text = { Text("আপনি কি এই মেসেজটি গুগল শিট থেকে স্বয়ংক্রিয়ভাবে মুছে ফেলতে চান?\n\nএটি গুগল শিট (Google Sheet) ক্লাউড ডেটাবেস থেকে ডিলিট হয়ে যাবে কিন্তু লোকাল হোস্টে চিরস্থায়ীভাবে থেকে যাবে।") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    }
                ) {
                    Text("হ্যাঁ, ডিলিট করুন", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("বাতিল করুন")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (msg.isOwn) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (msg.isOwn) {
                // Delete button
                IconButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.size(28.dp).testTag("delete_message_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete from Sheet",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))

                // Translation trigger button beside own messages
                IconButton(
                    onClick = { isTranslated = !isTranslated },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Translate,
                        contentDescription = "Translate",
                        tint = if (isTranslated) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp, horizontal = 8.dp)
                    .background(bg, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .border(
                        width = if (hasSearchMatch) 2.dp else 0.dp,
                        color = Color(0xFFF59E0B),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .weight(1f, fill = false)
            ) {
                Column {
                    if (msg.isUnviewedServerMessage) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = "🔒 নতুন অপঠিত ক্লাউড বার্তা",
                                color = contentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "বার্তাটি সার্ভার থেকে ডিলিট হয়ে আপনার লোকাল ফোনে যুক্ত হবে।",
                                color = contentColor.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onView,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Visibility, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("বার্তাটী দেখুন (View)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = msg.text,
                            color = contentColor,
                            fontWeight = if (hasSearchMatch) FontWeight.Bold else FontWeight.Normal
                        )

                        if (isTranslated) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(contentColor.copy(alpha = 0.25f))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = getLocalTranslation(msg.text),
                                color = contentColor.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (!msg.isOwn) {
                Spacer(modifier = Modifier.width(4.dp))
                // Translation trigger button beside partner messages
                IconButton(
                    onClick = { isTranslated = !isTranslated },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Translate,
                        contentDescription = "Translate",
                        tint = if (isTranslated) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))

                // Delete button
                IconButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.size(28.dp).testTag("delete_message_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete from Sheet",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Delivery check status right next to / under own messages
        if (msg.isOwn) {
            Spacer(modifier = Modifier.height(1.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp)
            ) {
                // Round mark / Circle checkbox
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .background(
                            color = when (msg.deliveryStatus) {
                                "seen" -> Color(0xFF4CAF50) // Green seen
                                "delivered" -> Color(0xFF1E88E5) // Blue delivered
                                else -> Color(0xFF78909C) // Grey sent
                            },
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(7.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = when (msg.deliveryStatus) {
                        "seen" -> "সিন (Seen)"
                        "delivered" -> "ডেলিভারি (Delivered)"
                        else -> "সেন্ট (Sent)"
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LazyColumnState(): androidx.compose.foundation.lazy.LazyListState {
    return rememberLazyListState()
}

// Dummy/Mock data helpers
val CircleGradBrush = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFEC4899)))

fun getThemeGradient(key: String): Brush {
    return when (key) {
        "sunset" -> Brush.linearGradient(listOf(Color(0xFFF97316), Color(0xFFEC4899)))
        "ocean" -> Brush.linearGradient(listOf(Color(0xFF0EA5E9), Color(0xFF06B6D4)))
        "forest" -> Brush.linearGradient(listOf(Color(0xFF22C55E), Color(0xFF16A34A)))
        "midnight" -> Brush.linearGradient(listOf(Color(0xFF000000), Color(0xFF0A0A1A)))
        "neon" -> Brush.linearGradient(listOf(Color(0xFF0D0221), Color(0xFF1A0533)))
        "warm" -> Brush.linearGradient(listOf(Color(0xFF1A0A00), Color(0xFF3D1A00)))
        "rose" -> Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0xFFFB923C)))
        else -> Brush.linearGradient(listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E)))
    }
}

fun removeLinkAndLockFromName(name: String): String {
    if (name.isEmpty()) return ""
    var clean = name.replace(Regex("\\[\\{.*?\\}\\(.*\\)\\]"), "")
    clean = clean.replace(Regex("(https?://[^\\s]+)"), "").replace(Regex("[=#৳®©™]+$"), "")
    return clean.trim()
}

fun extractPinFromLockString(lockStr: String): String {
    if (lockStr.isEmpty()) return ""
    val match = Regex("\\[\\{(.*?)\\}\\(.*?\\)\\]").find(lockStr)
    return match?.groupValues?.get(1) ?: lockStr
}

fun removeLinkFromName(name: String): String {
    return removeLinkAndLockFromName(name)
}

fun hasPhotoLink(name: String): Boolean {
    val cleanName = name.replace(Regex("\\[\\{.*?\\}\\(.*\\)\\]"), "").trim()
    val match = Regex("(https?://[^\\s]+)").find(cleanName)
    return match != null
}

fun extractLinkFromName(name: String): String? {
    val cleanName = name.replace(Regex("\\[\\{.*?\\}\\(.*\\)\\]"), "").trim()
    val match = Regex("(https?://[^\\s]+)").find(cleanName)
    return match?.value
}

fun clipboardCopy(text: String) {
    // mock helper
}

fun uriToBase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        if (originalBitmap != null) {
            val size = 150
            val scaledBitmap = if (originalBitmap.width > size || originalBitmap.height > size) {
                val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                val (w, h) = if (ratio > 1) {
                    Pair(size, (size / ratio).toInt())
                } else {
                    Pair((size * ratio).toInt(), size)
                }
                Bitmap.createScaledBitmap(originalBitmap, w, h, true)
            } else {
                originalBitmap
            }
            
            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val bytes = baos.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// ─────────────────────────────────────────────
//  SETTINGS & PROFILE DIALOG WITH MULTIPLE TABS
// ─────────────────────────────────────────────
@Composable
fun ProfileModalDialog(viewModel: EchoChatViewModel, onDismiss: () -> Unit) {
    val user by viewModel.currentUser.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentTheme by viewModel.chatTheme.collectAsState()
    val allActiveUsers by viewModel.allUsers.collectAsState()
    val premiumVerifiedColors by viewModel.premiumVerifiedColors.collectAsState()

    var activeTab by remember { mutableStateOf("profile") } // "profile", "security", "appearance", "pair", "device"

    var displayName by remember(user) { mutableStateOf(user?.name ?: "") }
    var statusMsg by remember(user) { mutableStateOf(user?.statusMessage ?: "") }
    var photoUrl by remember(user) { mutableStateOf(user?.photoUrl ?: "") }
    var base64Photo by remember { mutableStateOf<String?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            val base64 = uriToBase64(context, uri)
            if (base64 != null) {
                base64Photo = base64
            }
        }
    }

    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }

    val partnerEmail by viewModel.pairPartnerEmail.collectAsState()
    val partnerName by viewModel.pairPartnerName.collectAsState()
    val unreadCounts by viewModel.unreadCounts.collectAsState()

    val signalPairRequest by viewModel.incomingPairRequest.collectAsState()
    val activeSessions by viewModel.activeSessions.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Modal Profile Avatar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                    ) {
                        if (user?.photoUrl.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (user?.name?.take(2)?.uppercase() ?: "ME"),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            AsyncImage(
                                model = user?.photoUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val verifiedColor = user?.email?.let { premiumVerifiedColors[it] }
                    val isPremiumVerified = verifiedColor != null || user?.name?.contains("(Verified)") == true
                    val finalVerifiedColor = verifiedColor ?: if (user?.name?.contains("(Verified)") == true) "blue" else null
                    val cleanSelfName = removeLinkAndLockFromName(user?.name ?: "")

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isPremiumVerified) {
                            Text(
                                text = cleanSelfName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = parseVerifiedColor(finalVerifiedColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Premium Verified",
                                tint = parseVerifiedColor(finalVerifiedColor),
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = cleanSelfName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                    Text(text = user?.email ?: "", fontSize = 13.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Tabs Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabPill(text = "👤 Profile", active = activeTab == "profile") { activeTab = "profile" }
                    TabPill(text = "🔒 Security", active = activeTab == "security") { activeTab = "security" }
                    TabPill(text = "🎨 Theme", active = activeTab == "appearance") { activeTab = "appearance" }
                    TabPill(text = "💞 Pair", active = activeTab == "pair") { activeTab = "pair" }
                    TabPill(text = "📱 Device", active = activeTab == "device") { activeTab = "device" }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (activeTab) {
                    "profile" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "আপনার প্রোফাইল সম্পাদনা করুনঃ",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )

                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("প্রদর্শন নাম (Display Name)") },
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = statusMsg,
                                onValueChange = { statusMsg = it },
                                label = { Text("স্ট্যাটাস বার্তা (Status Message)") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                placeholder = { Text("যেমনঃ ব্যস্ত আছি, ঘুমাচ্ছি...") },
                                modifier = Modifier.fillMaxWidth().testTag("profile_status_input"),
                                singleLine = true
                            )

                            // Status Presets
                            Text(
                                text = "কুইক স্ট্যাটাস সিলেক্ট করুনঃ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            val presets = listOf(
                                "ব্যস্ত আছি",
                                "গান শুনছি",
                                "ঘুমাচ্ছি",
                                "কোডিং করছি",
                                "বাইরে আছি",
                                "ক্লাসে আছি",
                                "সর্বদা ইতিবাচক!"
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                presets.forEach { prs ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (statusMsg == prs) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable { statusMsg = prs }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = prs,
                                            fontSize = 11.sp,
                                            color = if (statusMsg == prs) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Divider()

                            // Profile Photo section
                            Text(
                                text = "প্রোফাইল ছবি পরিবর্তন করুনঃ",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            // Preset Avatars
                            val avatarPresets = listOf(
                                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=120&q=80",
                                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=120&q=80",
                                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=120&q=80",
                                "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=120&q=80",
                                "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=120&q=80",
                                "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=120&q=80"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "প্রিসেট এভাটার থেকে পছন্দ করুনঃ",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    avatarPresets.forEach { presetUrl ->
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    width = if (photoUrl == presetUrl) 3.dp else 1.dp,
                                                    color = if (photoUrl == presetUrl) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    photoUrl = presetUrl
                                                    base64Photo = null
                                                }
                                        ) {
                                            AsyncImage(
                                                model = presetUrl,
                                                contentDescription = "Avatar Preset",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }

                            // Custom photo URL
                            OutlinedTextField(
                                value = if (photoUrl.startsWith("data:image")) "[স্থানীয় আপলোড ছবি / Local Uploaded Image]" else photoUrl,
                                onValueChange = {
                                    if (!it.startsWith("[স্থানীয়")) {
                                        photoUrl = it
                                        base64Photo = null
                                    }
                                },
                                label = { Text("কাস্টম ছবির URL") },
                                leadingIcon = { Icon(Icons.Default.Link, null) },
                                modifier = Modifier.fillMaxWidth().testTag("profile_photo_url_input"),
                                singleLine = true,
                                trailingIcon = {
                                    if (photoUrl.isNotEmpty()) {
                                        IconButton(onClick = {
                                            photoUrl = ""
                                            base64Photo = null
                                        }) {
                                            Icon(Icons.Default.Clear, null)
                                        }
                                    }
                                }
                            )

                            // Image Picker Trigger
                            val imagePickerLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.GetContent()
                            ) { uri: Uri? ->
                                if (uri != null) {
                                    try {
                                        val inputStream = context.contentResolver.openInputStream(uri)
                                        val originalBitmap = BitmapFactory.decodeStream(inputStream)
                                        if (originalBitmap != null) {
                                            val size = 150
                                            val scaledBitmap = if (originalBitmap.width > size || originalBitmap.height > size) {
                                                val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                                                val (w, h) = if (ratio > 1) {
                                                    Pair(size, (size / ratio).toInt())
                                                } else {
                                                    Pair((size * ratio).toInt(), size)
                                                }
                                                Bitmap.createScaledBitmap(originalBitmap, w, h, true)
                                            } else {
                                                originalBitmap
                                            }
                                            
                                            val baos = ByteArrayOutputStream()
                                            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                                            val bytes = baos.toByteArray()
                                            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                            base64Photo = b64
                                            photoUrl = "data:image/jpeg;base64,$b64"
                                            Toast.makeText(context, "ছবি সফলভাবে নির্বাচন করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "ত্রুটিঃ ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }

                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.fillMaxWidth().testTag("profile_upload_button")
                            ) {
                                Icon(Icons.Default.PhotoCamera, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("গ্যালারি থেকে ছবি আপলোড করুন")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            var isUpdatingProfile by remember { mutableStateOf(false) }

                            Button(
                                onClick = {
                                    if (displayName.isBlank()) {
                                        Toast.makeText(context, "নাম খালি হতে পারবে না!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isUpdatingProfile = true
                                    viewModel.updateProfile(
                                        name = displayName,
                                        statusMessage = statusMsg,
                                        base64Photo = base64Photo,
                                        photoUrl = if (photoUrl.startsWith("data:image")) null else photoUrl
                                    ) { success ->
                                        isUpdatingProfile = false
                                        if (success) {
                                            Toast.makeText(context, "প্রোফাইল সফলভাবে আপডেট করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, "আপডেট ব্যর্থ হয়েছে!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_save_button"),
                                enabled = !isUpdatingProfile
                            ) {
                                if (isUpdatingProfile) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                } else {
                                    Icon(Icons.Default.Save, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("প্রোফাইল সংরক্ষণ করুন")
                                }
                            }
                        }
                    }

                    "security" -> {
                        Text("পাসওয়ার্ড পরিবর্তন করুনঃ", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = currentPass,
                            onValueChange = { currentPass = it },
                            label = { Text("বর্তমান পাসওয়ার্ড") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { newPass = it },
                            label = { Text("নতুন পাসওয়ার্ড") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = confirmPass,
                            onValueChange = { confirmPass = it },
                            label = { Text("পাসওয়ার্ড নিশ্চিত করুণ") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (newPass == confirmPass && newPass.isNotEmpty()) {
                                    viewModel.changePassword(
                                        newPass,
                                        onSuccess = {
                                            Toast.makeText(context, "পাসওয়ার্ড সফলভাবে পরিবর্তিত!", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        },
                                        onError = {
                                            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, "পাসওয়ার্ড ম্যাচ করেনি!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("আপডেট করুন")
                        }
                    }

                    "appearance" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Dark Theme (রাত্রিকালীন মোড)")
                            Switch(checked = isDarkMode, onCheckedChange = { viewModel.setDarkMode(it) })
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("অ্যাপের ব্যাকগ্রাউন্ড থিম নির্বাচন করুণঃ", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Render color buttons
                        val themes = listOf("default", "sunset", "ocean", "forest", "midnight", "neon", "warm", "rose")
                        Column {
                            themes.chunked(4).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { t ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(getThemeGradient(t))
                                                .clickable { viewModel.applyTheme(t) }
                                                .border(
                                                    width = if (currentTheme == t) 2.dp else 0.dp,
                                                    color = Color.White,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    "pair" -> {
                        // Recover Pairing tab logic
                        if (partnerEmail.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Green.copy(alpha = 0.1f))
                                    .padding(12.dp)
                            ) {
                                Text("✅ আপনি Paired আছেন!", fontWeight = FontWeight.Bold)
                                Text("অংশীদারঃ $partnerName ($partnerEmail)")
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.removePair("1234", onSuccess = {}, onError = {})
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                ) {
                                    Text("Pair সরান")
                                }
                            }
                        } else {
                            if (signalPairRequest != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "💞 ইনকামিং পেয়ার অনুরোধ!",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${signalPairRequest!!.fromName} (${signalPairRequest!!.fromEmail}) আপনাকে পেয়ার করার অনুরোধ পাঠিয়েছেন। সংযোগ সম্পন্ন করতে অংশীদারের কাছ থেকে পাওয়া অস্থায়ী ৬-সংখ্যার কোডটি নিচে লিখুনঃ",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        var codeToMatch by remember { mutableStateOf("") }
                                        OutlinedTextField(
                                            value = codeToMatch,
                                            onValueChange = { codeToMatch = it },
                                            placeholder = { Text("৬-সংখ্যার পেয়ারিং কোড") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("pair_code_input")
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.acceptPairRequestWithCode(
                                                        fromEmail = signalPairRequest!!.fromEmail,
                                                        enteredCode = codeToMatch,
                                                        onSuccess = {
                                                            Toast.makeText(context, "সফলভাবে পেয়ার করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        onError = { err ->
                                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                                },
                                                modifier = Modifier.testTag("pair_confirm_button")
                                            ) {
                                                Text("নিশ্চিত করুন")
                                            }
                                            TextButton(
                                                onClick = {
                                                    viewModel.respondToPairRequest(signalPairRequest!!.fromEmail, accept = false)
                                                }
                                            ) {
                                                Text("বাতিল", color = Color.Red)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Text("অন্য কোনো ব্যবহারকারীকে পেয়ার করার জন্য অনুরোধ পাঠানঃ", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            var reqMail by remember { mutableStateOf("") }
                            var pairRequestedEmailNotFound by remember { mutableStateOf<String?>(null) }
                            var pairRequestErrorMsg by remember { mutableStateOf<String?>(null) }

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = reqMail,
                                    onValueChange = { 
                                        reqMail = it 
                                        if (it.isEmpty()) {
                                            pairRequestedEmailNotFound = null
                                            pairRequestErrorMsg = null
                                        }
                                    },
                                    placeholder = { Text("অংশীদারের ইমেইল...") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    val exists = allActiveUsers.any { it.email.equals(reqMail.trim(), ignoreCase = true) }
                                    if (!exists) {
                                        pairRequestedEmailNotFound = reqMail
                                        pairRequestErrorMsg = "ইমেইলটি নিবন্ধিত নেই!"
                                    } else {
                                        pairRequestedEmailNotFound = null
                                        pairRequestErrorMsg = null
                                        viewModel.submitPairRequest(reqMail, onSuccess = {
                                            Toast.makeText(context, "Pair Request Sent!", Toast.LENGTH_SHORT).show()
                                        }, onError = {
                                            pairRequestErrorMsg = it
                                        })
                                    }
                                }) {
                                    Text("অনুরোধ")
                                }
                            }

                            if (reqMail.isNotEmpty() && allActiveUsers.any { it.email.equals(reqMail.trim(), ignoreCase = true) }) {
                                val pairingCode = viewModel.getPairingCode(user?.email ?: "", reqMail)
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "🤝 আপনার অস্থায়ী পেয়ারিং কোডঃ",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = pairingCode,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                        Text(
                                            text = "সংযোগ সম্পন্ন করতে আপনার অংশীদারকে কোডটি এই সেটিংসে প্রবেশ করাতে বলুন। কোনো অগ্রিম SMS লাগবে না।",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            if (pairRequestedEmailNotFound != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ErrorOutline,
                                            contentDescription = "Error Contact",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "ত্রুটিপূর্ণ সংযোগ (Error Contact) ⚠️",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${pairRequestedEmailNotFound} সংযোগ করতে ব্যর্থ হয়েছে!",
                                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "device" -> {
                        Text("Active Devices Concurrency (সেশনসমূহ):", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(modifier = Modifier.height(180.dp)) {
                            items(activeSessions) { session ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(session.deviceModel, fontWeight = FontWeight.SemiBold)
                                            Text(session.os, fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = { viewModel.terminateRemoteSession(session.sessionId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                        ) {
                                            Text("End Session", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dialog helper extension helper
fun EchoChatViewModel.changePassword(newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    // stub connector
}

@Composable
fun TabPill(text: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(text = text, color = if (active) Color.White else Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────
//  CALL MANAGING SINK (Incoming & Active Call)
// ─────────────────────────────────────────────
@Composable
fun CallManagerOverlay(viewModel: EchoChatViewModel) {
    val callState by viewModel.callState.collectAsState()
    val callDuration by viewModel.callDuration.collectAsState()
    val isMuted by viewModel.isCallMuted.collectAsState()
    val isCameraOff by viewModel.isCallCameraOff.collectAsState()

    val minutes = String.format("%02d", callDuration / 60)
    val seconds = String.format("%02d", callDuration % 60)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale1"
    )
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale2"
    )
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha2"
    )

    when (callState) {
        is CallState.Outgoing -> {
            val state = callState as CallState.Outgoing
            Dialog(onDismissRequest = {}) {
                Surface(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (state.callType == "video") "📹 Video Call Initializing..." else "📞 Voice Call Initializing...",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                            // Pulsing Ring 1
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .graphicsLayer(
                                        scaleX = pulseScale1,
                                        scaleY = pulseScale1,
                                        alpha = pulseAlpha1
                                    )
                                    .background(getAnimatedAccentColor().copy(alpha = 0.35f), CircleShape)
                            )
                            // Pulsing Ring 2
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .graphicsLayer(
                                        scaleX = pulseScale2,
                                        scaleY = pulseScale2,
                                        alpha = pulseAlpha2
                                    )
                                    .background(getAnimatedAccentColor().copy(alpha = 0.5f), CircleShape)
                            )
                            // Core Avatar
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(getAnimatedAccentColor()),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = removeLinkAndLockFromName(state.partnerName).take(2).uppercase(),
                                    fontSize = 32.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(removeLinkAndLockFromName(state.partnerName), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("Calling...", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(32.dp))

                        IconButton(
                            onClick = { viewModel.cancelOutgoingCall() },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        ) {
                            Icon(Icons.Filled.CallEnd, null, tint = Color.White)
                        }
                    }
                }
            }
        }

        is CallState.Incoming -> {
            val state = callState as CallState.Incoming
            Dialog(onDismissRequest = {}) {
                Surface(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (state.callType == "video") "Incoming Video Call 🔔" else "Incoming Voice Call 🔔",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                            // Pulsing Ring 1
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .graphicsLayer(
                                        scaleX = pulseScale1,
                                        scaleY = pulseScale1,
                                        alpha = pulseAlpha1
                                    )
                                    .background(getAnimatedAccentColor().copy(alpha = 0.35f), CircleShape)
                            )
                            // Pulsing Ring 2
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .graphicsLayer(
                                        scaleX = pulseScale2,
                                        scaleY = pulseScale2,
                                        alpha = pulseAlpha2
                                    )
                                    .background(getAnimatedAccentColor().copy(alpha = 0.5f), CircleShape)
                            )
                            // Core avatar
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(getAnimatedAccentColor()),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = removeLinkAndLockFromName(state.partnerName).take(2).uppercase(),
                                    fontSize = 32.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(removeLinkAndLockFromName(state.partnerName), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.rejectIncomingCall() },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            ) {
                                Icon(Icons.Filled.CallEnd, null, tint = Color.White)
                            }
                            IconButton(
                                onClick = { viewModel.acceptIncomingCall() },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF25D366)) // WhatsApp active green
                            ) {
                                Icon(if (state.callType == "video") Icons.Filled.Videocam else Icons.Filled.Call, null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        is CallState.Connected -> {
            val state = callState as CallState.Connected
            Dialog(onDismissRequest = {}) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A) // Dark slate background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background visuals & centered caller identity
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                                // Subtle infinite animation for connected voice calls to make them feel alive
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .graphicsLayer(
                                            scaleX = pulseScale2,
                                            scaleY = pulseScale2,
                                            alpha = pulseAlpha2 * 0.5f
                                        )
                                        .background(getAnimatedAccentColor().copy(alpha = 0.4f), CircleShape)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(getAnimatedAccentColor()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = removeLinkAndLockFromName(state.partnerName).take(2).uppercase(),
                                        color = Color.White,
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(removeLinkAndLockFromName(state.partnerName), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "$minutes:$seconds",
                                color = Color(0xFF38BDF8), // sky blue highlight
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Call controls bottom bar representation with modern styled translucent buttons
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(vertical = 32.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.toggleCallMute() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(if (isMuted) Color.White.copy(alpha = 0.25f) else Color.Transparent, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }

                            FloatingActionButton(
                                onClick = { viewModel.endActiveCall() },
                                containerColor = Color.Red,
                                contentColor = Color.White,
                                shape = CircleShape,
                                modifier = Modifier.size(60.dp)
                            ) {
                                Icon(Icons.Filled.CallEnd, null, modifier = Modifier.size(28.dp))
                            }

                            IconButton(
                                onClick = { viewModel.toggleCallCamera() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(if (isCameraOff) Color.White.copy(alpha = 0.25f) else Color.Transparent, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isCameraOff) Icons.Filled.VideocamOff else Icons.Filled.Videocam,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        else -> {}
    }
}
