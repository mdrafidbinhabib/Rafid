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
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF0078FF), Color(0xFF00C6FF)))),
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
                    modifier = Modifier.padding(bottom = 16.dp)
                )

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
    val currentChatUser by viewModel.currentChatUser.collectAsState()
    val allActiveUsers by viewModel.allUsers.collectAsState()
    val recentChats by viewModel.recentChats.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val unreadCounts by viewModel.unreadCounts.collectAsState()
    val securedChats by viewModel.securedChats.collectAsState()

    var showProfileModal by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchField by remember { mutableStateOf("") }

    var lockPromptEmail by remember { mutableStateOf<String?>(null) }
    var lockPromptPassword by remember { mutableStateOf("") }
    var lockPromptError by remember { mutableStateOf(false) }

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
                            if (!isSearchActive) searchField = ""
                        }) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Filled.Close else Icons.Filled.Search,
                                contentDescription = "Search"
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

                    val filteredUsers = if (searchField.isNotEmpty()) {
                        allActiveUsers.filter { u ->
                            u.name.lowercase().contains(searchField.lowercase()) ||
                                    u.email.lowercase().contains(searchField.lowercase())
                        }
                    } else {
                        emptyList()
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
            title = { Text(removeLinkAndLockFromName(target.name)) },
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
}

// Single User list Item layout
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserItemRow(
    user: User,
    unreadCount: Int,
    isSecured: Boolean,
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

            val colorAnimation = if (hasHighlight) {
                val infiniteTransition = rememberInfiniteTransition(label = "gold_pulse")
                infiniteTransition.animateColor(
                    initialValue = Color(0xFFD4AF37), // Golden
                    targetValue = Color.White,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "goldAnimation"
                ).value
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = cleanName,
                    fontSize = 16.sp,
                    fontWeight = if (hasHighlight) FontWeight.Bold else FontWeight.SemiBold,
                    color = colorAnimation,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (hasHighlight) {
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
            Text(
                text = user.email,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

    val chatWallpaper by viewModel.chatWallpaper.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val autoScrollLocked by viewModel.autoScrollLocked.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var isMenuExpanded by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

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
                        Text(
                            text = removeLinkFromName(chatUser?.name ?: ""),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (partnerOnline) {
                                "online" -> "🟢 Online"
                                "away" -> "🟡 Away"
                                else -> "⚫ Offline"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                            text = { Text("🖼️ Chat Wallpaper") },
                            onClick = { isMenuExpanded = false; showWallpaperDialog = true },
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
            // Typing indicator overlay
            if (typingPartner != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$typingPartner টাইপ করছে...",
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.primary
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
                        MessageBubble(msg, onCopy = {
                            clipboardCopy(msg.text)
                        })
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
                        // send typing heartbeat
                        viewModel.sendTyping()
                    },
                    placeholder = { Text("বার্তা লিখুন...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
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

// Single message item bubble
@Composable
fun MessageBubble(msg: ChatMessage, onCopy: () -> Unit) {
    val align = if (msg.isOwn) Alignment.End else Alignment.Start
    val bg = if (msg.isOwn) {
        Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
    }
    val contentColor = if (msg.isOwn) Color.White else Color.Black

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .background(bg, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(text = msg.text, color = contentColor)
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

// ─────────────────────────────────────────────
//  SETTINGS & PROFILE DIALOG WITH MULTIPLE TABS
// ─────────────────────────────────────────────
@Composable
fun ProfileModalDialog(viewModel: EchoChatViewModel, onDismiss: () -> Unit) {
    val user by viewModel.currentUser.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentTheme by viewModel.chatTheme.collectAsState()
    val allActiveUsers by viewModel.allUsers.collectAsState()

    var activeTab by remember { mutableStateOf("security") } // "security", "appearance", "pair", "device"

    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }

    val context = LocalContext.current

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
                    Text(text = user?.name ?: "", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                    TabPill(text = "🔒 Security", active = activeTab == "security") { activeTab = "security" }
                    TabPill(text = "🎨 Theme", active = activeTab == "appearance") { activeTab = "appearance" }
                    TabPill(text = "💞 Pair", active = activeTab == "pair") { activeTab = "pair" }
                    TabPill(text = "📱 Device", active = activeTab == "device") { activeTab = "device" }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (activeTab) {
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
                            Text("আপনি Paired নেই। রিসেট কোড পেতে অংশীদার করুনঃ")
                            Spacer(modifier = Modifier.height(8.dp))
                            var reqMail by remember { mutableStateOf("") }
                            var pairRequestedEmailNotFound by remember { mutableStateOf<String?>(null) }
                            var pairRequestErrorMsg by remember { mutableStateOf<String?>(null) }

                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = reqMail,
                                    onValueChange = { 
                                        reqMail = it 
                                        if (it.isEmpty()) {
                                            pairRequestedEmailNotFound = null
                                            pairRequestErrorMsg = null
                                        }
                                    },
                                    placeholder = { Text("ইমেইল লিখুন...") },
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
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
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
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                            )
                            // Core Avatar
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
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
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
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
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                            )
                            // Core avatar
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary),
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
                                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(Color.Gray),
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
