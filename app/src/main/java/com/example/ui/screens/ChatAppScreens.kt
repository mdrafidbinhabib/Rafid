package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.viewinterop.AndroidView
import android.hardware.Camera
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.AudioTrack
import android.media.AudioManager
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield


fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val pureBase64 = if (base64Str.contains(",")) {
            base64Str.substring(base64Str.indexOf(",") + 1)
        } else {
            base64Str
        }
        val decodedBytes = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}

@Composable
fun SafeAvatarImage(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alpha: Float = 1.0f
) {
    if (model.isNullOrEmpty()) {
        Image(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = contentDescription,
            modifier = modifier,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary.copy(alpha = alpha)),
            contentScale = contentScale
        )
    } else if (model.startsWith("data:image/")) {
        val bitmap = remember(model) { decodeBase64ToBitmap(model) }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = modifier,
                alpha = alpha,
                contentScale = contentScale
            )
        } else {
            Image(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = contentDescription,
                modifier = modifier,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary.copy(alpha = alpha)),
                contentScale = contentScale
            )
        }
    } else {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = modifier,
            alpha = alpha,
            contentScale = contentScale,
            error = rememberVectorPainter(Icons.Default.AccountCircle)
        )
    }
}

// Root App Screen Router
@Composable
fun PermissionBlockerScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    val permissions = remember {
        val list = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            list.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    var permissionsGrantedState by remember {
        mutableStateOf(
            permissions.all {
                androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val isBatteryOptimized = remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                pm.isIgnoringBatteryOptimizations(context.packageName).not()
            } else {
                false
            }
        )
    }

    var skipBatteryOptimizations by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                isBatteryOptimized.value = pm.isIgnoringBatteryOptimizations(context.packageName).not()
            } else {
                isBatteryOptimized.value = false
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    val isBypassedOrSkipped = !isBatteryOptimized.value || skipBatteryOptimizations

    val launcher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineLocationOk = results[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationOk = results[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true

        val postNotificationOk = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            results[android.Manifest.permission.POST_NOTIFICATIONS] == true
        } else {
            true
        }
        val locationOk = fineLocationOk || coarseLocationOk

        if (locationOk && postNotificationOk) {
            permissionsGrantedState = true
        } else {
            Toast.makeText(context, "অ্যাপটি ব্যবহারের জন্য লোকেশন ও নোটিফিকেশন পারমিশন আবশ্যক!", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(permissionsGrantedState, isBypassedOrSkipped) {
        if (permissionsGrantedState && isBypassedOrSkipped) {
            try {
                val serviceIntent = android.content.Intent(context, com.example.service.EchoNotificationService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onAllGranted()
        }
    }

    if (!permissionsGrantedState || !isBypassedOrSkipped) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E1B4B),
                            Color(0xFF311042)
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F1524).copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                border = BorderStroke(1.5.dp, Color(0xFFE040FB).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!permissionsGrantedState) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color(0xFFFF5722).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFF5722),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = "পারমিশন প্রয়োজন",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "অ্যাপটি ব্যবহার করতে নিচের পারমিশনগুলো দেওয়া আবশ্যক। পারমিশন না দিলে অ্যাপে প্রবেশ করা যাবে না।",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Divider(color = Color.White.copy(alpha = 0.1f))

                        PermissionItemRow(
                            icon = Icons.Default.LocationOn,
                            title = "লোকেশন পারমিশন (Location)",
                            desc = "আপনার অবস্থান নির্ভুলভাবে যাচাই ও শেয়ার করতে লোকেশন প্রয়োজন।"
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        PermissionItemRow(
                            icon = Icons.Default.Notifications,
                            title = "নোটিফিকেশন পারমিশন (Notifications)",
                            desc = "রিয়েল-টাইম এসএমএস এবং কল নোটিফিকেশন পেতে নোটিফিকেশন পারমিশন আবশ্যক।"
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                launcher.launch(permissions)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("grant_permissions_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "✅ লোকেশন ও নোটিফিকেশন পারমিশন দিন",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color(0xFFFFEB3B).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFEB3B),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = "ব্যাকগ্রাউন্ড রান অনুমতি",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "নোটিফিকেশন ও ব্যাকগ্রাউন্ড সার্ভিস ১০০% সচল রাখতে ব্যাটারি অপটিমাইজেশন বন্ধ করা আবশ্যক। নিচে ক্লিক করে 'Allow/Don't Restrict' সিলেক্ট করুন।",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Divider(color = Color.White.copy(alpha = 0.1f))

                        PermissionItemRow(
                            icon = Icons.Default.Star,
                            title = "ব্যাটারি অপটিমাইজেশন বাইপাস করুন",
                            desc = "অ্যান্ড্রয়েড সিস্টেম যাতে ব্যাকগ্রাউন্ডে চ্যাট সার্ভিস বন্ধ না করে দেয়।"
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                triggerBackgroundOptimizationExemption(context)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("grant_battery_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE040FB),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "🔋 অপটিমাইজেশন বন্ধ করুন (Allow Always)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = {
                                skipBatteryOptimizations = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "পরবর্তীতে করব (Skip for now)",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

fun triggerBackgroundOptimizationExemption(context: android.content.Context) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
            try {
                val intent = android.content.Intent().apply {
                    action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = android.content.Intent().apply {
                        action = android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }
}

@Composable
fun PermissionItemRow(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFE040FB).copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFE040FB),
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.55f),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun EchoChatApp(viewModel: EchoChatViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentTheme by viewModel.chatTheme.collectAsState()
    val offensiveWarningMessage by viewModel.offensiveWarningMessage.collectAsState()
    val latestVersionInfo by viewModel.latestVersionInfo.collectAsState()
    val currentAppVersion = remember { viewModel.getCurrentAppVersion() }
    var userSkippedVersion by remember(currentUser) { mutableStateOf(viewModel.getSkippedVersion()) }

    val bgBrush = getThemeGradient(currentTheme)

    val context = LocalContext.current
    val requiredPermissions = remember {
        val list = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            list.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    var allPermissionsGranted by remember {
        mutableStateOf(
            requiredPermissions.all {
                androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted) {
            try {
                val serviceIntent = android.content.Intent(context, com.example.service.EchoNotificationService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    MyCustomTheme(isDarkMode = isDarkMode) {
        if (!allPermissionsGranted) {
            PermissionBlockerScreen(onAllGranted = { allPermissionsGranted = true })
        } else {
            if (offensiveWarningMessage != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissOffensiveWarning() },
                    title = { Text("⚠️ কন্টেন্ট সতর্কতা (Content Warning)") },
                    text = { Text(offensiveWarningMessage!!) },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.dismissOffensiveWarning() }
                        ) {
                            Text("ঠিক আছে")
                        }
                    }
                )
            }

            val latestVer = latestVersionInfo
            val isUserRafid = viewModel.isRafidUser(currentUser)
            if (latestVer != null && latestVer.versionNumber != currentAppVersion && !isUserRafid && latestVer.versionNumber != userSkippedVersion) {
                AlertDialog(
                    onDismissRequest = {
                        viewModel.setSkippedVersion(latestVer.versionNumber)
                        userSkippedVersion = latestVer.versionNumber
                    },
                    title = { Text("🚀 নতুন আপডেট উপলব্ধ (${latestVer.versionNumber})") },
                    text = {
                        Column {
                            Text(latestVer.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("নতুন ভার্সন এসেছে ডাউনলোড করেন।")
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.setSkippedVersion(latestVer.versionNumber)
                                userSkippedVersion = latestVer.versionNumber
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(latestVer.link))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        ) {
                            Text("ডাউনলোড")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                viewModel.setSkippedVersion(latestVer.versionNumber)
                                userSkippedVersion = latestVer.versionNumber
                            }
                        ) {
                            Text("স্কিপ")
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgBrush)
            ) {
                AnimatedContent(
                    targetState = currentUser == null,
                    transitionSpec = {
                        (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))).togetherWith(
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)) + scaleOut(targetScale = 0.9f, animationSpec = spring(stiffness = Spring.StiffnessLow))
                        )
                    },
                    label = "app_screens_transition"
                ) { isAuthRequired ->
                    if (isAuthRequired) {
                        AuthScreen(viewModel = viewModel)
                    } else {
                        DashboardScreen(viewModel = viewModel)
                    }
                }

                // Global Overlays (e.g. Calls)
                CallManagerOverlay(viewModel = viewModel)

                // App Lock Screen Overlay
                val isAppLocked by viewModel.isAppLocked.collectAsState()
                val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

                if (isAppLocked) {
                    AppLockScreenOverlay(viewModel = viewModel)
                } else if (isAppLockEnabled && lifecycleState < androidx.lifecycle.Lifecycle.State.RESUMED) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .clickable(enabled = true, onClick = {})
                    )
                }
            }
        }
    }
}

@Composable
fun MyCustomTheme(
    isDarkMode: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkMode) {
        darkColorScheme(
            primary = Color(0xFFFF5722), // Vibrant Sunset Coral
            secondary = Color(0xFFE040FB), // Vibrant Neon Orchid
            tertiary = Color(0xFF00E676), // Glow Mint
            background = Color.Black, // Pure Black
            surface = Color.Black, // Pure Black
            onPrimary = Color.White,
            onSecondary = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White,
            surfaceVariant = Color(0xFF121212),
            onSurfaceVariant = Color.White
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFFF5722), // Vibrant Sunset Coral
            secondary = Color(0xFFE040FB), // Vibrant Neon Orchid
            tertiary = Color(0xFF00E676), // Glow Mint
            background = Color.Black, // Pure Black
            surface = Color.Black, // Pure Black
            onPrimary = Color.White,
            onSecondary = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White,
            surfaceVariant = Color(0xFF121212),
            onSurfaceVariant = Color.White
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
    var photoName by remember { mutableStateOf("কোনো ছবি নির্বাচন করা হয়নি") }

    val authImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                if (originalBitmap != null) {
                    val size = 150
                    val scaledBitmap = if (originalBitmap.width > size || originalBitmap.height > size) {
                        val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                        val (w, h) = if (ratio > 1) {
                            Pair(size, (size / ratio).toInt())
                        } else {
                            Pair((size * ratio).toInt(), size)
                        }
                        android.graphics.Bitmap.createScaledBitmap(originalBitmap, w, h, true)
                    } else {
                        originalBitmap
                    }
                    val baos = java.io.ByteArrayOutputStream()
                    scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, baos)
                    val bytes = baos.toByteArray()
                    val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    base64Photo = "data:image/jpeg;base64,$b64"
                    photoName = "ছবি সফলভাবে যুক্ত হয়েছে ✔"
                }
            } catch (e: Exception) {
                Toast.makeText(context, "ছবি লোড করতে ব্যর্থ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Forgot password fields
    var forgotStep by remember { mutableStateOf(1) } // 1, 2, 3
    var forgotEmail by remember { mutableStateOf("") }
    var forgotPartnerEmail by remember { mutableStateOf("") }
    var forgotViaPartner by remember { mutableStateOf(false) }
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

                // Premium Secured Message Banner
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ক্লাউড-সুরক্ষিত চ্যাট স্পেস",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "আপনার অ্যাকাউন্ট ব্যবহার করে নিরাপদে লগইন করুন",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
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
                                    try {
                                        val serviceIntent = android.content.Intent(context, com.example.service.EchoNotificationService::class.java)
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            context.startForegroundService(serviceIntent)
                                        } else {
                                            context.startService(serviceIntent)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
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
                                text = "নতুন অ্যাকাউন্ট খুলুন (Register)",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable { screenMode = "register" }
                                    .padding(4.dp),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "পাসওয়ার্ড ভুলে গেছেন? (Forgot)",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable { screenMode = "forgot"; forgotStep = 1 }
                                    .padding(4.dp),
                                fontWeight = FontWeight.Medium
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
                                    authImagePickerLauncher.launch("image/*")
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
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
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
                            text = "Reset Access Key",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (forgotStep == 1) {
                            OutlinedTextField(
                                value = forgotEmail,
                                onValueChange = { forgotEmail = it },
                                label = { Text("আপনার ইমেইল (My Email)") },
                                leadingIcon = { Icon(Icons.Default.Email, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { forgotViaPartner = !forgotViaPartner }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = forgotViaPartner,
                                    onCheckedChange = { forgotViaPartner = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "পেয়ারকৃত মেম্বারের মাধ্যমে পাসওয়ার্ড রিসেট করুন",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (forgotViaPartner) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = forgotPartnerEmail,
                                    onValueChange = { forgotPartnerEmail = it },
                                    label = { Text("পেয়ারকৃত মেম্বারের ইমেইল (Partner Email)") },
                                    leadingIcon = { Icon(Icons.Default.People, null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "💡 নির্দেশনাঃ কোডটি আপনার পেয়ার করা পার্টনারের অ্যাপ স্ক্রিনে পাঠানো হবে। কোডটি পার্টনারের থেকে সংগ্রহ করুন।",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    lineHeight = 15.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (forgotEmail.trim().isEmpty()) {
                                        Toast.makeText(context, "আপনার ইমেইল প্রদান করুন", Toast.LENGTH_SHORT).show()
                                    } else if (forgotViaPartner && forgotPartnerEmail.trim().isEmpty()) {
                                        Toast.makeText(context, "পার্টনারের ইমেইল প্রদান করুন", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.requestForgotPasswordCode(forgotEmail.trim(), {
                                            if (forgotViaPartner) {
                                                Toast.makeText(context, "কোডটি আপনার পার্টনারের অ্যাপ স্ক্রিনে পাঠানো হয়েছে!", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "রিসেট কোড পাঠানো হয়েছে!", Toast.LENGTH_SHORT).show()
                                            }
                                            forgotStep = 2
                                        }, { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                        })
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                enabled = !authLoading
                            ) {
                                if (authLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("কোড পাঠান", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (forgotStep == 2) {
                            Text(
                                text = "কোডটি আপনার ইমেইলে পাঠানো হয়েছে। কোড লিখুনঃ",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            OutlinedTextField(
                                value = forgotCode,
                                onValueChange = { forgotCode = it },
                                label = { Text("Verification Code") },
                                leadingIcon = { Icon(Icons.Default.VpnKey, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (timerSecs > 0) {
                                    Text("রিসেন্ড করুন ($timerSecs s)", fontSize = 12.sp, color = Color.Gray)
                                } else {
                                    Text(
                                        text = "কোড আবার পাঠান (Resend)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable {
                                            viewModel.requestForgotPasswordCode(forgotEmail.trim(), {
                                                Toast.makeText(context, "কোড পুনরায় পাঠানো হয়েছে!", Toast.LENGTH_SHORT).show()
                                            }, { err ->
                                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                            })
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (forgotCode.trim().isEmpty()) {
                                        Toast.makeText(context, "কোড প্রদান করুন", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.verifyForgotPasswordCode(forgotEmail.trim(), forgotCode.trim(), {
                                            Toast.makeText(context, "কোড ভেরিফিকেশন সফল!", Toast.LENGTH_SHORT).show()
                                            forgotStep = 3
                                        }, { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                        })
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                enabled = !authLoading
                            ) {
                                if (authLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("কোড যাচাই করুন", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = forgotNewPass,
                                onValueChange = { forgotNewPass = it },
                                label = { Text("New Access Key") },
                                leadingIcon = { Icon(Icons.Default.Lock, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (forgotNewPass.trim().isEmpty()) {
                                        Toast.makeText(context, "নতুন পাসওয়ার্ড লিখুন", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.resetPassword(forgotEmail.trim(), forgotNewPass.trim(), {
                                            Toast.makeText(context, "পাসওয়ার্ড সফলভাবে পরিবর্তন করা হয়েছে!", Toast.LENGTH_LONG).show()
                                            screenMode = "login"
                                        }, { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                        })
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                enabled = !authLoading
                            ) {
                                if (authLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("পাসওয়ার্ড রিসেট করুন", fontWeight = FontWeight.Bold)
                                }
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
    val hiddenChats by viewModel.hiddenChats.collectAsState()
    val mutedChats by viewModel.mutedChats.collectAsState()
    val lastMessageSenderMap by viewModel.lastMessageSenderMap.collectAsState()
    val usersOnlineStatuses by viewModel.usersOnlineStatuses.collectAsState()
    val spyingOnUser by viewModel.spyingOnUser.collectAsState()
    val agreedUsers by viewModel.agreedUsers.collectAsState()
    val allRawMessages by viewModel.allRawMessages.collectAsState()

    var usersTabSearchQuery by remember { mutableStateOf("") }
    var spySelectedUser by remember { mutableStateOf<User?>(null) }

    val hasUnreadMessages = remember(unreadCounts) { unreadCounts.values.any { it > 0 } }
    val hasUnreadHiddenMessages = remember(unreadCounts, hiddenChats) { hiddenChats.keys.any { email -> (unreadCounts[email] ?: 0) > 0 } }

    var showProfileModal by remember { mutableStateOf(false) }
    var showPremiumModal by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var isSearchLoading by remember { mutableStateOf(false) }
    var searchField by remember { mutableStateOf("") }
    var shuffledUsers by remember { mutableStateOf<List<User>>(emptyList()) }

    LaunchedEffect(isSearchActive, allActiveUsers) {
        if (isSearchActive) {
            shuffledUsers = allActiveUsers.shuffled()
        }
    }

    val isViewingHidden = remember(searchField, currentChatUser, hiddenChats, allActiveUsers) {
        val isViewingHiddenFolder = searchField.isNotEmpty() && allActiveUsers.any { u ->
            val isHidden = hiddenChats.containsKey(u.email)
            isHidden && searchField.trim().lowercase() == (hiddenChats[u.email] ?: "").trim().lowercase()
        }
        val isViewingHiddenChat = currentChatUser?.let { hiddenChats.containsKey(it.email) } == true
        isViewingHiddenFolder || isViewingHiddenChat
    }

    LaunchedEffect(isViewingHidden) {
        viewModel.setViewingHidden(isViewingHidden)
    }

    val myGroups by viewModel.myGroups.collectAsState()
    val groupMembers by viewModel.groupMembers.collectAsState()
    val partnerResetCode by viewModel.partnerResetCode.collectAsState()
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var dashboardTab by remember { mutableStateOf("all") }
    var showGroupCustomizerDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<User?>(null) }



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
    var showHideSetDialog by remember { mutableStateOf(false) }
    var hideTextValue by remember { mutableStateOf("") }
    var showMuteDialog by remember { mutableStateOf(false) }
    var muteDialogUser by remember { mutableStateOf<User?>(null) }

    AnimatedContent(
        targetState = currentChatUser,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) { width -> width } + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))).togetherWith(
                    slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> -width } + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                )
            } else {
                (slideInHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) { width -> -width } + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))).togetherWith(
                    slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> width } + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                )
            }
        },
        label = "dashboard_chat_window_transition"
    ) { chatUser ->
        if (chatUser != null) {
            ChatWindowScreen(viewModel = viewModel, onEditGroup = { groupToEdit = it; showGroupCustomizerDialog = true })
        } else {
            Scaffold(
                containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                Icon(Icons.Filled.ChatBubble, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            var titleClickCount by remember { mutableStateOf(0) }
                            val isUserRafid = remember(currentUser) { viewModel.isRafidUser(currentUser) }
                            Box(modifier = Modifier.clickable {
                                if (isUserRafid) {
                                    titleClickCount++
                                    if (titleClickCount >= 5) {
                                        titleClickCount = 0
                                        viewModel.toggleForceAdmin()
                                        android.widget.Toast.makeText(context, "Admin Mode Toggled!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "অ্যাক্সেস অস্বীকার করা হয়েছে!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                val hasUnreadHiddenMessages = remember(unreadCounts, hiddenChats) {
                                    unreadCounts.any { (email, count) -> count > 0 && hiddenChats.containsKey(email) }
                                }
                                val onBgColor = MaterialTheme.colorScheme.onBackground
                                val headerText = buildAnnotatedString {
                                    if (hasUnreadHiddenMessages) {
                                        withStyle(SpanStyle(color = Color(0xFFE040FB))) {
                                            append("Echo ")
                                        }
                                        withStyle(SpanStyle(color = Color(0xFF00E5FF))) {
                                            append("Chat")
                                        }
                                    } else {
                                        withStyle(SpanStyle(color = onBgColor)) {
                                            append("Echo Chat")
                                        }
                                    }
                                }
                                Text(text = headerText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }
                    },
                    actions = {
                        // Search Toggle Action Icon
                        IconButton(onClick = {
                            isSearchActive = !isSearchActive
                            if (isSearchActive) {
                                isSearchLoading = true
                                shuffledUsers = allActiveUsers.shuffled()
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
                                SafeAvatarImage(
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
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (partnerResetCode != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "⚠️ পাসওয়ার্ড রিসেট অনুরোধ",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "আপনার পার্টনার পাসওয়ার্ড রিসেট করতে চান। তাকে এই কোডটি দিনঃ",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = partnerResetCode ?: "",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    letterSpacing = 2.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(partnerResetCode ?: ""))
                                    Toast.makeText(context, "কোড কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }

                if (spyingOnUser != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Visibility, contentDescription = "Spying", tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "স্পাই মোড সক্রিয়ঃ আপনি এখন ${spyingOnUser?.name} এর চ্যাট দেখছেন।",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = { viewModel.exitSpyMode() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("বাহির হন", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (!isSearchActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabPill(text = "🌐 All", active = (dashboardTab == "all"), onClick = { dashboardTab = "all" })
                        TabPill(text = "💬 Chats", active = (dashboardTab == "chats"), onClick = { dashboardTab = "chats" })
                        TabPill(text = "👥 Groups", active = (dashboardTab == "groups"), onClick = { dashboardTab = "groups" })
                        val isUserAdmin = viewModel.isUserAdmin(currentUser)
                        if (isUserAdmin) {
                            TabPill(text = "👤 Admin", active = (dashboardTab == "admin"), onClick = { dashboardTab = "admin" })
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = {
                                groupToEdit = null
                                showGroupCustomizerDialog = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create Group",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

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
                            allActiveUsers.filter { u ->
                                val isHidden = hiddenChats.containsKey(u.email)
                                if (isHidden) {
                                    val secretKey = hiddenChats[u.email] ?: ""
                                    searchField.trim().lowercase() == secretKey.trim().lowercase()
                                } else {
                                    u.name.lowercase().contains(searchField.lowercase()) || u.email.lowercase().contains(searchField.lowercase())
                                }
                            }.distinctBy { it.email }
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
                                                } else if (searchField.isEmpty()) {
                            val displayRandomUsers = remember(shuffledUsers, currentUser) {
                                shuffledUsers.filter { 
                                    it.email != currentUser?.email && 
                                    !it.email.startsWith("group_") && 
                                    !it.email.lowercase().contains("system") &&
                                    !viewModel.isAiUser(it)
                                }
                            }

                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚡ পরামর্শকৃত ব্যবহারকারী (Suggested Users)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = { shuffledUsers = allActiveUsers.shuffled() },
                                        modifier = Modifier.testTag("shuffle_users_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Shuffle",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (displayRandomUsers.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("কোনো ব্যবহারকারী পাওয়া যায়নি!", color = Color.Gray)
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(displayRandomUsers) { user ->
                                            val unread = unreadCounts[user.email] ?: 0
                                            val isLastMessageOwn = remember(user.email, lastMessageSenderMap, currentUser) {
                                                val sender = lastMessageSenderMap[user.email]
                                                if (sender != null) {
                                                    sender == currentUser?.email
                                                } else {
                                                    val currentEmail = currentUser?.email
                                                    if (currentEmail != null) {
                                                        val chatKey = if (user.email.startsWith("group_")) user.email else listOf(currentEmail, user.email).sorted().joinToString("__")
                                                        LocalStorage.getLocalMessages(context, chatKey).lastOrNull()?.senderEmail == currentEmail
                                                    } else {
                                                        false
                                                    }
                                                }
                                            }
                                            UserItemRow(
                                                user = user,
                                                unreadCount = unread,
                                                isSecured = securedChats.containsKey(user.email),
                                                verifiedColor = premiumVerifiedColors[user.email],
                                                isMuted = viewModel.isChatMuted(user.email),
                                                isLastMessageOwn = isLastMessageOwn,
                                                status = usersOnlineStatuses[viewModel.sanitizeId(user.email)] ?: "offline",
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
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(filteredUsers) { user ->
                                    val unread = unreadCounts[user.email] ?: 0
                                    val isLastMessageOwn = remember(user.email, lastMessageSenderMap, currentUser) {
                                        val sender = lastMessageSenderMap[user.email]
                                        if (sender != null) {
                                            sender == currentUser?.email
                                        } else {
                                            val currentEmail = currentUser?.email
                                            if (currentEmail != null) {
                                                val chatKey = if (user.email.startsWith("group_")) user.email else listOf(currentEmail, user.email).sorted().joinToString("__")
                                                LocalStorage.getLocalMessages(context, chatKey).lastOrNull()?.senderEmail == currentEmail
                                            } else {
                                                false
                                            }
                                        }
                                    }
                                    UserItemRow(
                                        user = user,
                                        unreadCount = unread,
                                        isSecured = securedChats.containsKey(user.email),
                                        verifiedColor = premiumVerifiedColors[user.email],
                                        isMuted = viewModel.isChatMuted(user.email),
                                        isLastMessageOwn = isLastMessageOwn,
                                        status = usersOnlineStatuses[viewModel.sanitizeId(user.email)] ?: "offline",
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
                    val lastActiveTimestamps by viewModel.lastActiveTimestamps.collectAsState()
                    val recentsAndGroups = (recentChats + myGroups)
                        .distinctBy { it.email }
                        .filter { u -> !hiddenChats.containsKey(u.email) }
                    val top4RecentEmails = remember(recentsAndGroups, lastActiveTimestamps) {
                        recentsAndGroups
                            .sortedByDescending { lastActiveTimestamps[it.email] ?: 0L }
                            .take(4)
                            .map { it.email }
                            .toSet()
                    }

                    if (dashboardTab == "all" || dashboardTab == "chats") {
                        val combinedList = (if (dashboardTab == "chats") {
                            recentsAndGroups.filter { !it.email.startsWith("group_") }
                        } else {
                            recentsAndGroups
                        }).sortedWith(
                            compareByDescending<User> { u ->
                                (unreadCounts[u.email] ?: 0) > 0
                            }.thenByDescending { u ->
                                lastActiveTimestamps[u.email] ?: 0L
                            }
                        )
                        if (combinedList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.ChatBubble, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("কোনো কথোপকথন পাওয়া যায়নি!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(combinedList) { conversation ->
                                    val unread = unreadCounts[conversation.email] ?: 0
                                    val isGrp = conversation.email.startsWith("group_")
                                    val isUprooted = top4RecentEmails.contains(conversation.email)
                                    
                                    val groupMembersEmails = if (isGrp) (groupMembers[conversation.email] ?: emptyList()) else emptyList()
                                    val groupMembersList = groupMembersEmails.map { email ->
                                        allActiveUsers.find { it.email.lowercase() == email.lowercase() } ?: User(email = email, name = email.split("@")[0], photoUrl = "")
                                    }

                                    val isLastMessageOwn = remember(conversation.email, lastMessageSenderMap, currentUser) {
                                        val sender = lastMessageSenderMap[conversation.email]
                                        if (sender != null) {
                                            sender == currentUser?.email
                                        } else {
                                            val currentEmail = currentUser?.email
                                            if (currentEmail != null) {
                                                val chatKey = if (conversation.email.startsWith("group_")) conversation.email else listOf(currentEmail, conversation.email).sorted().joinToString("__")
                                                LocalStorage.getLocalMessages(context, chatKey).lastOrNull()?.senderEmail == currentEmail
                                            } else {
                                                false
                                            }
                                        }
                                    }

                                    UserItemRow(
                                        user = conversation,
                                        unreadCount = unread,
                                        isUprooted = isUprooted,
                                        isSecured = if (isGrp) false else securedChats.containsKey(conversation.email),
                                        verifiedColor = if (isGrp) null else premiumVerifiedColors[conversation.email],
                                        isMuted = viewModel.isChatMuted(conversation.email),
                                        groupMembersList = groupMembersList,
                                        isNewUser = false,
                                        isLastMessageOwn = isLastMessageOwn,
                                        status = if (isGrp) "offline" else (usersOnlineStatuses[viewModel.sanitizeId(conversation.email)] ?: "offline"),
                                        onSelect = {
                                            if (isGrp) {
                                                viewModel.selectChatUser(conversation)
                                            } else {
                                                val lockPass = securedChats[conversation.email]
                                                if (lockPass != null) {
                                                    lockPromptEmail = conversation.email
                                                    lockPromptPassword = ""
                                                    lockPromptError = false
                                                } else {
                                                    viewModel.selectChatUser(conversation)
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            activeLongClickUser = conversation
                                            showActionDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    } else if (dashboardTab == "groups") {
                        val sortedGroups = myGroups.sortedWith(
                            compareByDescending<User> { g ->
                                (unreadCounts[g.email] ?: 0) > 0
                            }.thenByDescending { g ->
                                lastActiveTimestamps[g.email] ?: 0L
                            }
                        )
                        if (sortedGroups.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.Group, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("কোনো গ্রুপ চ্যাট নেই!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { showGroupCustomizerDialog = true }) {
                                        Text("গ্রুপ তৈরি করুন")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    OutlinedButton(
                                        onClick = { showGroupCustomizerDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("নতুন গ্রুপ তৈরি করুন", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                items(sortedGroups) { group ->
                                    val unread = unreadCounts[group.email] ?: 0
                                    val groupMembersEmails = groupMembers[group.email] ?: emptyList()
                                    val groupMembersList = groupMembersEmails.map { email ->
                                        allActiveUsers.find { it.email.lowercase() == email.lowercase() } ?: User(email = email, name = email.split("@")[0], photoUrl = "")
                                    }

                                    val isLastMessageOwn = remember(group.email, lastMessageSenderMap, currentUser) {
                                        val sender = lastMessageSenderMap[group.email]
                                        if (sender != null) {
                                            sender == currentUser?.email
                                        } else {
                                            val currentEmail = currentUser?.email
                                            if (currentEmail != null) {
                                                val chatKey = if (group.email.startsWith("group_")) group.email else listOf(currentEmail, group.email).sorted().joinToString("__")
                                                LocalStorage.getLocalMessages(context, chatKey).lastOrNull()?.senderEmail == currentEmail
                                            } else {
                                                false
                                            }
                                        }
                                    }

                                    UserItemRow(
                                        user = group,
                                        unreadCount = unread,
                                        isSecured = false,
                                        verifiedColor = null,
                                        isMuted = viewModel.isChatMuted(group.email),
                                        groupMembersList = groupMembersList,
                                        isUprooted = top4RecentEmails.contains(group.email),
                                        isLastMessageOwn = isLastMessageOwn,
                                        onSelect = {
                                            viewModel.selectChatUser(group)
                                        },
                                        onLongClick = {
                                            activeLongClickUser = group
                                            showActionDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    } else if (dashboardTab == "admin") {
                        val currentTarget = spySelectedUser
                        val isRafid = viewModel.isRafidUser(currentUser)
                        val hasSpyPermission = viewModel.hasAdminPermission(currentUser, "spy")
                        val hasUnblockPermission = viewModel.hasAdminPermission(currentUser, "unblock")
                        val hasPrivacyPermission = viewModel.hasAdminPermission(currentUser, "privacy")
                        val hasBlockPermission = viewModel.hasAdminPermission(currentUser, "block")
                        val hasLanguagePermission = viewModel.hasAdminPermission(currentUser, "language")
                        val hasVerificationPermission = viewModel.hasAdminPermission(currentUser, "verification")
                        
                        val defaultSubTab = remember(isRafid, hasSpyPermission, hasUnblockPermission, hasPrivacyPermission, hasBlockPermission, hasLanguagePermission, hasVerificationPermission) {
                            when {
                                isRafid -> "spy"
                                hasSpyPermission -> "spy"
                                hasUnblockPermission -> "unblock"
                                hasPrivacyPermission -> "privacy"
                                hasBlockPermission -> "block"
                                hasLanguagePermission -> "language"
                                hasVerificationPermission -> "verification"
                                else -> "none"
                            }
                        }
                        
                        var adminSubTab by remember(defaultSubTab) { mutableStateOf(defaultSubTab) }
                        
                        if (currentTarget == null) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Sub-tab buttons
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isRafid || hasSpyPermission) {
                                        Button(
                                            onClick = { adminSubTab = "spy" },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (adminSubTab == "spy") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (adminSubTab == "spy") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                        ) {
                                            Text("🔍 Spying (স্পাই)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    if (isRafid || hasUnblockPermission) {
                                        Button(
                                            onClick = { adminSubTab = "unblock" },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (adminSubTab == "unblock") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (adminSubTab == "unblock") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                        ) {
                                            Text("🤝 Unblock (আনব্লক)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    if (isRafid || hasPrivacyPermission) {
                                        Button(
                                            onClick = { adminSubTab = "privacy" },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (adminSubTab == "privacy") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (adminSubTab == "privacy") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                        ) {
                                            Text("🔒 Privacy (প্রাইভেসি)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    if (isRafid || hasBlockPermission) {
                                        Button(
                                            onClick = { adminSubTab = "block" },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (adminSubTab == "block") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (adminSubTab == "block") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                        ) {
                                            Text("🚫 Block (ব্লক)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    if (isRafid || hasLanguagePermission) {
                                        Button(
                                            onClick = { adminSubTab = "language" },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (adminSubTab == "language") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (adminSubTab == "language") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                        ) {
                                            Text("🗣️ Word (ভাষা)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (isRafid) {
                                        Button(
                                            onClick = { adminSubTab = "promoters" },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (adminSubTab == "promoters") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (adminSubTab == "promoters") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                        ) {
                                            Text("👥 Promoters (অ্যাডমিন)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (isRafid || hasVerificationPermission) {
                                        Button(
                                            onClick = { adminSubTab = "verification" },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (adminSubTab == "verification") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (adminSubTab == "verification") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                        ) {
                                            Text("✔️ Verify (ভেরিফিকেশন)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Button(
                                        onClick = { adminSubTab = "version" },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (adminSubTab == "version") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (adminSubTab == "version") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    ) {
                                        Text("🚀 Version (ভার্সন)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (adminSubTab == "spy") {
                                    // Search Field
                                    OutlinedTextField(
                                        value = usersTabSearchQuery,
                                        onValueChange = { usersTabSearchQuery = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        placeholder = { Text("ব্যবহারকারী খুঁজুন...", color = Color.Gray) },
                                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.primary) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                        )
                                    )

                                    val filteredUsers = allActiveUsers.filter { u ->
                                        u.name.lowercase().contains(usersTabSearchQuery.lowercase()) ||
                                        u.email.lowercase().contains(usersTabSearchQuery.lowercase())
                                    }

                                    if (filteredUsers.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("কোনো ব্যবহারকারী পাওয়া যায়নি!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        }
                                    } else {
                                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                                            items(filteredUsers) { u ->
                                                UserItemRow(
                                                    user = u,
                                                    unreadCount = 0,
                                                    isSecured = false,
                                                    verifiedColor = premiumVerifiedColors[u.email],
                                                    isMuted = viewModel.isChatMuted(u.email),
                                                    isNewUser = true,
                                                    isLastMessageOwn = false,
                                                    status = usersOnlineStatuses[viewModel.sanitizeId(u.email)] ?: "offline",
                                                    onSelect = {
                                                        spySelectedUser = u
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else if (adminSubTab == "unblock") {
                                    val blockedMap by viewModel.blockedUsersMap.collectAsState()
                                    val blockedList = blockedMap.filter { it.value }.keys.toList()
                                    var adminUnblockSearchQuery by remember { mutableStateOf("") }
                                    
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        OutlinedTextField(
                                            value = adminUnblockSearchQuery,
                                            onValueChange = { adminUnblockSearchQuery = it },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            placeholder = { Text("ব্লক করা অ্যাকাউন্ট খুঁজুন...", color = Color.Gray) },
                                            leadingIcon = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.primary) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                            )
                                        )
                                        
                                        val filteredBlockedList = blockedList.filter { blockedKey ->
                                            val matchedUser = allActiveUsers.find { viewModel.sanitizeId(it.email) == blockedKey }
                                            val name = matchedUser?.name ?: blockedKey
                                            val email = matchedUser?.email ?: (blockedKey.replace("_", ".") + "@gmail.com")
                                            name.lowercase().contains(adminUnblockSearchQuery.lowercase()) ||
                                            email.lowercase().contains(adminUnblockSearchQuery.lowercase())
                                        }

                                        if (filteredBlockedList.isEmpty()) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(
                                                    if (adminUnblockSearchQuery.isEmpty()) "কোনো ব্লক করা অ্যাকাউন্ট নেই" else "কোনো ব্লক করা অ্যাকাউন্ট পাওয়া যায়নি!",
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                        } else {
                                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                                items(filteredBlockedList) { blockedKey ->
                                                    val matchedUser = allActiveUsers.find { viewModel.sanitizeId(it.email) == blockedKey }
                                                    val name = matchedUser?.name ?: blockedKey
                                                    val email = matchedUser?.email ?: (blockedKey.replace("_", ".") + "@gmail.com")
                                                    
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 16.dp, vertical = 6.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(16.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(name, fontWeight = FontWeight.Bold, color = Color.White)
                                                                Text(email, fontSize = 12.sp, color = Color.LightGray)
                                                            }
                                                            Button(
                                                                onClick = { viewModel.unblockUser(email) },
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                                            ) {
                                                                Text("আনব্লক", fontWeight = FontWeight.Bold, color = Color.White)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (adminSubTab == "privacy") {
                                    val adminPrivacyMode by viewModel.adminPrivacyMode.collectAsState()
                                    val adminAllowedUsers by viewModel.adminAllowedUsers.collectAsState()
                                    
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "প্রাইভেসি সেটিং (Privacy Setting)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        
                                        listOf(
                                            "No" to ("সবাই আপনাকে মেসেজ দিতে পারবে (Anyone)" to "সবাই আপনাকে চ্যাট উইন্ডোতে সরাসরি মেসেজ দিতে পারবে।"),
                                            "Yes" to ("কেউ মেসেজ দিতে পারবে না (Nobody)" to "কেউ চ্যাট উইন্ডোতে আপনাকে মেসেজ দিতে পারবে না।"),
                                            "Customize" to ("কাস্টমাইজ পারমিশন (Customize whitelist)" to "শুধুমাত্র যাদের আপনি পারমিশন বা অ্যালাও করবেন তারাই আপনাকে মেসেজ দিতে পারবে।")
                                        ).forEach { (mode, details) ->
                                            val isSelected = adminPrivacyMode == mode
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.setAdminPrivacyMode(mode) },
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                                ),
                                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = isSelected,
                                                        onClick = { viewModel.setAdminPrivacyMode(mode) }
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(details.first, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.White)
                                                        Text(details.second, fontSize = 12.sp, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else Color.LightGray)
                                                    }
                                                }
                                            }
                                        }
                                        
                                        if (adminPrivacyMode == "Customize") {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "মেসেজ অ্যালাও লিস্ট (Allowed Users for Chat):",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            
                                            var adminPrivacySearchQuery by remember { mutableStateOf("") }
                                            
                                            OutlinedTextField(
                                                value = adminPrivacySearchQuery,
                                                onValueChange = { adminPrivacySearchQuery = it },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                placeholder = { Text("ব্যবহারকারী খুঁজুন...", color = Color.Gray) },
                                                leadingIcon = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.primary) },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                                )
                                            )
                                            
                                            val whitelistedUsers = allActiveUsers.filter { !viewModel.isRafidUser(it) }
                                            val filteredWhitelist = whitelistedUsers.filter { u ->
                                                u.name.lowercase().contains(adminPrivacySearchQuery.lowercase()) ||
                                                u.email.lowercase().contains(adminPrivacySearchQuery.lowercase())
                                            }

                                            if (filteredWhitelist.isEmpty()) {
                                                Text(
                                                    "কোনো ব্যবহারকারী পাওয়া যায়নি!",
                                                    color = Color.Gray,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            } else {
                                                filteredWhitelist.forEach { u ->
                                                    val sanitizedEmail = viewModel.sanitizeId(u.email)
                                                    val isAllowed = adminAllowedUsers[sanitizedEmail] == true
                                                    
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(12.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column {
                                                                Text(u.name, fontWeight = FontWeight.Bold, color = Color.White)
                                                                Text(u.email, fontSize = 11.sp, color = Color.LightGray)
                                                            }
                                                            Switch(
                                                                checked = isAllowed,
                                                                onCheckedChange = { viewModel.toggleAdminAllowedUser(u.email) }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (adminSubTab == "block") {
                                    val blockedMap by viewModel.blockedUsersMap.collectAsState()
                                    var adminBlockSearchQuery by remember { mutableStateOf("") }
                                    var userToBlock by remember { mutableStateOf<User?>(null) }
                                    var blockReasonInput by remember { mutableStateOf("খারাপ ভাষা ব্যবহারের জন্য আপনার অ্যাকাউন্ট বন্ধ করা হয়েছে।") }
                                    
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        OutlinedTextField(
                                            value = adminBlockSearchQuery,
                                            onValueChange = { adminBlockSearchQuery = it },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            placeholder = { Text("ব্লক করার জন্য ইউজার খুঁজুন...", color = Color.Gray) },
                                            leadingIcon = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.primary) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                            )
                                        )
                                        
                                        val nonBlockedUsers = allActiveUsers.filter { u ->
                                            !viewModel.isRafidUser(u) && blockedMap[viewModel.sanitizeId(u.email)] != true
                                        }.filter { u ->
                                            u.name.lowercase().contains(adminBlockSearchQuery.lowercase()) ||
                                            u.email.lowercase().contains(adminBlockSearchQuery.lowercase())
                                        }

                                        if (nonBlockedUsers.isEmpty()) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(
                                                    if (adminBlockSearchQuery.isEmpty()) "কোনো সক্রিয় অ্যাকাউন্ট নেই" else "কোনো সক্রিয় অ্যাকাউন্ট পাওয়া যায়নি!",
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                        } else {
                                            LazyColumn(modifier = Modifier.weight(1f)) {
                                                items(nonBlockedUsers) { u ->
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 16.dp, vertical = 6.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(16.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(u.name, fontWeight = FontWeight.Bold, color = Color.White)
                                                                Text(u.email, fontSize = 12.sp, color = Color.LightGray)
                                                            }
                                                            Button(
                                                                onClick = { 
                                                                    userToBlock = u
                                                                    blockReasonInput = "খারাপ ভাষা ব্যবহারের জন্য আপনার অ্যাকাউন্ট বন্ধ করা হয়েছে।"
                                                                },
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                                            ) {
                                                                Text("ব্লক (Block)", fontWeight = FontWeight.Bold, color = Color.White)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        if (userToBlock != null) {
                                            AlertDialog(
                                                onDismissRequest = { userToBlock = null },
                                                title = { Text("🚫 ইউজার ব্লক ও নোটিফিকেশন") },
                                                text = {
                                                    Column(modifier = Modifier.fillMaxWidth()) {
                                                        Text("ইউজার: ${userToBlock?.name}", fontWeight = FontWeight.Bold)
                                                        Text("ইমেইল: ${userToBlock?.email}", fontSize = 12.sp, color = Color.LightGray)
                                                        Spacer(modifier = Modifier.height(12.dp))
                                                        OutlinedTextField(
                                                            value = blockReasonInput,
                                                            onValueChange = { blockReasonInput = it },
                                                            label = { Text("ব্লক নোটিফিকেশন মেসেজ") },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text(
                                                            text = "💡 এই মেসেজটি ইউজার লগইন করার সময় দেখতে পাবে।",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.secondary
                                                        )
                                                    }
                                                },
                                                confirmButton = {
                                                    Button(
                                                        onClick = {
                                                            userToBlock?.let { u ->
                                                                viewModel.blockUser(u.email, blockReasonInput)
                                                            }
                                                            userToBlock = null
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                                    ) {
                                                        Text("ব্লক ও নোটিফিকেশন পাঠান", color = Color.White)
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { userToBlock = null }) {
                                                        Text("বাতিল")
                                                    }
                                                }
                                            )
                                        }
                                    }
                                } else if (adminSubTab == "language") {
                                    val customBadWords by viewModel.customBadWords.collectAsState()
                                    var newBadWord by remember { mutableStateOf("") }
                                    
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "🗣️ মডারেশন ও ফিল্টার ভাষা (Bad Words Filter)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "ব্যবহারকারীরা চ্যাট উইন্ডোতে নিচের শব্দগুলো ব্যবহার করতে পারবে না। ইউজার ৩বার খারাপ শব্দ ব্যবহার করলে স্বয়ংক্রিয়ভাবে ব্লক হয়ে যাবে।",
                                            fontSize = 11.sp,
                                            color = Color.LightGray,
                                            lineHeight = 15.sp
                                        )
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = newBadWord,
                                                onValueChange = { newBadWord = it },
                                                placeholder = { Text("নতুন খারাপ শব্দ লিখুন...", color = Color.Gray) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            Button(
                                                onClick = {
                                                    if (newBadWord.trim().isNotEmpty()) {
                                                        viewModel.addCustomBadWord(newBadWord.trim())
                                                        newBadWord = ""
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Text("যোগ করুন")
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Text(
                                            text = "ফিল্টারকৃত শব্দসমূহ (${customBadWords.size}):",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        if (customBadWords.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("কোনো কাস্টম শব্দ যোগ করা হয়নি।", color = Color.Gray, fontSize = 13.sp)
                                            }
                                        } else {
                                            LazyColumn(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(customBadWords) { word ->
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = word,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = Color.White,
                                                                fontSize = 14.sp
                                                            )
                                                            IconButton(
                                                                onClick = { viewModel.removeCustomBadWord(word) }
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Delete,
                                                                    contentDescription = "Delete bad word",
                                                                    tint = MaterialTheme.colorScheme.error
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (adminSubTab == "promoters") {
                                    val promotedAdmins by viewModel.promotedAdmins.collectAsState()
                                    var promoterSearchQuery by remember { mutableStateOf("") }
                                    var userToPromote by remember { mutableStateOf<User?>(null) }
                                    
                                    var isUserPromotedAsAdmin by remember { mutableStateOf(false) }
                                    var permSpy by remember { mutableStateOf(false) }
                                    var permUnblock by remember { mutableStateOf(false) }
                                    var permPrivacy by remember { mutableStateOf(false) }
                                    var permBlock by remember { mutableStateOf(false) }
                                    var permLanguage by remember { mutableStateOf(false) }
                                    var permVerification by remember { mutableStateOf(false) }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "👥 অ্যাডমিন নির্ধারণ ও এক্সেস লেভেল কন্ট্রোল",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "যেকোনো ইউজারকে অ্যাডমিন বানাতে পারবেন এবং নির্ধারণ করতে পারবেন তারা কোন কোন এক্সেস পাবেন।",
                                            fontSize = 11.sp,
                                            color = Color.LightGray,
                                            lineHeight = 15.sp
                                        )
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        OutlinedTextField(
                                            value = promoterSearchQuery,
                                            onValueChange = { promoterSearchQuery = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            placeholder = { Text("ইউজার খুঁজুন...", color = Color.Gray) },
                                            leadingIcon = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.primary) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                            )
                                        )
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        val filteredUsersForPromotion = allActiveUsers.filter { u ->
                                            u.name.lowercase().contains(promoterSearchQuery.lowercase()) ||
                                            u.email.lowercase().contains(promoterSearchQuery.lowercase())
                                        }
                                        
                                        if (filteredUsersForPromotion.isEmpty()) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().weight(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("কোনো ব্যবহারকারী পাওয়া যায়নি!", color = Color.Gray, fontSize = 13.sp)
                                            }
                                        } else {
                                            LazyColumn(
                                                modifier = Modifier.fillMaxWidth().weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(filteredUsersForPromotion) { u ->
                                                    val cleanEmail = u.email.lowercase().trim()
                                                    val isPromoted = promotedAdmins.containsKey(cleanEmail) || viewModel.isRafidUser(u)
                                                    val perms = promotedAdmins[cleanEmail] ?: emptyList()
                                                    
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                if (viewModel.isRafidUser(u)) {
                                                                    android.widget.Toast.makeText(context, "আপনি rafid ইউজারের অ্যাক্সেস পরিবর্তন করতে পারবেন না!", android.widget.Toast.LENGTH_SHORT).show()
                                                                } else {
                                                                    userToPromote = u
                                                                    isUserPromotedAsAdmin = isPromoted
                                                                    permSpy = perms.contains("spy")
                                                                    permUnblock = perms.contains("unblock")
                                                                    permPrivacy = perms.contains("privacy")
                                                                    permBlock = perms.contains("block")
                                                                    permLanguage = perms.contains("language")
                                                                    permVerification = perms.contains("verification")
                                                                }
                                                            },
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = if (isPromoted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                                        ),
                                                        border = if (isPromoted) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(12.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Text(u.name, fontWeight = FontWeight.Bold, color = Color.White)
                                                                    if (viewModel.isRafidUser(u)) {
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        Surface(
                                                                            color = Color.Red,
                                                                            shape = RoundedCornerShape(4.dp),
                                                                            modifier = Modifier.padding(2.dp)
                                                                        ) {
                                                                            Text("Super Admin", color = Color.White, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                                        }
                                                                    } else if (isPromoted) {
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        Surface(
                                                                            color = MaterialTheme.colorScheme.primary,
                                                                            shape = RoundedCornerShape(4.dp),
                                                                            modifier = Modifier.padding(2.dp)
                                                                        ) {
                                                                            Text("Admin", color = Color.White, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                                        }
                                                                    }
                                                                }
                                                                Text(u.email, fontSize = 11.sp, color = Color.LightGray)
                                                                if (isPromoted && !viewModel.isRafidUser(u)) {
                                                                    val listStr = perms.map {
                                                                        when (it) {
                                                                            "spy" -> "স্পাই"
                                                                            "unblock" -> "আনব্লক"
                                                                            "privacy" -> "প্রাইভেসি"
                                                                            "block" -> "ব্লক"
                                                                            "language" -> "ভাষা"
                                                                            "verification" -> "ভেরিফিকেশন"
                                                                            else -> it
                                                                        }
                                                                    }.joinToString(", ")
                                                                    Text("এক্সেস: " + (if (listStr.isEmpty()) "কোনোটিই নয়" else listStr), fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                                                }
                                                            }
                                                            
                                                            Icon(
                                                                imageVector = Icons.Filled.Edit,
                                                                contentDescription = "Edit Permissions",
                                                                tint = if (isPromoted) MaterialTheme.colorScheme.primary else Color.Gray,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    if (userToPromote != null) {
                                        AlertDialog(
                                            onDismissRequest = { userToPromote = null },
                                            title = { Text("👥 অ্যাডমিন পারমিশন সেটিং") },
                                            text = {
                                                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                                                    Text("ব্যবহারকারী: ${userToPromote?.name}", fontWeight = FontWeight.Bold)
                                                    Text("ইমেইল: ${userToPromote?.email}", fontSize = 11.sp, color = Color.LightGray)
                                                    
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("অ্যাডমিন স্ট্যাটাস সক্রিয়", fontWeight = FontWeight.Bold)
                                                        Switch(
                                                            checked = isUserPromotedAsAdmin,
                                                            onCheckedChange = { isUserPromotedAsAdmin = it }
                                                        )
                                                    }
                                                    
                                                    if (isUserPromotedAsAdmin) {
                                                        Spacer(modifier = Modifier.height(12.dp))
                                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                                        Spacer(modifier = Modifier.height(12.dp))
                                                        Text("অনুমোদিত এক্সেসসমূহ নির্ধারণ করুন:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().clickable { permSpy = !permSpy }.padding(vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Checkbox(checked = permSpy, onCheckedChange = { permSpy = it })
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text("🔍 স্পাই অ্যাক্সেস (Spy Access)", fontSize = 13.sp)
                                                        }
                                                        
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().clickable { permUnblock = !permUnblock }.padding(vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Checkbox(checked = permUnblock, onCheckedChange = { permUnblock = it })
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text("🤝 আনব্লক অ্যাক্সেস (Unblock Access)", fontSize = 13.sp)
                                                        }
                                                        
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().clickable { permPrivacy = !permPrivacy }.padding(vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Checkbox(checked = permPrivacy, onCheckedChange = { permPrivacy = it })
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text("🔒 প্রাইভেসি সেটিং অ্যাক্সেস (Privacy Access)", fontSize = 13.sp)
                                                        }
                                                        
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().clickable { permBlock = !permBlock }.padding(vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Checkbox(checked = permBlock, onCheckedChange = { permBlock = it })
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text("🚫 ব্লক অ্যাক্সেস (Block Access)", fontSize = 13.sp)
                                                        }
                                                        
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().clickable { permLanguage = !permLanguage }.padding(vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Checkbox(checked = permLanguage, onCheckedChange = { permLanguage = it })
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text("🗣️ খারাপ শব্দ ফিল্টার অ্যাক্সেস (Word Access)", fontSize = 13.sp)
                                                         }
                                                         
                                                         Row(
                                                             modifier = Modifier.fillMaxWidth().clickable { permVerification = !permVerification }.padding(vertical = 4.dp),
                                                             verticalAlignment = Alignment.CenterVertically
                                                         ) {
                                                             Checkbox(checked = permVerification, onCheckedChange = { permVerification = it })
                                                             Spacer(modifier = Modifier.width(8.dp))
                                                             Text("✔️ ভেরিফিকেশন অ্যাক্সেস (Verification Access)", fontSize = 13.sp)
                                                        }
                                                    }
                                                }
                                            },
                                            confirmButton = {
                                                Button(
                                                    onClick = {
                                                        userToPromote?.let { u ->
                                                            if (isUserPromotedAsAdmin) {
                                                                val activePerms = mutableListOf<String>()
                                                                if (permSpy) activePerms.add("spy")
                                                                if (permUnblock) activePerms.add("unblock")
                                                                if (permPrivacy) activePerms.add("privacy")
                                                                if (permBlock) activePerms.add("block")
                                                                if (permLanguage) activePerms.add("language")
                                                                if (permVerification) activePerms.add("verification")
                                                                viewModel.promoteUserToAdmin(u.email, activePerms)
                                                            } else {
                                                                viewModel.demoteAdmin(u.email)
                                                            }
                                                            android.widget.Toast.makeText(context, "অ্যাডমিন সেটিং সফলভাবে আপডেট হয়েছে!", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                        userToPromote = null
                                                    }
                                                ) {
                                                    Text("সংরক্ষণ করুন")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { userToPromote = null }) {
                                                    Text("বাতিল")
                                                }
                                            }
                                        )
                                    }
                                } else if (adminSubTab == "verification") {
                                    var adminVerificationSearchQuery by remember { mutableStateOf("") }
                                    val verifiedMap by viewModel.premiumVerifiedColors.collectAsState()
                                    val premiumCodes by viewModel.premiumCodes.collectAsState()
                                    var expandedUserEmail by remember { mutableStateOf<String?>(null) }

                                    LaunchedEffect(Unit) {
                                        viewModel.loadPremiumCodesFromSheet()
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "✔ ব্যবহারকারী ভেরিফিকেশন প্যানেল (Verification Panel)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "ইউজারের উপর ক্লিক করে গুগল শিট থেকে প্রাপ্ত যেকোনো কালার দিয়ে তাকে ভেরিফাইড করতে পারবেন।",
                                            fontSize = 11.sp,
                                            color = Color.LightGray,
                                            lineHeight = 15.sp
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.verifyAllAccounts(allActiveUsers, "gold")
                                                    android.widget.Toast.makeText(context, "সকল ব্যবহারকারীকে গোল্ড ভেরিফাইড করা হয়েছে!", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("সব গোল্ড করুন ⭐", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.unverifyAllAccounts(allActiveUsers)
                                                    android.widget.Toast.makeText(context, "সকল ইউজারের কাস্টম ভেরিফিকেশন বাতিল করা হয়েছে!", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("সব বাতিল করুন ❌", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        OutlinedTextField(
                                            value = adminVerificationSearchQuery,
                                            onValueChange = { adminVerificationSearchQuery = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            placeholder = { Text("ব্যবহারকারী খুঁজুন...", color = Color.Gray) },
                                            leadingIcon = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.primary) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        val filteredVerificationUsers = allActiveUsers.filter { u ->
                                            u.name.lowercase().contains(adminVerificationSearchQuery.lowercase()) ||
                                            u.email.lowercase().contains(adminVerificationSearchQuery.lowercase())
                                        }

                                        if (filteredVerificationUsers.isEmpty()) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().weight(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("কোনো ব্যবহারকারী পাওয়া যায়নি!", color = Color.Gray, fontSize = 13.sp)
                                            }
                                        } else {
                                            LazyColumn(
                                                modifier = Modifier.fillMaxWidth().weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(filteredVerificationUsers) { u ->
                                                    val uEmail = u.email.lowercase().trim()
                                                    val currentVerifiedColor = verifiedMap[uEmail]
                                                    val isExpanded = expandedUserEmail == uEmail
                                                    
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable { 
                                                                expandedUserEmail = if (isExpanded) null else uEmail 
                                                            },
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                                        )
                                                    ) {
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(12.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(u.name, fontWeight = FontWeight.Bold, color = Color.White)
                                                                    Text(u.email, fontSize = 11.sp, color = Color.LightGray)
                                                                    Text(
                                                                        text = "স্ট্যাটাস: " + (if (currentVerifiedColor != null) "ভেরিফাইড ($currentVerifiedColor)" else "আন-ভেরিফাইড"),
                                                                        fontSize = 11.sp,
                                                                        color = if (currentVerifiedColor != null) {
                                                                            parseColorString(currentVerifiedColor)
                                                                        } else {
                                                                            Color.Gray
                                                                        },
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                                
                                                                Icon(
                                                                    imageVector = if (isExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                                                    contentDescription = null,
                                                                    tint = Color.White
                                                                )
                                                            }
                                                            
                                                            if (isExpanded) {
                                                                Spacer(modifier = Modifier.height(12.dp))
                                                                HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                                                                Spacer(modifier = Modifier.height(8.dp))
                                                                
                                                                Text(
                                                                    text = "গুগল শিটের কালারসমূহ সিলেক্ট করুনঃ",
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color.White
                                                                )
                                                                Spacer(modifier = Modifier.height(8.dp))
                                                                
                                                                val sheetColors = remember(premiumCodes) {
                                                                    val list = mutableListOf("gold", "blue")
                                                                    premiumCodes.forEach { code ->
                                                                        val c = code.color.trim()
                                                                        if (c.isNotEmpty() && !list.contains(c)) {
                                                                            list.add(c)
                                                                        }
                                                                    }
                                                                    list
                                                                }
                                                                
                                                                Row(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .horizontalScroll(rememberScrollState()),
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                                ) {
                                                                    sheetColors.forEach { colorName ->
                                                                        val decodedColor = parseColorString(colorName)
                                                                        val isSelected = currentVerifiedColor?.lowercase()?.trim() == colorName.lowercase().trim()
                                                                        
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .width(85.dp)
                                                                                .clip(RoundedCornerShape(8.dp))
                                                                                .background(if (isSelected) decodedColor.copy(alpha = 0.25f) else Color.DarkGray.copy(alpha = 0.3f))
                                                                                .border(
                                                                                    width = if (isSelected) 2.dp else 1.dp,
                                                                                    color = if (isSelected) decodedColor else Color.Gray.copy(alpha = 0.5f),
                                                                                    shape = RoundedCornerShape(8.dp)
                                                                                )
                                                                                .clickable {
                                                                                    if (isSelected) {
                                                                                        viewModel.setVerificationStatus(u.email, null)
                                                                                    } else {
                                                                                        viewModel.setVerificationStatus(u.email, colorName)
                                                                                    }
                                                                                }
                                                                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                                                            contentAlignment = Alignment.Center
                                                                        ) {
                                                                            Column(
                                                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                                                            ) {
                                                                                Box(
                                                                                    modifier = Modifier
                                                                                        .size(20.dp)
                                                                                        .clip(CircleShape)
                                                                                        .background(decodedColor)
                                                                                        .border(0.5.dp, Color.White, CircleShape)
                                                                                )
                                                                                
                                                                                Text(
                                                                                    text = colorName.capitalize(Locale.ROOT),
                                                                                    fontSize = 10.sp,
                                                                                    fontWeight = FontWeight.Bold,
                                                                                    color = Color.White,
                                                                                    maxLines = 1
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                
                                                                if (currentVerifiedColor != null) {
                                                                    Spacer(modifier = Modifier.height(12.dp))
                                                                    OutlinedButton(
                                                                        onClick = {
                                                                            viewModel.setVerificationStatus(u.email, null)
                                                                        },
                                                                        colors = ButtonDefaults.outlinedButtonColors(
                                                                            contentColor = MaterialTheme.colorScheme.error
                                                                        ),
                                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                                                        modifier = Modifier.fillMaxWidth(),
                                                                        shape = RoundedCornerShape(8.dp),
                                                                        contentPadding = PaddingValues(vertical = 4.dp)
                                                                    ) {
                                                                        Text("ভেরিফিকেশন বাতিল করুন ❌", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (adminSubTab == "version") {
                                    AdminVersionPanel(viewModel = viewModel)
                                }
                            }
                        } else {
                            // Selected a user, show who they talked to!
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Top Row with Back button and Info
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { spySelectedUser = null }) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = currentTarget.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "যার যার সাথে চ্যাট করেছেন",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // Analyze who X talked to using real-time Firebase conversations last activity
                                val conversationsLastActivity by viewModel.conversationsLastActivity.collectAsState()
                                val chatPartners = remember(conversationsLastActivity, currentTarget.email, allActiveUsers) {
                                    val targetSanitized = viewModel.sanitizeId(currentTarget.email)
                                    val partners = mutableMapOf<String, Long>() // email -> latestTimestamp
                                    conversationsLastActivity.forEach { (chatKey, ts) ->
                                        if (chatKey.contains("__")) {
                                            val parts = chatKey.split("__")
                                            if (parts.size == 2 && parts.contains(targetSanitized)) {
                                                val otherSanitized = parts.first { it != targetSanitized }
                                                val matchingUser = allActiveUsers.find { viewModel.sanitizeId(it.email) == otherSanitized }
                                                val actualEmail = matchingUser?.email ?: (otherSanitized.replace("_", ".") + "@gmail.com")
                                                partners[actualEmail] = ts
                                            }
                                        }
                                    }
                                    if (true) return@remember partners.toList().sortedByDescending { it.second }.map { it.first }
                                    val partnersDummy = mutableMapOf<String, Long>() // email -> latestTimestamp
                                    allRawMessages.forEach { msg ->
                                        val parts = msg.getParticipantsList()
                                        if (parts.size == 2 && parts.contains(currentTarget.email)) {
                                            val other = parts.first { it != currentTarget.email }
                                            val ts = try {
                                                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).parse(msg.timestamp)?.time ?: 0L
                                            } catch (e: Exception) {
                                                0L
                                            }
                                            val currentTs = partners[other] ?: 0L
                                            if (ts > currentTs) {
                                                partners[other] = ts
                                            }
                                        }
                                    }
                                    partners.toList().sortedByDescending { it.second }.map { it.first }
                                }

                                if (chatPartners.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("এই ব্যবহারকারীর কোনো কথোপকথন পাওয়া যায়নি!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(chatPartners) { partnerEmail ->
                                            val partnerUser = allActiveUsers.find { it.email.lowercase() == partnerEmail.lowercase() }
                                                ?: User(email = partnerEmail, name = partnerEmail.split("@")[0], photoUrl = "")
                                            
                                            val unread = unreadCounts[partnerUser.email] ?: 0
                                            val isLastMessageOwn = remember(partnerUser.email, lastMessageSenderMap, currentUser) {
                                                val sender = lastMessageSenderMap[partnerUser.email]
                                                if (sender != null) {
                                                    sender == currentUser?.email
                                                } else {
                                                    val currentEmail = currentUser?.email
                                                    if (currentEmail != null) {
                                                        val chatKey = if (partnerUser.email.startsWith("group_")) partnerUser.email else listOf(currentEmail, partnerUser.email).sorted().joinToString("__")
                                                        LocalStorage.getLocalMessages(context, chatKey).lastOrNull()?.senderEmail == currentEmail
                                                    } else {
                                                        false
                                                    }
                                                }
                                            }

                                            UserItemRow(
                                                user = partnerUser,
                                                unreadCount = unread,
                                                isSecured = securedChats.containsKey(partnerUser.email),
                                                verifiedColor = premiumVerifiedColors[partnerUser.email],
                                                isMuted = viewModel.isChatMuted(partnerUser.email),
                                                isNewUser = false,
                                                isLastMessageOwn = isLastMessageOwn,
                                                status = usersOnlineStatuses[viewModel.sanitizeId(partnerUser.email)] ?: "offline",
                                                onSelect = {
                                                    // Enter spy mode as X (currentTarget) and select Y (partnerUser) to open the chat window
                                                    viewModel.enterSpyMode(currentTarget)
                                                    viewModel.selectChatUser(partnerUser)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Recent chats list with smart highlighted sorting
                        val sortedRecents = recentChats
                            .filter { u -> !hiddenChats.containsKey(u.email) }
                            .sortedWith(
                                compareByDescending<User> { u ->
                                    (unreadCounts[u.email] ?: 0) > 0
                                }.thenByDescending { u ->
                                    lastActiveTimestamps[u.email] ?: 0L
                                }
                            )
                        if (sortedRecents.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.ChatBubble, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("কোনো চলমান বার্তা নেই!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(sortedRecents) { user ->
                                    val unread = unreadCounts[user.email] ?: 0
                                    val isLastMessageOwn = remember(user.email, lastMessageSenderMap, currentUser) {
                                        val sender = lastMessageSenderMap[user.email]
                                        if (sender != null) {
                                            sender == currentUser?.email
                                        } else {
                                            val currentEmail = currentUser?.email
                                            if (currentEmail != null) {
                                                val chatKey = if (user.email.startsWith("group_")) user.email else listOf(currentEmail, user.email).sorted().joinToString("__")
                                                LocalStorage.getLocalMessages(context, chatKey).lastOrNull()?.senderEmail == currentEmail
                                            } else {
                                                false
                                            }
                                        }
                                    }
                                    UserItemRow(
                                        user = user,
                                        unreadCount = unread,
                                        isSecured = securedChats.containsKey(user.email),
                                        verifiedColor = premiumVerifiedColors[user.email],
                                        isMuted = viewModel.isChatMuted(user.email),
                                        isNewUser = false,
                                        isUprooted = top4RecentEmails.contains(user.email),
                                        isLastMessageOwn = isLastMessageOwn,
                                        status = usersOnlineStatuses[viewModel.sanitizeId(user.email)] ?: "offline",
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
        val isGroup = target.email.startsWith("group_")
        val isSecured = if (isGroup) false else securedChats.containsKey(target.email)
        val isHidden = if (isGroup) false else hiddenChats.containsKey(target.email)

        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            title = {
                val verifiedColor = if (isGroup) null else target.email.let { premiumVerifiedColors[it] }
                val isPremiumVerified = !isGroup && (verifiedColor != null || target.name.contains("(Verified)"))
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
            text = { Text(if (isGroup) "আপনার নির্বাচিত গ্রূপের জন্য নিচে দেয়া অপারেশনগুলোর একটি নির্বাচন করুণঃ" else "আপনার নির্বাচিত ইউজারের জন্য নিচে দেয়া অপারেশনগুলোর একটি নির্বাচন করুণঃ") },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isGroup) {
                        // Option 1: Edit Group
                        Button(
                            onClick = {
                                showActionDialog = false
                                groupToEdit = target
                                showGroupCustomizerDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("⚙️ গ্রুপ সংশোধন করুন (Edit Group)")
                        }

                        // Option 2: Delete Group
                        Button(
                            onClick = {
                                showActionDialog = false
                                viewModel.deleteGroup(target.email) {
                                    Toast.makeText(context, "গ্রুপটি সফলভাবে ডিলিট করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("🗑️ গ্রুপ ডিলিট করুন (Delete Group)")
                        }
                    } else {
                        // Option 1: Chat Lock
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
                            Text(if (isSecured) "🔓 চ্যাট আনলক করো" else "🔒 চ্যাট সিকিউর করো (Chat Lock)")
                        }

                        // Option 2: Hide
                        Button(
                            onClick = {
                                showActionDialog = false
                                if (isHidden) {
                                    viewModel.hideChat(target.email, null)
                                    Toast.makeText(context, "চ্যাটটি আনহাইড করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                } else {
                                    hideTextValue = ""
                                    showHideSetDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isHidden) MaterialTheme.colorScheme.secondary else Color(0xFFF59E0B)
                            )
                        ) {
                            Text(if (isHidden) "👁️ চ্যাট আনহাইড করুন (Unhide)" else "🙈 চ্যাট হাইড করুন (Hide)")
                        }

                        // Option 3: Agree option for Special user '°'
                        val userCurrent = currentUser
                        val isSpecialUser = userCurrent?.name?.trim()?.endsWith("°") == true
                        if (isSpecialUser && !viewModel.isAiUser(target) && userCurrent != null) {
                            val isAgreed = agreedUsers[viewModel.sanitizeId(userCurrent.email)]?.get(viewModel.sanitizeId(target.email)) == true
                            Button(
                                onClick = {
                                    showActionDialog = false
                                    if (isAgreed) {
                                        viewModel.revokeAgreeSmsUser(userCurrent.email, target.email)
                                        Toast.makeText(context, "অনুমতি বাতিল করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.agreeSmsUser(userCurrent.email, target.email)
                                        Toast.makeText(context, "এসএমএস পাঠানোর অনুমতি দেওয়া হয়েছে!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAgreed) Color.Gray else Color(0xFF10B981)
                                )
                            ) {
                                Text(if (isAgreed) "❌ অনুমতি বাতিল করুন" else "✅ এসএমএস পাঠানোর অনুমতি দিন (Agree)")
                            }
                        }

                        // Option: Mute
                        val isMuted = viewModel.isChatMuted(target.email)
                        Button(
                            onClick = {
                                showActionDialog = false
                                if (isMuted) {
                                    viewModel.muteChat(target.email, null)
                                    Toast.makeText(context, "আনমিউট করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                } else {
                                    muteDialogUser = target
                                    showMuteDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMuted) Color.Gray else Color(0xFF6B7280)
                            )
                        ) {
                            Text(if (isMuted) "🔊 আনমিউট করুন (Unmute)" else "🔇 মিউট করুন (Mute Chat)")
                        }

                        // Option 4: Delete
                        Button(
                            onClick = {
                                showActionDialog = false
                                viewModel.deleteConversation(target.email)
                                Toast.makeText(context, "চ্যাট সফলভাবে ডিলিট করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("🗑️ চ্যাট ডিলিট করুন (Delete)")
                        }

                        // Option 5: Spy Mode (Only for rafid)
                        val isRafid = viewModel.isRafidUser(currentUser)
                        if (isRafid && !target.email.startsWith("group_") && !viewModel.isAiUser(target)) {
                            Button(
                                onClick = {
                                    showActionDialog = false
                                    viewModel.enterSpyMode(target)
                                    Toast.makeText(context, "স্পাই মোড সক্রিয়: ${target.name} হিসেবে দেখছেন", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Text("👁️ প্রবেশ করুন (Spy Mode)")
                            }
                        }
                    }
                }
            }
        )
    }

    // Hide Password/Secret Set Dialog
    if (showHideSetDialog && activeLongClickUser != null) {
        val target = activeLongClickUser!!
        AlertDialog(
            onDismissRequest = { showHideSetDialog = false },
            title = { Text("🙈 চ্যাট হাইড করুন (Secret Key Set)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("চ্যাটটি হাইড করার জন্য একটি গোপন শব্দ বা কোড লিখুন। পরবর্তীতে এই কোডটি দিয়ে সার্চ করলে চ্যাটটি পুনরায় খুঁজে পাবেনঃ")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = hideTextValue,
                        onValueChange = { hideTextValue = it },
                        placeholder = { Text("যেমন: secret123, swapno") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showHideSetDialog = false }) {
                        Text("বাতিল")
                    }
                    Button(
                        onClick = {
                            if (hideTextValue.trim().isEmpty()) {
                                Toast.makeText(context, "একটি কোড বা শব্দ লিখুন!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.hideChat(target.email, hideTextValue.trim())
                                showHideSetDialog = false
                                Toast.makeText(context, "চ্যাটটি সফলভাবে হাইড করা হয়েছে!", Toast.LENGTH_LONG).show()
                            }
                        }
                    ) {
                        Text("সাবমিট")
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

    // Mute Dialog
    if (showMuteDialog && muteDialogUser != null) {
        val target = muteDialogUser!!
        AlertDialog(
            onDismissRequest = { showMuteDialog = false },
            title = { Text("🔇 মিউট করার সময় নির্ধারণ করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("কত সময়ের জন্য আপনি '${removeLinkAndLockFromName(target.name)}' কে মিউট করতে চান?", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val options = listOf(
                        Pair("১ ঘণ্টা (1 Hour)", 60 * 60 * 1000L),
                        Pair("৮ ঘণ্টা (8 Hours)", 8 * 60 * 60 * 1000L),
                        Pair("১ দিন (1 Day)", 24 * 60 * 60 * 1000L),
                        Pair("১ সপ্তাহ (1 Week)", 7 * 24 * 60 * 60 * 1000L),
                        Pair("আজীবন (Mute Forever)", Long.MAX_VALUE)
                    )
                    
                    options.forEach { (label, duration) ->
                        Button(
                            onClick = {
                                viewModel.muteChat(target.email, duration)
                                showMuteDialog = false
                                Toast.makeText(context, "'${removeLinkAndLockFromName(target.name)}' সফলভাবে মিউট করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Text(label, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMuteDialog = false }) {
                    Text("বাতিল", color = MaterialTheme.colorScheme.primary)
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

    if (showGroupCustomizerDialog) {
        var groupName by remember { mutableStateOf(groupToEdit?.name ?: "") }
        var groupPhotoUrl by remember { mutableStateOf(groupToEdit?.photoUrl ?: "") }
        val selectedMembers = remember { mutableStateListOf<String>() }
        var memberSearchQuery by remember { mutableStateOf("") }

        val groupImagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    if (originalBitmap != null) {
                        val size = 150
                        val scaledBitmap = if (originalBitmap.width > size || originalBitmap.height > size) {
                            val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                            val (w, h) = if (ratio > 1) {
                                Pair(size, (size / ratio).toInt())
                            } else {
                                Pair((size * ratio).toInt(), size)
                            }
                            android.graphics.Bitmap.createScaledBitmap(originalBitmap, w, h, true)
                        } else {
                            originalBitmap
                        }
                        
                        val baos = java.io.ByteArrayOutputStream()
                        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, baos)
                        val bytes = baos.toByteArray()
                        val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        groupPhotoUrl = "data:image/jpeg;base64,$b64"
                        Toast.makeText(context, "গ্রুপের ছবি সফলভাবে নির্বাচন করা হয়েছে!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "ছবি লোড করতে ব্যর্থ: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showGroupCustomizerDialog = false; groupToEdit = null },
            title = { Text(if (groupToEdit != null) "👥 গ্রুপ সংশোধন করুন" else "👥 নতুন গ্রুপ তৈরি করুন", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("গ্রুপের নাম") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (groupPhotoUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = groupPhotoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("গ্রুপ প্রোফাইল ছবি", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = if (groupPhotoUrl.isNotEmpty()) "ছবি সফলভাবে যুক্ত হয়েছে ✔" else "কোনো ছবি যুক্ত করা হয়নি",
                                fontSize = 11.sp,
                                color = if (groupPhotoUrl.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                        Button(
                            onClick = { groupImagePickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("গ্যালারি", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("গ্রুপের ছবির ডেমো নির্বাচন করুনঃ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                    
                    val demoAvatars = listOf(
                        "https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=150&q=80" to "টিম",
                        "https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?auto=format&fit=crop&w=150&q=80" to "আড্ডা",
                        "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=150&q=80" to "মিউজিক",
                        "https://images.unsplash.com/photo-1543269865-cbf427effbad?auto=format&fit=crop&w=150&q=80" to "ক্লাস"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        demoAvatars.forEach { (url, label) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (groupPhotoUrl == url) MaterialTheme.colorScheme.primaryContainer else Color.LightGray.copy(alpha = 0.2f))
                                    .clickable { groupPhotoUrl = url }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (groupPhotoUrl == url) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("গ্রুপে সদস্য যোগ করুনঃ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = memberSearchQuery,
                        onValueChange = { memberSearchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        placeholder = { Text("সদস্য খুঁজুন...", color = Color.Gray, fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val filteredMembers = allActiveUsers.filter { u ->
                        u.name.lowercase().contains(memberSearchQuery.lowercase()) ||
                        u.email.lowercase().contains(memberSearchQuery.lowercase())
                    }
                    LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 200.dp)) {
                        items(filteredMembers) { u ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedMembers.contains(u.email)) {
                                            selectedMembers.remove(u.email)
                                        } else {
                                            selectedMembers.add(u.email)
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedMembers.contains(u.email),
                                    onCheckedChange = { checked ->
                                        if (checked == true) {
                                            selectedMembers.add(u.email)
                                        } else {
                                            selectedMembers.remove(u.email)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(u.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (groupToEdit != null) {
                        Button(
                            onClick = {
                                viewModel.deleteGroup(groupToEdit!!.email) {
                                    showGroupCustomizerDialog = false
                                    groupToEdit = null
                                    Toast.makeText(context, "গ্রুপটি ডিলিট করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("ডিলিট", color = Color.White)
                        }
                    }
                    Button(
                        onClick = {
                            if (groupName.isNotEmpty()) {
                                viewModel.createOrUpdateGroup(
                                    groupId = groupToEdit?.email,
                                    name = groupName,
                                    photoUrl = groupPhotoUrl.ifEmpty { "https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=150&q=80" },
                                    members = selectedMembers.toList()
                                ) { success ->
                                    if (success) {
                                        showGroupCustomizerDialog = false
                                        groupToEdit = null
                                        Toast.makeText(context, "গ্রুপ সফলভাবে সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "দুঃখিত, গ্রুপ তৈরিতে সমস্যা হয়েছে।", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    ) {
                        Text("সম্পন্ন")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showGroupCustomizerDialog = false; groupToEdit = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Neon corner pop-up removed as requested
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
    val infiniteTransition = rememberInfiniteTransition(label = "new_accent_color_transition")
    val animatedColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF6366F1), // Deep Royal Indigo
        targetValue = Color(0xFF6366F1),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 15000
                Color(0xFF6366F1) at 0      // Deep Royal Indigo
                Color(0xFF8B5CF6) at 3000   // Soft Orchid Violet
                Color(0xFFEC4899) at 6000   // Premium Vibrant Pink
                Color(0xFF0EA5E9) at 9000   // Vivid Sky Blue
                Color(0xFF10B981) at 12000  // Bright Emerald Teal
                Color(0xFF6366F1) at 15000  // Loop back
            },
            repeatMode = RepeatMode.Restart
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

@Composable
fun parseVerifiedColor(colorStr: String?): Color {
    val isDark = isSystemInDarkTheme()
    if (colorStr == null) return Color(0xFFD4AF37)
    val baseColor = when (colorStr.lowercase().trim()) {
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
                val cleaned = colorStr.trim()
                if (cleaned.startsWith("#")) {
                    Color(android.graphics.Color.parseColor(cleaned))
                } else if (cleaned.matches(Regex("[0-9a-fA-F]{6,8}"))) {
                    Color(android.graphics.Color.parseColor("#$cleaned"))
                } else {
                    Color(0xFFD4AF37)
                }
            } catch (e: Exception) {
                Color(0xFFD4AF37)
            }
        }
    }
    if (isDark && (baseColor == Color.Black || baseColor == Color(0xFF000000))) {
        return Color(0xFFF1F5F9)
    }
    if (!isDark && (baseColor == Color.White || baseColor == Color(0xFFFFFFFF))) {
        return Color(0xFF1E233D)
    }
    return baseColor
}

@Composable
fun GroupMembersStack(members: List<User>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-10).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val displayMembers = members.take(6)
        displayMembers.forEach { member ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(CircleGradBrush)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                if (member.photoUrl.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        )
                    }
                } else {
                    SafeAvatarImage(
                        model = member.photoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        if (members.size > 6) {
            val remainingCount = members.size - 6
            val pluses = "+".repeat(remainingCount)
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pluses,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
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
    onLongClick: () -> Unit = {},
    groupMembersList: List<User> = emptyList(),
    isNewUser: Boolean = false,
    isUprooted: Boolean = false,
    isLastMessageOwn: Boolean = false,
    status: String = "offline",
    isMuted: Boolean = false
) {
    val rowBg = if (isNewUser) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.08f)
    } else {
        Color.Transparent
    }

    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(shape)
            .background(rowBg)
            .combinedClickable(
                onClick = onSelect,
                onLongClick = onLongClick
            )
            .padding(start = if (isNewUser) 12.dp else 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isNewUser) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.tertiary)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

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
                SafeAvatarImage(
                    model = user.photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
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
                    val displayNameColor = if (unreadCount > 0) MaterialTheme.colorScheme.primary else parseVerifiedColor(finalVerifiedColor)
                    Text(
                        text = cleanName,
                        fontSize = 16.sp,
                        fontWeight = if (unreadCount > 0) FontWeight.ExtraBold else FontWeight.Bold,
                        color = displayNameColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                } else {
                    val staticColor = if (unreadCount > 0) MaterialTheme.colorScheme.primary else (if (hasHighlight) Color(0xFFD4AF37) else MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = cleanName,
                        fontSize = 16.sp,
                        fontWeight = if (unreadCount > 0) FontWeight.ExtraBold else (if (hasHighlight) FontWeight.Bold else FontWeight.SemiBold),
                        color = staticColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                if (isNewUser) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "NEW USER",
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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

                if (isMuted) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.VolumeOff,
                        contentDescription = "Muted",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
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

        if (user.email.startsWith("group_") && groupMembersList.isNotEmpty()) {
            GroupMembersStack(
                members = groupMembersList,
                modifier = Modifier.padding(end = 8.dp)
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
fun ChatWindowScreen(viewModel: EchoChatViewModel, onEditGroup: (User) -> Unit = {}) {
    val chatUser by viewModel.currentChatUser.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val typingPartner by viewModel.typingPartnerName.collectAsState()
    val partnerOnline by viewModel.partnerOnlineStatus.collectAsState()
    val premiumVerifiedColors by viewModel.premiumVerifiedColors.collectAsState()
    val chatSeenMap by viewModel.chatSeenMap.collectAsState()
    val allActiveUsers by viewModel.allUsers.collectAsState()
    val groupMembers by viewModel.groupMembers.collectAsState()
    val groupCreators by viewModel.groupCreators.collectAsState()
    val groupSubAdmins by viewModel.groupSubAdmins.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val spyingOnUser by viewModel.spyingOnUser.collectAsState()
    val agreedUsers by viewModel.agreedUsers.collectAsState()
    val adminPrivacyMode by viewModel.adminPrivacyMode.collectAsState()
    val adminAllowedUsers by viewModel.adminAllowedUsers.collectAsState()

    val chatWallpaper by viewModel.chatWallpaper.collectAsState()
    val perChatWallpapers by viewModel.perChatWallpaper.collectAsState()
    val perChatWallpaperVal = chatUser?.email?.let { perChatWallpapers[it] }
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val autoScrollLocked by viewModel.autoScrollLocked.collectAsState()
    val currentPollVotes by viewModel.currentPollVotes.collectAsState()
    val currentUserVotes by viewModel.currentUserVotes.collectAsState()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.applyWallpaper(uri.toString())
        }
    }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    androidx.activity.compose.BackHandler {
        if (spyingOnUser != null) {
            viewModel.exitSpyMode()
        }
        viewModel.selectChatUser(null)
    }

    var isMenuExpanded by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var isChatSearchVisible by remember { mutableStateOf(false) }
    var chatSearchQuery by remember { mutableStateOf("") }

    // Extra modal toggles matching DOM options
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showPollDialog by remember { mutableStateOf(false) }
    var showPlusOptionsDialog by remember { mutableStateOf(false) }
    var showGroupMembersDialog by remember { mutableStateOf(false) }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }

    var showAiChangePasswordDialog by remember { mutableStateOf(false) }
    var showAiReportUserDialog by remember { mutableStateOf(false) }
    var showAiForgotPasswordDialog by remember { mutableStateOf(false) }

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

    val currentTheme by viewModel.chatTheme.collectAsState()
    val perChatThemes by viewModel.perChatTheme.collectAsState()
    val perChatThemeVal = chatUser?.email?.let { perChatThemes[it] }
    val windowTheme = perChatThemeVal ?: currentTheme
    val bgBrush = getThemeGradient(windowTheme)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (spyingOnUser != null) {
                            viewModel.exitSpyMode()
                        }
                        viewModel.selectChatUser(null)
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        val verifiedColor = chatUser?.email?.let { premiumVerifiedColors[it] }
                        val isPremiumVerified = verifiedColor != null || chatUser?.name?.contains("(Verified)") == true
                        val finalVerifiedColor = verifiedColor ?: if (chatUser?.name?.contains("(Verified)") == true) "blue" else null
                        val cleanName = removeLinkFromName(chatUser?.name ?: "")
                        val headerImageUrl = chatUser?.name?.let { extractLinkFromName(it) }
                        val headerHasHighlight = chatUser?.name?.let { hasPhotoLink(it) } == true

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
                                    style = MaterialTheme.typography.titleMedium.copy(color = if (headerHasHighlight) Color(0xFFD4AF37) else MaterialTheme.colorScheme.onSurface),
                                    fontWeight = FontWeight.Bold
                                )
                                if (headerHasHighlight) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Verified,
                                        contentDescription = "Highlighted Group",
                                        tint = Color(0xFFD4AF37),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (!headerImageUrl.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                AsyncImage(
                                    model = headerImageUrl,
                                    contentDescription = "Header Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, Color(0xFFD4AF37), CircleShape)
                                )
                            }
                        }
                        Text(
                            text = if (chatUser?.email?.startsWith("group_") == true) {
                                "👥 Group ${chatUser?.statusMessage ?: ""}"
                            } else {
                                (when (partnerOnline) {
                                    "online" -> "🟢 Online"
                                    "away" -> "🟡 Away"
                                    else -> "⚫ Offline"
                                }) + if (!chatUser?.statusMessage.isNullOrEmpty()) " | \"${chatUser?.statusMessage}\"" else ""
                            },
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
                        if (chatUser?.email?.startsWith("group_") == true) {
                            DropdownMenuItem(
                                text = { Text("👥 Edit Group") },
                                onClick = { isMenuExpanded = false; onEditGroup(chatUser!!) },
                                leadingIcon = { Icon(Icons.Filled.Edit, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("👥 Group Members & Admins") },
                                onClick = { isMenuExpanded = false; showGroupMembersDialog = true },
                                leadingIcon = { Icon(Icons.Filled.Group, null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("📁 Chat Statistics") },
                            onClick = { isMenuExpanded = false; showStatsDialog = true },
                            leadingIcon = { Icon(Icons.Filled.BarChart, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("🖼️ Custom Wallpaper") },
                            onClick = { isMenuExpanded = false; galleryLauncher.launch("image/*") },
                            leadingIcon = { Icon(Icons.Filled.Image, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("🎨 Choose Chat Theme") },
                            onClick = { isMenuExpanded = false; showThemeDialog = true },
                            leadingIcon = { Icon(Icons.Filled.Palette, null) }
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
            if (spyingOnUser != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Visibility, contentDescription = "Spying", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "স্পাই মোড সক্রিয়ঃ আপনি এখন ${spyingOnUser?.name} এর চ্যাট দেখছেন।",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { viewModel.exitSpyMode() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("বাহির হন", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

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

            // AI assistant quick action banner
            val isAiChat = viewModel.isAiUser(chatUser)
            if (isAiChat) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "🤖 এ আই কুইক অ্যাকশনস (AI Quick Actions)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Change Password button
                            Button(
                                onClick = { viewModel.sendMessage("পাসওয়ার্ড পরিবর্তন") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Lock, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("পাসওয়ার্ড পরিবর্তন", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Report User button
                            Button(
                                onClick = { viewModel.sendMessage("ইউজার রিপোর্ট") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Report, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ইউজার রিপোর্ট", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Forgot Password button
                            Button(
                                onClick = { viewModel.sendMessage("পাসওয়ার্ড ফরগেট") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.VpnKey, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("পাসওয়ার্ড ফরগেট", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
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
                    .then(Modifier.background(Color.Transparent))
            ) {
                // If the wallpaper is "receiver_center", show a large centered watermark of the partner's profile photo
                if (perChatWallpaperVal == "receiver_center") {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!chatUser?.photoUrl.isNullOrEmpty()) {
                            SafeAvatarImage(
                                model = chatUser?.photoUrl,
                                contentDescription = null,
                                alpha = 0.12f,
                                modifier = Modifier.size(240.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                modifier = Modifier.size(240.dp)
                            )
                        }
                    }
                }

                // If it is a custom wallpaper, render it with Coil AsyncImage as a background
                val currentWallpaper = perChatWallpaperVal ?: chatWallpaper
                if (currentWallpaper.isNotEmpty() && currentWallpaper != "none" && currentWallpaper != "dots" && currentWallpaper != "grid" && currentWallpaper != "waves" && currentWallpaper != "stars" && currentWallpaper != "receiver_center") {
                    AsyncImage(
                        model = currentWallpaper,
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
                        val activeChatUser = chatUser
                        val isGrp = activeChatUser?.email?.startsWith("group_") == true
                        val senderVerifiedColor = premiumVerifiedColors[msg.senderEmail]

                        // Calculate seenUsers:
                        val potentialUsers = if (isGrp) {
                            val memberEmails = activeChatUser?.email?.let { groupMembers[it] } ?: emptyList()
                            memberEmails.filter { it != msg.senderEmail }
                        } else {
                            if (activeChatUser != null) {
                                listOf(activeChatUser.email)
                            } else {
                                emptyList()
                            }
                        }

                        val seenUsers = potentialUsers.mapNotNull { email ->
                            val sanitized = sanitizeEmailForSeen(email)
                            val seenTs = chatSeenMap[sanitized] ?: 0L
                            if (seenTs >= msg.timestampMs) {
                                val found = allActiveUsers.find { it.email == email }
                                found ?: User(email = email, name = email.split("@")[0], photoUrl = "")
                            } else {
                                null
                            }
                        }

                        val senderUser = allActiveUsers.find { it.email.lowercase() == msg.senderEmail.lowercase() }
                        val senderPhotoUrl = senderUser?.photoUrl ?: extractLinkFromName(msg.senderName)

                        MessageBubble(
                            msg = msg,
                            searchQuery = chatSearchQuery,
                            partnerOnline = partnerOnline,
                            isGroup = isGrp,
                            senderVerifiedColor = senderVerifiedColor,
                            seenUsers = seenUsers,
                            votesMap = currentPollVotes[msg.id] ?: emptyMap(),
                            currentUserVote = currentUserVotes[msg.id],
                            onVote = { selectedOption ->
                                viewModel.castVote(msg.id, selectedOption)
                            },
                            onViewPoll = {
                                viewModel.fetchPollVotes()
                            },
                            onCopy = {
                                clipboardCopy(msg.text)
                            },
                            onDelete = {
                                viewModel.deleteMessage(msg.id)
                            },
                            onView = {
                                activeChatUser?.email?.let { otherMail ->
                                    viewModel.viewAndDecryptMessage(otherMail, msg)
                                }
                            },
                            senderPhotoUrl = senderPhotoUrl,
                            onReply = {
                                replyingToMessage = msg
                            }
                        )
                    }
                }
                
                // Wallpaper support overlays
                val activeWallpaper = perChatWallpaperVal ?: chatWallpaper
                if (activeWallpaper != "none" && activeWallpaper.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        when (activeWallpaper) {
                            "dots" -> {
                                val dotRadius = 2f
                                val spacing = 40f
                                for (x in 0..size.width.toInt() step spacing.toInt()) {
                                    for (y in 0..size.height.toInt() step spacing.toInt()) {
                                        drawCircle(
                                            color = Color.LightGray.copy(alpha = 0.2f),
                                            radius = dotRadius,
                                            center = Offset(x.toFloat(), y.toFloat())
                                        )
                                    }
                                }
                            }
                            "grid" -> {
                                val spacing = 60f
                                for (x in 0..size.width.toInt() step spacing.toInt()) {
                                    drawLine(
                                        color = Color.LightGray.copy(alpha = 0.12f),
                                        start = Offset(x.toFloat(), 0f),
                                        end = Offset(x.toFloat(), size.height),
                                        strokeWidth = 1f
                                    )
                                }
                                for (y in 0..size.height.toInt() step spacing.toInt()) {
                                    drawLine(
                                        color = Color.LightGray.copy(alpha = 0.12f),
                                        start = Offset(0f, y.toFloat()),
                                        end = Offset(size.width, y.toFloat()),
                                        strokeWidth = 1f
                                    )
                                }
                            }
                            "sunset" -> {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFFFF7E5F).copy(alpha = 0.08f), Color(0xFFFEB47B).copy(alpha = 0.08f))
                                    )
                                )
                            }
                            "ocean" -> {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF2E0854).copy(alpha = 0.08f), Color(0xFF00C9FF).copy(alpha = 0.08f))
                                    )
                                )
                            }
                            "forest" -> {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF134E5E).copy(alpha = 0.08f), Color(0xFF71B280).copy(alpha = 0.08f))
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Message Input bar layout matching premium glassmorphism
            val localChatUser = chatUser
            val localCurrentUser = currentUser
            val isGroup = localChatUser?.email?.startsWith("group_") == true
            val isRecipientSpecial = !isGroup && localChatUser?.name?.trim()?.endsWith("°") == true
            val isSenderRafid = viewModel.isRafidUser(localCurrentUser)
            val isSenderSelf = localCurrentUser?.email?.lowercase() == localChatUser?.email?.lowercase()
            val isSenderAgreed = if (localChatUser != null && localCurrentUser != null) {
                agreedUsers[viewModel.sanitizeId(localChatUser.email)]?.get(viewModel.sanitizeId(localCurrentUser.email)) == true
            } else {
                false
            }
            val canSms = !isRecipientSpecial || isSenderRafid || isSenderSelf || isSenderAgreed

            val isRecipientAdmin = !isGroup && viewModel.isRafidUser(localChatUser)
            val isSenderAdmin = viewModel.isRafidUser(localCurrentUser)

            val isAllowedByAdminPrivacy = if (isRecipientAdmin && !isSenderAdmin) {
                when (adminPrivacyMode) {
                    "Yes" -> false
                    "Customize" -> {
                        val senderKey = if (localCurrentUser != null) viewModel.sanitizeId(localCurrentUser.email) else ""
                        adminAllowedUsers[senderKey] == true
                    }
                    else -> true // "No"
                }
            } else {
                true
            }

            if (canSms && isAllowedByAdminPrivacy) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    replyingToMessage?.let { reply ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(36.dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "রিপ্লাই করা হচ্ছে: ${reply.senderName}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                    Text(
                                        text = reply.text,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(
                                onClick = { replyingToMessage = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Reply",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

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
                            showPlusOptionsDialog = true
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
                                    val replyData = replyingToMessage?.let {
                                        ReplyToData(
                                            id = it.id,
                                            text = it.text,
                                            user = it.senderName
                                        )
                                    }
                                    viewModel.sendMessage(textInput, replyData)
                                    textInput = ""
                                    replyingToMessage = null
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
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.Red.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (!isAllowedByAdminPrivacy) "this account is not available" else "ইউ ক্যান নট এসএমএস দিস account",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Wallpaper configuration dialog representation
    if (showWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            title = { Text("🖼️ চ্যাট ওয়ালপেপার (Wallpaper)") },
            text = {
                Column {
                    Text("ব্যক্তিগত ওয়ালপেপার (শুধুমাত্র আপনার ফোনে):", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                    listOf(
                        "none" to "None (ডিফল্ট)",
                        "dots" to "Dots (বিন্দু)",
                        "grid" to "Grid (গ্রিড)",
                        "waves" to "Waves (ঢেউ)",
                        "stars" to "Stars (তারা)"
                    ).forEach { (k, name) ->
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
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text("দ্বিপাক্ষিক ওয়ালপেপার (উভয় ব্যবহারকারীর ফোনে):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
                    listOf(
                        "receiver_center" to "Receiver Center Photo (গ্রাহকের ছবি সেন্টারে)",
                        "sunset" to "Sunset Gradient (সূর্যাস্ত থিম)",
                        "ocean" to "Ocean Gradient (মহাসাগর থিম)",
                        "forest" to "Forest Gradient (অরণ্য থিম)"
                    ).forEach { (k, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    chatUser?.email?.let { otherEmail ->
                                        viewModel.setPerChatWallpaper(otherEmail, k)
                                    }
                                    showWallpaperDialog = false
                                }
                                .padding(12.dp)
                        ) {
                            Text(name, color = MaterialTheme.colorScheme.primary)
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

    if (showPlusOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showPlusOptionsDialog = false },
            title = { Text("📎 Attachments & Options", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Option 1: Create Poll
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .clickable {
                                showPlusOptionsDialog = false
                                showPollDialog = true
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Poll, "Poll", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("📊 Create Poll (ভোট তৈরি করুন)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("গ্রুপে যেকোনো বিষয়ে পোল বা ভোট আয়োজন করুন", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Option 2: Share Location
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                            .clickable {
                                showPlusOptionsDialog = false
                                try {
                                    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
                                    val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
                                    val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
                                    if (!isGpsEnabled && !isNetworkEnabled) {
                                        Toast.makeText(context, "আগে লোকেশন অন করুন", Toast.LENGTH_LONG).show()
                                        return@clickable
                                    }
                                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        var loc: android.location.Location? = null
                                        if (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                                            loc = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                                        }
                                        if (loc == null && locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                                            loc = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                                        }
                                        
                                        if (loc != null) {
                                            val lat = loc.latitude
                                            val lng = loc.longitude
                                            val acc = loc.accuracy
                                            viewModel.sendMessage("📍 Accurate GPS Location: $lat, $lng (Accuracy: ${acc}m)\nGoogle Map: https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                                        } else {
                                            // Request a single update if last known is null
                                            val listener = object : android.location.LocationListener {
                                                override fun onLocationChanged(location: android.location.Location) {
                                                    val lat = location.latitude
                                                    val lng = location.longitude
                                                    val acc = location.accuracy
                                                    viewModel.sendMessage("📍 Accurate GPS Location: $lat, $lng (Accuracy: ${acc}m)\nGoogle Map: https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                                                    locationManager.removeUpdates(this)
                                                }
                                                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                                                override fun onProviderEnabled(provider: String) {}
                                                override fun onProviderDisabled(provider: String) {}
                                            }
                                            
                                            // Request update on main thread/main looper
                                            locationManager.requestLocationUpdates(
                                                android.location.LocationManager.GPS_PROVIDER,
                                                0L,
                                                0f,
                                                listener,
                                                android.os.Looper.getMainLooper()
                                            )
                                            // Let the user know we're fetching accurate GPS location
                                            Toast.makeText(context, "নির্ভুল জিপিএস অবস্থান খোঁজা হচ্ছে...", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "লোকেশন পারমিশন দেওয়া হয়নি!", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    viewModel.sendMessage("📍 Location: 23.8103° N, 90.4125° E (Dhaka, Bangladesh - GPS Unavailable)")
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.LocationOn, "Location", tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("📍 Share Location (অবস্থান শেয়ার করুন)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("আপনার বর্তমান অবস্থান ম্যাপ লিংকের সাথে চ্যাটে পাঠান", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Option 3: Custom Wallpaper
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
                            .clickable {
                                showPlusOptionsDialog = false
                                galleryLauncher.launch("image/*")
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Image, "Wallpaper", tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("🖼️ Chat Wallpaper (চ্যাট ওয়ালপেপার)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("চ্যাট উইন্ডোর জন্য কাস্টম ওয়ালপেপার সেট করুন", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPlusOptionsDialog = false }) {
                    Text("বন্ধ করুন")
                }
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

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("🎨 চ্যাট থিম পরিবর্তন") },
            text = {
                val themes = listOf("default", "sunset", "ocean", "forest", "midnight", "neon", "warm", "rose")
                Column {
                    Text("এই চ্যাটের জন্য নির্দিষ্ট একটি থিম নির্বাচন করুনঃ", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    themes.chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { t ->
                                val isSelected = (perChatThemeVal == t || (perChatThemeVal == null && currentTheme == t))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(getThemeGradient(t))
                                        .clickable {
                                            chatUser?.email?.let { otherEmail ->
                                                viewModel.setPerChatTheme(otherEmail, t)
                                            }
                                            showThemeDialog = false
                                        }
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = Color.White,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }

    if (showAiChangePasswordDialog) {
        var oldPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var errorText by remember { mutableStateOf<String?>(null) }
        var successText by remember { mutableStateOf<String?>(null) }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAiChangePasswordDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("পাসওয়ার্ড পরিবর্তন")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("আপনার অ্যাকাউন্টের পাসওয়ার্ড পরিবর্তন করুন।", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    
                    OutlinedTextField(
                        value = oldPass,
                        onValueChange = { oldPass = it; errorText = null },
                        label = { Text("পুরাতন পাসওয়ার্ড (Old Password)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it; errorText = null },
                        label = { Text("নতুন পাসওয়ার্ড (New Password)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorText != null) {
                        Text(errorText!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    if (successText != null) {
                        Text(successText!!, color = Color(0xFF00E676), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (oldPass.isEmpty() || newPass.isEmpty()) {
                            errorText = "সবগুলো ঘর পূরণ করুন।"
                            return@Button
                        }
                        if (newPass.length < 6) {
                            errorText = "নতুন পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে।"
                            return@Button
                        }
                        isSubmitting = true
                        viewModel.changePassword(
                            oldPass = oldPass,
                            newPass = newPass,
                            onSuccess = {
                                isSubmitting = false
                                successText = "পাসওয়ার্ড সফলভাবে পরিবর্তিত হয়েছে!"
                                oldPass = ""
                                newPass = ""
                            },
                            onError = { error ->
                                isSubmitting = false
                                errorText = error
                            }
                        )
                    },
                    enabled = !isSubmitting && successText == null
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("পরিবর্তন করুন")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiChangePasswordDialog = false }) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }

    if (showAiReportUserDialog) {
        var reportedEmail by remember { mutableStateOf("") }
        var reason by remember { mutableStateOf("") }
        var errorText by remember { mutableStateOf<String?>(null) }
        var successText by remember { mutableStateOf<String?>(null) }
        var isSubmitting by remember { mutableStateOf(false) }
        var isDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAiReportUserDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Report, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ইউজার রিপোর্ট করুন")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("কোনো ব্যবহারকারী অপব্যবহার বা নিয়ম ভঙ্গ করলে তার বিরুদ্ধে রিপোর্ট জমা দিন।", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = reportedEmail,
                            onValueChange = { reportedEmail = it; errorText = null },
                            label = { Text("রিপোর্টকৃত ইউজারের ইমেইল") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { isDropdownExpanded = !isDropdownExpanded }) {
                                    Icon(Icons.Filled.ArrowDropDown, "Select User")
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allActiveUsers.filter { it.email != currentUser?.email && !it.email.startsWith("group_") && !viewModel.isAiUser(it) }.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text("${u.name} (${u.email})") },
                                    onClick = {
                                        reportedEmail = u.email
                                        isDropdownExpanded = false
                                        errorText = null
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it; errorText = null },
                        label = { Text("রিপোর্ট করার কারণ (Reason)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    if (errorText != null) {
                        Text(errorText!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    if (successText != null) {
                        Text(successText!!, color = Color(0xFF00E676), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportedEmail.isEmpty() || reason.isEmpty()) {
                            errorText = "সবগুলো ঘর পূরণ করুন।"
                            return@Button
                        }
                        isSubmitting = true
                        viewModel.reportUser(
                            reportedEmail = reportedEmail,
                            reason = reason,
                            onSuccess = {
                                isSubmitting = false
                                successText = "রিপোর্টটি সফলভাবে জমা দেওয়া হয়েছে। এডমিন প্যানেল এটি পর্যালোচনা করবে।"
                                reportedEmail = ""
                                reason = ""
                            },
                            onError = { error ->
                                isSubmitting = false
                                errorText = error
                            }
                        )
                    },
                    enabled = !isSubmitting && successText == null
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("রিপোর্ট জমা দিন")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiReportUserDialog = false }) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }

    if (showAiForgotPasswordDialog) {
        var forgotStep by remember { mutableStateOf(1) } // 1: request code, 2: verify code, 3: reset password
        var forgotEmail by remember { mutableStateOf("") }
        var forgotCode by remember { mutableStateOf("") }
        var forgotNewPass by remember { mutableStateOf("") }
        var forgotErrorText by remember { mutableStateOf<String?>(null) }
        var forgotSuccessText by remember { mutableStateOf<String?>(null) }
        var forgotIsSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAiForgotPasswordDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("পাসওয়ার্ড ফরগেট / রিসেট")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (forgotStep) {
                        1 -> {
                            Text("আপনার রেজিস্টার্ড ইমেইল দিন। আমরা একটি ওটিপি কোড পাঠাবো।", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            OutlinedTextField(
                                value = forgotEmail,
                                onValueChange = { forgotEmail = it; forgotErrorText = null },
                                label = { Text("ইমেইল এড্রেস (Email Address)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        2 -> {
                            Text("আপনার ইমেইলে পাঠানো ভেরিফিকেশন কোডটি নিচে দিন।", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            OutlinedTextField(
                                value = forgotCode,
                                onValueChange = { forgotCode = it; forgotErrorText = null },
                                label = { Text("ভেরিফিকেশন কোড (Code)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        3 -> {
                            Text("আপনার নতুন পাসওয়ার্ড সেট করুন।", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            OutlinedTextField(
                                value = forgotNewPass,
                                onValueChange = { forgotNewPass = it; forgotErrorText = null },
                                label = { Text("নতুন পাসওয়ার্ড (New Password)") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (forgotErrorText != null) {
                        Text(forgotErrorText!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    if (forgotSuccessText != null) {
                        Text(forgotSuccessText!!, color = Color(0xFF00E676), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        forgotErrorText = null
                        when (forgotStep) {
                            1 -> {
                                if (forgotEmail.isEmpty()) {
                                    forgotErrorText = "অনুগ্রহ করে ইমেইল লিখুন।"
                                    return@Button
                                }
                                forgotIsSubmitting = true
                                viewModel.requestForgotPasswordCode(
                                    email = forgotEmail,
                                    onSuccess = {
                                        forgotIsSubmitting = false
                                        forgotStep = 2
                                    },
                                    onError = { error ->
                                        forgotIsSubmitting = false
                                        forgotErrorText = error
                                    }
                                )
                            }
                            2 -> {
                                if (forgotCode.isEmpty()) {
                                    forgotErrorText = "অনুগ্রহ করে কোডটি লিখুন।"
                                    return@Button
                                }
                                forgotIsSubmitting = true
                                viewModel.verifyForgotPasswordCode(
                                    email = forgotEmail,
                                    code = forgotCode,
                                    onSuccess = {
                                        forgotIsSubmitting = false
                                        forgotStep = 3
                                    },
                                    onError = { error ->
                                        forgotIsSubmitting = false
                                        forgotErrorText = error
                                    }
                                )
                            }
                            3 -> {
                                if (forgotNewPass.isEmpty()) {
                                    forgotErrorText = "অনুগ্রহ করে নতুন পাসওয়ার্ড লিখুন।"
                                    return@Button
                                }
                                if (forgotNewPass.length < 6) {
                                    forgotErrorText = "নতুন পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে।"
                                    return@Button
                                }
                                forgotIsSubmitting = true
                                viewModel.resetPassword(
                                    email = forgotEmail,
                                    newPass = forgotNewPass,
                                    onSuccess = {
                                        forgotIsSubmitting = false
                                        forgotSuccessText = "পাসওয়ার্ড সফলভাবে রিসেট করা হয়েছে!"
                                    },
                                    onError = { error ->
                                        forgotIsSubmitting = false
                                        forgotErrorText = error
                                    }
                                )
                            }
                        }
                    },
                    enabled = !forgotIsSubmitting && (forgotSuccessText == null)
                ) {
                    if (forgotIsSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(
                            when (forgotStep) {
                                1 -> "কোড পাঠান"
                                2 -> "কোড যাচাই করুন"
                                3 -> "পাসওয়ার্ড রিসেট করুন"
                                else -> "পরবর্তী"
                            }
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (forgotStep > 1 && forgotSuccessText == null) {
                            forgotStep--
                        } else {
                            showAiForgotPasswordDialog = false
                        }
                    }
                ) {
                    Text(if (forgotStep > 1 && forgotSuccessText == null) "পিছনে যান" else "বন্ধ করুন")
                }
            }
        )
    }

    if (showGroupMembersDialog && chatUser != null && chatUser!!.email.startsWith("group_")) {
        val groupId = chatUser!!.email
        val creatorEmail = groupCreators[groupId] ?: ""
        val subAdminEmails = groupSubAdmins[groupId] ?: emptyList()
        val memberEmails = groupMembers[groupId] ?: emptyList()
        
        val membersList = memberEmails.map { email ->
            allActiveUsers.find { it.email.lowercase() == email.lowercase() } ?: User(email = email, name = email.split("@")[0], photoUrl = "")
        }
        
        GroupMembersAndAdminsDialog(
            groupId = groupId,
            currentUserEmail = currentUser?.email ?: "",
            creatorEmail = creatorEmail,
            subAdminEmails = subAdminEmails,
            membersList = membersList,
            onToggleSubAdmin = { email ->
                viewModel.toggleSubAdmin(groupId, email)
            },
            onMakeEveryoneSubAdmin = {
                viewModel.makeEveryoneSubAdmin(groupId)
            },
            onDismiss = { showGroupMembersDialog = false }
        )
    }
    }
}

@Composable
fun GroupMembersAndAdminsDialog(
    groupId: String,
    currentUserEmail: String,
    creatorEmail: String,
    subAdminEmails: List<String>,
    membersList: List<User>,
    onToggleSubAdmin: (String) -> Unit,
    onMakeEveryoneSubAdmin: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "গ্রুপ মেম্বার এবং অ্যাডমিন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                // If current user is the main group admin, show "Make Everyone Sub-Admin" button
                val isCurrentUserAdmin = currentUserEmail.lowercase() == creatorEmail.lowercase()
                if (isCurrentUserAdmin) {
                    Button(
                        onClick = {
                            onMakeEveryoneSubAdmin()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Filled.Stars, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("সবাইকে সাব-অ্যাডমিন করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Divider(modifier = Modifier.padding(bottom = 8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(membersList) { member ->
                        val isCreator = member.email.lowercase() == creatorEmail.lowercase()
                        val isSubAdmin = subAdminEmails.any { it.lowercase() == member.email.lowercase() }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CircleGradBrush)
                            ) {
                                if (member.photoUrl.isNullOrEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = member.name.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                } else {
                                    SafeAvatarImage(
                                        model = member.photoUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Name & Badge
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isCreator) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFD4AF37))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "👑 অ্যাডমিন",
                                                color = Color.Black,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    } else if (isSubAdmin) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "⭐ সাব-অ্যাডমিন",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "মেম্বার",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }

                            // If current user is the main group admin and this is not themselves, allow promoting/demoting
                            if (isCurrentUserAdmin && !isCreator) {
                                IconButton(
                                    onClick = { onToggleSubAdmin(member.email) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSubAdmin) Icons.Filled.RemoveCircleOutline else Icons.Filled.AddCircleOutline,
                                        contentDescription = if (isSubAdmin) "Demote Sub-Admin" else "Promote Sub-Admin",
                                        tint = if (isSubAdmin) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন", fontWeight = FontWeight.Bold)
            }
        }
    )
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

@Composable
fun ClickableLinkText(
    text: String,
    textColor: Color,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontStyle: FontStyle? = null
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val annotatedString = remember(text, textColor) {
        buildAnnotatedString {
            val urlPattern = java.util.regex.Pattern.compile(
                "((?:https?://|www\\.)[^\\s]+)",
                java.util.regex.Pattern.CASE_INSENSITIVE
            )
            val matcher = urlPattern.matcher(text)
            var lastIndex = 0
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                
                // Append text before match
                if (start > lastIndex) {
                    append(text.substring(lastIndex, start))
                }
                
                // Append highlighted link
                val linkText = text.substring(start, end)
                pushStringAnnotation(tag = "URL", annotation = linkText)
                withStyle(
                    style = androidx.compose.ui.text.SpanStyle(
                        color = Color(0xFF38BDF8),
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(linkText)
                }
                pop()
                lastIndex = end
            }
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }

    androidx.compose.foundation.text.ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = textColor,
            fontWeight = fontWeight,
            fontSize = fontSize,
            fontStyle = fontStyle
        ),
        modifier = modifier,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        var targetUrl = annotation.item
                        if (targetUrl.lowercase().startsWith("www.")) {
                            targetUrl = "https://" + targetUrl
                        }
                        uriHandler.openUri(targetUrl)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        }
    )
}

private fun sanitizeEmailForSeen(email: String): String {
    return email.lowercase().replace(Regex("[.#$\\[\\]]"), "_")
}

// Helper for displaying user avatars seen list
@Composable
fun SeenUsersRow(seenUsers: List<User>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy((-5).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val displayUsers = seenUsers.take(6)
        displayUsers.forEach { user ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
                    .border(1.dp, Color.White, CircleShape)
            ) {
                if (user.photoUrl.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CircleGradBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    SafeAvatarImage(
                        model = user.photoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        if (seenUsers.size > 6) {
            val remainingCount = seenUsers.size - 6
            val pluses = "+".repeat(remainingCount)
            Text(
                text = pluses,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}

// Single message item bubble
@Composable
fun MessageBubble(
    msg: ChatMessage,
    searchQuery: String = "",
    partnerOnline: String = "offline",
    isGroup: Boolean = false,
    senderVerifiedColor: String? = null,
    seenUsers: List<User> = emptyList(),
    votesMap: Map<Int, Int> = emptyMap(),
    currentUserVote: Int? = null,
    onVote: (Int) -> Unit = {},
    onViewPoll: () -> Unit = {},
    onCopy: () -> Unit = {},
    onDelete: () -> Unit = {},
    onView: () -> Unit = {},
    senderPhotoUrl: String? = null,
    onReply: () -> Unit = {}
) {
    LaunchedEffect(msg.id) {
        onView()
    }
    if (msg.text.startsWith("📊 POLL:")) {
        LaunchedEffect(msg.id) {
            onViewPoll()
        }
    }
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
            text = { Text("আপনি কি এই মেসেজটি ক্লাউড সার্ভার থেকে স্বয়ংক্রিয়ভাবে মুছে ফেলতে চান?\n\nএটি ক্লাউড সার্ভার (Cloud Server) ডেটাবেস থেকে ডিলিট হয়ে যাবে কিন্তু লোকাল হোস্টে চিরস্থায়ীভাবে থেকে যাবে।") },
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

    // Swipe-to-reply gesture logic with smooth spring bounce back using standard Modifier.draggable
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by androidx.compose.animation.core.animateFloatAsState(
        targetValue = offsetX,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        )
    )

    val draggableState = rememberDraggableState { delta ->
        if (msg.isOwn) {
            offsetX = (offsetX + delta).coerceIn(-150f, 0f)
        } else {
            offsetX = (offsetX + delta).coerceIn(0f, 150f)
        }
    }

    val swipeModifier = Modifier.draggable(
        state = draggableState,
        orientation = Orientation.Horizontal,
        onDragStopped = { _: Float ->
            if (msg.isOwn && offsetX <= -80f) {
                onReply()
            } else if (!msg.isOwn && offsetX >= 80f) {
                onReply()
            }
            offsetX = 0f
        }
    )

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
        // Group Sender ID Label & Verified badge
        if (isGroup && !msg.isOwn) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
            ) {
                val cleanSenderName = removeLinkAndLockFromName(msg.senderName)
                val senderImageUrl = senderPhotoUrl ?: extractLinkFromName(msg.senderName)
                val senderHasHighlight = hasPhotoLink(msg.senderName)

                Text(
                    text = cleanSenderName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (senderHasHighlight) Color(0xFFD4AF37) else MaterialTheme.colorScheme.primary
                )
                if (senderVerifiedColor != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verified Sender",
                        tint = parseVerifiedColor(senderVerifiedColor),
                        modifier = Modifier.size(13.dp)
                    )
                } else if (senderHasHighlight) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Highlighted Sender",
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(13.dp)
                    )
                }
                if (!senderImageUrl.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    AsyncImage(
                        model = senderImageUrl,
                        contentDescription = "Sender Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0xFFD4AF37), CircleShape)
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            // Drag indicators for Reply action
            if (offsetX > 15f && !msg.isOwn) {
                Icon(
                    imageVector = Icons.Default.Reply,
                    contentDescription = "Reply",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = (offsetX / 150f).coerceIn(0f, 1f)),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp)
                        .size(24.dp)
                )
            } else if (offsetX < -15f && msg.isOwn) {
                Icon(
                    imageVector = Icons.Default.Reply,
                    contentDescription = "Reply",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = (-offsetX / 150f).coerceIn(0f, 1f)),
                    modifier = Modifier
                        .graphicsLayer { rotationY = 180f }
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .size(24.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = if (msg.isOwn) Arrangement.End else Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = animatedOffsetX }
                    .then(swipeModifier)
            ) {
                if (!msg.isOwn) {
                    val senderImageUrl = senderPhotoUrl ?: extractLinkFromName(msg.senderName)
                    SafeAvatarImage(
                        model = senderImageUrl,
                        contentDescription = "Sender Profile Photo",
                        modifier = Modifier
                            .padding(start = 8.dp, top = 4.dp, end = 4.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                    )
                }
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
                        // Render reply parent message preview if it exists
                        msg.replyTo?.let { reply ->
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (msg.isOwn) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f))
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Spacer(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(34.dp)
                                            .background(if (msg.isOwn) Color.White else MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)) {
                                        Text(
                                            text = reply.user,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (msg.isOwn) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary
                                        )
                                    Text(
                                        text = reply.text,
                                        fontSize = 11.sp,
                                        color = if (msg.isOwn) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                        if (msg.text.startsWith("📊 POLL:")) {
                        val lines = msg.text.split("\n")
                        val question = lines.getOrNull(0)?.replace("📊 POLL:", "")?.trim() ?: ""
                        val options = lines.drop(1).map { it.trim() }.filter { it.isNotEmpty() }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "📊 $question",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val totalVotes = votesMap.values.sum()
                            
                            options.forEachIndexed { index, option ->
                                val votesForOption = votesMap[index] ?: 0
                                val isMyVote = currentUserVote == index
                                val percentage = if (totalVotes > 0) (votesForOption.toFloat() / totalVotes * 100).toInt() else 0
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isMyVote) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            } else {
                                                contentColor.copy(alpha = 0.08f)
                                            }
                                        )
                                        .border(
                                            width = if (isMyVote) 2.dp else 1.dp,
                                            color = if (isMyVote) MaterialTheme.colorScheme.primary else contentColor.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onVote(index) }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = isMyVote,
                                                onClick = { onVote(index) },
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = if (msg.isOwn) Color.White else MaterialTheme.colorScheme.primary
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = option,
                                                fontWeight = if (isMyVote) FontWeight.Bold else FontWeight.Medium,
                                                color = contentColor,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Text(
                                            text = "$votesForOption ভোট ($percentage%)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = contentColor.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        ClickableLinkText(
                            text = msg.text,
                            textColor = contentColor,
                            fontWeight = if (hasSearchMatch) FontWeight.Bold else FontWeight.Normal
                        )

                        // Extracted image preview for link sharing
                        val imageRegex = "(https?://[^\\s]+(?:\\.jpg|\\.png|\\.gif|\\.webp|\\.jpeg|\\?.*img|\\?.*image)[^\\s]*)".toRegex(RegexOption.IGNORE_CASE)
                        val inlineImageUrl = msg.imageUrl ?: imageRegex.find(msg.text)?.value
                        
                        if (!inlineImageUrl.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    AsyncImage(
                                        model = inlineImageUrl,
                                        contentDescription = "Shared Image Link",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = "📷 Scene: 100% Captured",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isTranslated) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(contentColor.copy(alpha = 0.25f))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ClickableLinkText(
                            text = getLocalTranslation(msg.text),
                            textColor = contentColor.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium
                        )
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

        // Overlapping seen avatars under/adjacent to message
        if (seenUsers.isNotEmpty() && msg.isOwn) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (msg.isOwn) Arrangement.End else Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Seen",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                SeenUsersRow(seenUsers = seenUsers)
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
    clean = clean.replace(Regex("(https?://[^\\s]+)"), "").replace(Regex("[=#৳®©™°]+$"), "")
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
                    TabPill(text = "🔒 App Lock", active = activeTab == "applock") { activeTab = "applock" }
                    TabPill(text = "🌐 Language", active = activeTab == "language") { activeTab = "language" }
                    TabPill(text = "🎨 Theme", active = activeTab == "appearance") { activeTab = "appearance" }
                    TabPill(text = "💞 Pair", active = activeTab == "pair") { activeTab = "pair" }
                    TabPill(text = "🔑 Password", active = activeTab == "password") { activeTab = "password" }
                    TabPill(text = "📱 Device", active = activeTab == "device") { activeTab = "device" }
                    TabPill(text = "🚀 Version", active = activeTab == "version") { activeTab = "version" }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(initialScale = 0.98f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))).togetherWith(
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + scaleOut(targetScale = 0.98f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        )
                    },
                    label = "settings_tabs_fade"
                ) { targetTab ->
                    when (targetTab) {
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

                    "appearance" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Dark Theme (রাত্রিকালীন মোড)")
                                Switch(checked = isDarkMode, onCheckedChange = { viewModel.setDarkMode(it) })
                            }

                            Text("অ্যাপের ব্যাকগ্রাউন্ড থিম নির্বাচন করুণঃ", fontWeight = FontWeight.SemiBold)

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
                    }

                    "pair" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
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
                                
                                var pairSearchQuery by remember { mutableStateOf("") }
                                var reqMail by remember { mutableStateOf("") }
                                var pairRequestedEmailNotFound by remember { mutableStateOf<String?>(null) }
                                var pairRequestErrorMsg by remember { mutableStateOf<String?>(null) }

                                OutlinedTextField(
                                    value = pairSearchQuery,
                                    onValueChange = { pairSearchQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    placeholder = { Text("পেয়ারিংয়ের জন্য ইউজার সার্চ করুন...", color = Color.Gray) },
                                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.primary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )

                                val pairingCandidates = allActiveUsers.filter { u ->
                                    u.email != user?.email && !u.email.startsWith("group_") && !viewModel.isAiUser(u)
                                }
                                val filteredPairCandidates = if (pairSearchQuery.isNotEmpty()) {
                                    pairingCandidates.filter { u ->
                                        u.name.lowercase().contains(pairSearchQuery.lowercase()) ||
                                        u.email.lowercase().contains(pairSearchQuery.lowercase())
                                    }
                                } else {
                                    emptyList()
                                }

                                if (filteredPairCandidates.isNotEmpty()) {
                                    Text("খোঁজা হয়েছে (সার্চ রেজাল্ট):", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                                        items(filteredPairCandidates) { u ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clickable {
                                                        reqMail = u.email
                                                        pairSearchQuery = "" // Reset query after selection
                                                    },
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(u.name.take(1).uppercase(), color = Color.White, fontSize = 11.sp)
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(u.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                                        Text(u.email, fontSize = 10.sp, color = Color.LightGray)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

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

                    "password" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🔑 পাসওয়ার্ড (Access Key) পরিবর্তন করুণঃ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = currentPass,
                                onValueChange = { currentPass = it },
                                label = { Text("বর্তমান পাসওয়ার্ড") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = newPass,
                                onValueChange = { newPass = it },
                                label = { Text("নতুন পাসওয়ার্ড") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = confirmPass,
                                onValueChange = { confirmPass = it },
                                label = { Text("নতুন পাসওয়ার্ড নিশ্চিত করুণ") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                                        Toast.makeText(context, "সবগুলো ঘর পূরণ করুণ", Toast.LENGTH_SHORT).show()
                                    } else if (newPass != confirmPass) {
                                        Toast.makeText(context, "নতুন পাসওয়ার্ড দুইটি মিলেনি", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.changePassword(currentPass, newPass, {
                                            Toast.makeText(context, "পাসওয়ার্ড সফলভাবে পরিবর্তন করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                            currentPass = ""
                                            newPass = ""
                                            confirmPass = ""
                                        }, { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                        })
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("পরিবর্তন নিশ্চিত করুণ", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    "language" -> {
                        val appLanguage by viewModel.appLanguage.collectAsState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (appLanguage == "en") "Select App Language:" else "অ্যাপের ভাষা নির্বাচন করুনঃ",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            // English Option
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.changeLanguage("en") },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (appLanguage == "en") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = if (appLanguage == "en") androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = appLanguage == "en",
                                        onClick = { viewModel.changeLanguage("en") }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("English", fontWeight = FontWeight.Bold)
                                        Text("Set app interface to English", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                            
                            // Bengali Option
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.changeLanguage("bn") },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (appLanguage == "bn") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = if (appLanguage == "bn") androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = appLanguage == "bn",
                                        onClick = { viewModel.changeLanguage("bn") }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("বাংলা (Bengali)", fontWeight = FontWeight.Bold)
                                        Text("অ্যাপের ভাষা বাংলায় পরিবর্তন করুন", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }

                    "version" -> {
                        UserVersionHistoryScreen(viewModel = viewModel)
                    }

                    "applock" -> {
                        val isLockEnabled by viewModel.isAppLockEnabled.collectAsState()
                        val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
                        val currentPIN by viewModel.appLockPIN.collectAsState()

                        var inChangePinMode by remember { mutableStateOf(currentPIN == null) }
                        var setupPIN by remember { mutableStateOf("") }
                        var confirmSetupPIN by remember { mutableStateOf("") }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "🔒 অ্যাপ লক সেটিংস (App Lock)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (inChangePinMode) {
                                Text(
                                    text = "অ্যাপের নিরাপত্তা নিশ্চিত করতে ৪-সংখ্যার PIN সেট করুন। ফোন লক না থাকলেও এটি আপনার চ্যাট সুরক্ষিত রাখবে।",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )

                                OutlinedTextField(
                                    value = setupPIN,
                                    onValueChange = { if (it.length <= 4) setupPIN = it.filter { char -> char.isDigit() } },
                                    label = { Text("৪-সংখ্যার নতুন PIN লিখুন") },
                                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                                    ),
                                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth().testTag("app_lock_setup_pin"),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = confirmSetupPIN,
                                    onValueChange = { if (it.length <= 4) confirmSetupPIN = it.filter { char -> char.isDigit() } },
                                    label = { Text("নতুন PIN নিশ্চিত করতে পুনরায় লিখুন") },
                                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                                    ),
                                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth().testTag("app_lock_confirm_pin"),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (currentPIN != null) {
                                        OutlinedButton(
                                            onClick = { inChangePinMode = false },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("বাতিল")
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            if (setupPIN.length != 4) {
                                                Toast.makeText(context, "PIN অবশ্যই ৪-সংখ্যার হতে হবে!", Toast.LENGTH_SHORT).show()
                                            } else if (setupPIN != confirmSetupPIN) {
                                                Toast.makeText(context, "উভয় PIN মেলেনি!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.setAppLockPIN(setupPIN)
                                                viewModel.setAppLockEnabled(true)
                                                inChangePinMode = false
                                                setupPIN = ""
                                                confirmSetupPIN = ""
                                                Toast.makeText(context, "PIN সফলভাবে সেট করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f).testTag("app_lock_save_pin_button")
                                    ) {
                                        Text("সংরক্ষণ করুন")
                                    }
                                }
                            } else {
                                // Lock is configured, show settings
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("অ্যাপ লক চালু করুন", fontWeight = FontWeight.Bold)
                                                Text("অ্যাপ চালু বা পুনরায় ওপেন করার সময় লক স্ক্রিন দেখাবে", fontSize = 11.sp, color = Color.Gray)
                                            }
                                            Switch(
                                                checked = isLockEnabled,
                                                onCheckedChange = { viewModel.setAppLockEnabled(it) },
                                                modifier = Modifier.testTag("app_lock_enabled_switch")
                                            )
                                        }

                                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("ফিঙ্গারপ্রিন্ট / ফেস লক", fontWeight = FontWeight.Bold)
                                                Text("ডিভাইসের বায়োমেট্রিক বায়োসেন্সর দিয়ে আনলক করুন", fontSize = 11.sp, color = Color.Gray)
                                            }
                                            Switch(
                                                checked = isBiometricEnabled,
                                                onCheckedChange = { viewModel.setBiometricEnabled(it) },
                                                enabled = isLockEnabled,
                                                modifier = Modifier.testTag("app_lock_biometric_switch")
                                            )
                                        }

                                        if (isLockEnabled) {
                                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

                                            val currentTimeout by viewModel.appLockTimeoutMs.collectAsState()
                                            val timeoutOptions = listOf(
                                                0L to "তাত্ক্ষণিকভাবে",
                                                30000L to "৩০ সেকেন্ড",
                                                60000L to "১ মিনিট",
                                                300000L to "৫ মিনিট",
                                                600000L to "১০ মিনিট"
                                            )

                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("স্বয়ংক্রিয় লক টাইমাউট", fontWeight = FontWeight.Bold)
                                                Text("কতক্ষণ পরে অ্যাপ স্বয়ংক্রিয়ভাবে লক হবে তা নির্ধারণ করুন", fontSize = 11.sp, color = Color.Gray)

                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    timeoutOptions.forEach { (ms, label) ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable { viewModel.setAppLockTimeout(ms) }
                                                                .padding(vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            RadioButton(
                                                                selected = currentTimeout == ms,
                                                                onClick = { viewModel.setAppLockTimeout(ms) },
                                                                modifier = Modifier.testTag("app_lock_timeout_$ms")
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(label, fontSize = 13.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { inChangePinMode = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(Icons.Default.VpnKey, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("PIN পরিবর্তন করুন (Change PIN)")
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.setAppLockPIN(null)
                                        Toast.makeText(context, "অ্যাপ লক সফলভাবে বন্ধ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("অ্যাপ লক নিষ্ক্রিয় করুন (Turn Off)")
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

@Composable
fun LocalCameraPreview(modifier: Modifier = Modifier) {
    var camera by remember { mutableStateOf<Camera?>(null) }
    
    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        try {
                            val cam = try {
                                Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT)
                            } catch (e: Exception) {
                                Camera.open()
                            }
                            camera = cam
                            cam.setPreviewDisplay(holder)
                            cam.setDisplayOrientation(90)
                            cam.startPreview()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                        // Safe to ignore or re-start preview
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        try {
                            camera?.stopPreview()
                            camera?.release()
                            camera = null
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                })
            }
        },
        modifier = modifier,
        onRelease = {
            try {
                camera?.stopPreview()
                camera?.release()
                camera = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    )
}

@Composable
fun RealTimeMicrophoneVisualizer(bars: List<Float>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        bars.forEach { barHeight ->
            val animatedHeight by animateFloatAsState(
                targetValue = barHeight,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "barHeight"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(6.dp)
                    .height((40.dp * animatedHeight) + 8.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF38BDF8), Color(0xFFE040FB))
                        ),
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────
//  CALL MANAGING SINK (Incoming & Active Call)
// ─────────────────────────────────────────────
@Composable
fun CallManagerOverlay(viewModel: EchoChatViewModel) {
    val localCtx = LocalContext.current
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

    if (callState is CallState.Idle) return

    val allUsers by viewModel.allUsers.collectAsState()
    val chatUser by viewModel.currentChatUser.collectAsState()

    val backgroundClickModifier = Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {}
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (callState) {
            is CallState.Outgoing -> {
                val state = callState as CallState.Outgoing
                val partnerName = state.partnerName
                val peerUser = chatUser ?: allUsers.find { it.name == partnerName }
                val peerPhotoUrl = peerUser?.photoUrl

                Surface(
                    modifier = Modifier.fillMaxSize().then(backgroundClickModifier),
                    color = Color(0xFF0F172A)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Full screen peer photo background for video calls
                        if (state.callType == "video" && !peerPhotoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = peerPhotoUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.55f))
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (state.callType == "video") "📹 ভিডিও কল শুরু হচ্ছে..." else "📞 অডিও কল শুরু হচ্ছে...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.height(40.dp))

                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .graphicsLayer(
                                            scaleX = pulseScale1,
                                            scaleY = pulseScale1,
                                            alpha = pulseAlpha1
                                        )
                                        .background(Color(0xFFE040FB).copy(alpha = 0.35f), CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .graphicsLayer(
                                            scaleX = pulseScale2,
                                            scaleY = pulseScale2,
                                            alpha = pulseAlpha2
                                        )
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.5f), CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(getAnimatedAccentColor()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!peerPhotoUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = peerPhotoUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = removeLinkAndLockFromName(state.partnerName).take(2).uppercase(),
                                            fontSize = 36.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = removeLinkAndLockFromName(state.partnerName),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("ডায়াল হচ্ছে (Calling)...", color = Color.Gray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(80.dp))

                            FloatingActionButton(
                                onClick = { viewModel.cancelOutgoingCall() },
                                containerColor = Color.Red,
                                contentColor = Color.White,
                                shape = CircleShape,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(Icons.Filled.CallEnd, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }

            is CallState.Incoming -> {
                val state = callState as CallState.Incoming
                val partnerName = state.partnerName
                val peerUser = allUsers.find { it.name == partnerName }
                val peerPhotoUrl = peerUser?.photoUrl

                Surface(
                    modifier = Modifier.fillMaxSize().then(backgroundClickModifier),
                    color = Color(0xFF0F172A)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Full screen peer photo background for video calls
                        if (state.callType == "video" && !peerPhotoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = peerPhotoUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.55f))
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (state.callType == "video") "📹 ইনকামিং ভিডিও কল" else "📞 ইনকামিং অডিও কল",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(40.dp))

                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .graphicsLayer(
                                            scaleX = pulseScale1,
                                            scaleY = pulseScale1,
                                            alpha = pulseAlpha1
                                        )
                                        .background(Color(0xFF10B981).copy(alpha = 0.35f), CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .graphicsLayer(
                                            scaleX = pulseScale2,
                                            scaleY = pulseScale2,
                                            alpha = pulseAlpha2
                                        )
                                        .background(getAnimatedAccentColor().copy(alpha = 0.5f), CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(getAnimatedAccentColor()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!peerPhotoUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = peerPhotoUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = removeLinkAndLockFromName(state.partnerName).take(2).uppercase(),
                                            fontSize = 36.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = removeLinkAndLockFromName(state.partnerName),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.height(80.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(48.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FloatingActionButton(
                                    onClick = { viewModel.rejectIncomingCall() },
                                    containerColor = Color.Red,
                                    contentColor = Color.White,
                                    shape = CircleShape,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Icon(Icons.Filled.CallEnd, null, tint = Color.White, modifier = Modifier.size(32.dp))
                                }
                                FloatingActionButton(
                                    onClick = { viewModel.acceptIncomingCall() },
                                    containerColor = Color(0xFF25D366),
                                    contentColor = Color.White,
                                    shape = CircleShape,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Icon(
                                        imageVector = if (state.callType == "video") Icons.Filled.Videocam else Icons.Filled.Call,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is CallState.Connected -> {
                val state = callState as CallState.Connected
                val partnerName = state.partnerName
                val peerUser = chatUser ?: allUsers.find { it.name == partnerName }
                val peerPhotoUrl = peerUser?.photoUrl

                val bars = remember { mutableStateListOf(0.2f, 0.4f, 0.3f, 0.5f, 0.2f, 0.6f, 0.4f, 0.3f, 0.2f) }

                LaunchedEffect(isMuted) {
                    if (isMuted) {
                        for (i in 0 until bars.size) {
                            bars[i] = 0.1f
                        }
                        return@LaunchedEffect
                    }
                    
                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                            localCtx,
                            android.Manifest.permission.RECORD_AUDIO
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        return@LaunchedEffect
                    }

                    val bufferSize = AudioRecord.getMinBufferSize(
                        8000,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    if (bufferSize <= 0) return@LaunchedEffect

                    var audioRecord: AudioRecord? = null
                    var audioTrack: AudioTrack? = null

                    try {
                        audioRecord = AudioRecord(
                            MediaRecorder.AudioSource.MIC,
                            8000,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            bufferSize
                        )

                        audioTrack = AudioTrack(
                            AudioManager.STREAM_VOICE_CALL,
                            8000,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            bufferSize,
                            AudioTrack.MODE_STREAM
                        )

                        if (audioRecord.state == AudioRecord.STATE_INITIALIZED &&
                            audioTrack.state == AudioTrack.STATE_INITIALIZED) {
                            
                            audioRecord.startRecording()
                            audioTrack.play()
                            val buffer = ShortArray(bufferSize)

                            while (isActive) {
                                val read = audioRecord.read(buffer, 0, buffer.size)
                                if (read > 0) {
                                    if (!isMuted) {
                                        // 1. Play back recorded audio on speaker
                                        audioTrack.write(buffer, 0, read)

                                        // 2. Compute amplitude for visualizer bars
                                        var max = 0
                                        for (i in 0 until read) {
                                            val absVal = Math.abs(buffer[i].toInt())
                                            if (absVal > max) max = absVal
                                        }
                                        val targetAmp = (max / 32768f).coerceIn(0f, 1f)
                                        for (j in 0 until bars.size) {
                                            bars[j] = (targetAmp * (0.3f + (0.7f * java.util.Random().nextFloat()))).coerceIn(0.1f, 1.0f)
                                        }
                                    }
                                } else {
                                    delay(100)
                                }
                                yield()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            audioRecord?.stop()
                        } catch (e: Exception) {}
                        try {
                            audioRecord?.release()
                        } catch (e: Exception) {}
                        try {
                            audioTrack?.stop()
                        } catch (e: Exception) {}
                        try {
                            audioTrack?.release()
                        } catch (e: Exception) {}
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize().then(backgroundClickModifier),
                    color = Color(0xFF0F172A)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 1. Full-screen visual of peer / partner (উপার্জন / অপরজন)
                        if (!peerPhotoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = peerPhotoUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Semi-transparent overlay to ensure controls and text are super readable
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.55f))
                            )
                        } else {
                            // Fancy fallback background
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(240.dp)
                                        .graphicsLayer(
                                            scaleX = pulseScale1,
                                            scaleY = pulseScale1,
                                            alpha = pulseAlpha1 * 0.4f
                                        )
                                        .background(getAnimatedAccentColor().copy(alpha = 0.2f), CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(190.dp)
                                        .graphicsLayer(
                                            scaleX = pulseScale2,
                                            scaleY = pulseScale2,
                                            alpha = pulseAlpha2 * 0.6f
                                        )
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.25f), CircleShape)
                                )
                            }
                        }

                        // Centered Identity overlay (always visible clearly)
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(getAnimatedAccentColor())
                                    .border(3.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!peerPhotoUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = peerPhotoUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = removeLinkAndLockFromName(state.partnerName).take(2).uppercase(),
                                        color = Color.White,
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = removeLinkAndLockFromName(state.partnerName),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$minutes:$seconds",
                                color = Color(0xFF38BDF8),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            if (state.callType == "audio") {
                                RealTimeMicrophoneVisualizer(bars = bars, modifier = Modifier.padding(horizontal = 40.dp))
                            } else {
                                Text(
                                    text = "ভিডিও কল চলমান (Video Call Active)",
                                    color = Color(0xFF10B981),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 2. Floating PIP Self Video Feed (ডানকোনায় উপরে নিজের ছবি দেখবে)
                        if (state.callType == "video") {
                            Box(
                                modifier = Modifier
                                    .padding(top = 48.dp, end = 20.dp)
                                    .size(width = 110.dp, height = 150.dp)
                                    .align(Alignment.TopEnd)
                                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1E293B))
                                    .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            ) {
                                if (!isCameraOff) {
                                    LocalCameraPreview(modifier = Modifier.fillMaxSize())
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Filled.VideocamOff,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "ক্যামেরা বন্ধ",
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Floating Bottom Call Controls Bar
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(vertical = 32.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.toggleCallMute() },
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(if (isMuted) Color.White.copy(alpha = 0.25f) else Color.Transparent, CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            FloatingActionButton(
                                onClick = { viewModel.endActiveCall() },
                                containerColor = Color.Red,
                                contentColor = Color.White,
                                shape = CircleShape,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(Icons.Filled.CallEnd, null, modifier = Modifier.size(30.dp))
                            }

                            if (state.callType == "video") {
                                IconButton(
                                    onClick = { viewModel.toggleCallCamera() },
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(if (isCameraOff) Color.White.copy(alpha = 0.25f) else Color.Transparent, CircleShape)
                                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isCameraOff) Icons.Filled.VideocamOff else Icons.Filled.Videocam,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
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
}

fun parseColorString(colorStr: String): Color {
    return try {
        if (colorStr.startsWith("#")) {
            Color(android.graphics.Color.parseColor(colorStr))
        } else {
            when (colorStr.lowercase().trim()) {
                "gold" -> Color(0xFFFFB300)
                "blue" -> Color(0xFF2196F3)
                "red" -> Color(0xFFE53935)
                "green" -> Color(0xFF4CAF50)
                "black" -> Color(0xFF333333)
                "white" -> Color(0xFFFFFFFF)
                "purple" -> Color(0xFF9C27B0)
                "pink" -> Color(0xFFE91E63)
                "orange" -> Color(0xFFFF9800)
                "yellow" -> Color(0xFFFFEB3B)
                else -> {
                    if (colorStr.matches(Regex("[0-9a-fA-F]{6,8}"))) {
                        Color(android.graphics.Color.parseColor("#$colorStr"))
                    } else {
                        Color.Gray
                    }
                }
            }
        }
    } catch (e: Exception) {
        Color.Gray
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVersionPanel(viewModel: EchoChatViewModel) {
    val context = LocalContext.current
    var versionNumber by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var changes by remember { mutableStateOf("") }
    val forceUpdate = false
    var editingVersion by remember { mutableStateOf<AppVersionInfo?>(null) }

    val versions by viewModel.versions.collectAsState()
    var isSending by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (editingVersion != null) "✏️ অ্যাপ সংস্করণ সম্পাদনা করুন" else "🚀 নতুন অ্যাপ সংস্করণ প্রকাশ করুন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = versionNumber,
                    onValueChange = { versionNumber = it },
                    label = { Text("ভার্সন নাম্বার (যেমন: 1.0.1)") },
                    placeholder = { Text("1.0.1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("টাইটেল/বিবরণ") },
                    placeholder = { Text("নতুন ফিচার এবং বাগ ফিক্স!") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("ডাউনলোড লিংক") },
                    placeholder = { Text("https://example.com/download") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = changes,
                    onValueChange = { changes = it },
                    label = { Text("কি কি পরিবর্তন করা হয়েছে (Changes)") },
                    placeholder = { Text("১. নোটিফিকেশন সমস্যা সমাধান\n২. নতুন ডিজাইন যোগ") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (editingVersion != null) {
                        OutlinedButton(
                            onClick = {
                                editingVersion = null
                                versionNumber = ""
                                title = ""
                                link = ""
                                changes = ""
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSending
                        ) {
                            Text("বাতিল ❌", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            if (versionNumber.isEmpty() || title.isEmpty() || link.isEmpty() || changes.isEmpty()) {
                                Toast.makeText(context, "অনুগ্রহ করে সবগুলো ঘর পূরণ করুন", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSending = true
                            val verNum = versionNumber.trim()
                            val tText = title.trim()
                            val lLink = link.trim()
                            val cChanges = changes.trim()
                            val ev = editingVersion
                            if (ev != null) {
                                viewModel.editVersion(
                                    oldMessageId = ev.messageId ?: "",
                                    versionNumber = verNum,
                                    title = tText,
                                    link = lLink,
                                    changes = cChanges,
                                    forceUpdate = forceUpdate,
                                    onSuccess = {
                                        isSending = false
                                        Toast.makeText(context, "ভার্সন সফলভাবে পরিবর্তন করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                        versionNumber = ""
                                        title = ""
                                        link = ""
                                        changes = ""
                                        editingVersion = null
                                    },
                                    onError = { err ->
                                        isSending = false
                                        Toast.makeText(context, "ত্রুটি: $err", Toast.LENGTH_LONG).show()
                                    }
                                )
                            } else {
                                viewModel.sendVersionUpdateToSheet(
                                    versionNumber = verNum,
                                    title = tText,
                                    link = lLink,
                                    changes = cChanges,
                                    forceUpdate = forceUpdate,
                                    onSuccess = {
                                        isSending = false
                                        Toast.makeText(context, "ভার্সন সফলভাবে আপডেট করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                        versionNumber = ""
                                        title = ""
                                        link = ""
                                        changes = ""
                                    },
                                    onError = { err ->
                                        isSending = false
                                        Toast.makeText(context, "ত্রুটি: $err", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        modifier = Modifier.weight(if (editingVersion != null) 1.2f else 1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSending
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Text(if (editingVersion != null) "পরিবর্তন করুন ✏️" else "সেন্ড করুন 🚀", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // List of previous versions
        Text(
            text = "📜 পূর্ববর্তী সংস্করণসমূহ (Version History)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (versions.isEmpty()) {
            Text("কোনো পূর্ববর্তী সংস্করণ পাওয়া যায়নি।", color = Color.Gray, fontSize = 13.sp)
        } else {
            versions.forEach { ver ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, Color.DarkGray)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ভার্সন: ${ver.versionNumber}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 15.sp
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        editingVersion = ver
                                        versionNumber = ver.versionNumber
                                        title = ver.title
                                        link = ver.link
                                        changes = ver.changes ?: ""
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "সম্পাদনা",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        ver.messageId?.let { msgId ->
                                            isSending = true
                                            viewModel.deleteVersion(
                                                messageId = msgId,
                                                onSuccess = {
                                                    isSending = false
                                                    Toast.makeText(context, "ভার্সন সফলভাবে মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err ->
                                                    isSending = false
                                                    Toast.makeText(context, "ত্রুটি: $err", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "মুছে ফেলুন",
                                        tint = Color.Red,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = ver.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        if (!ver.changes.isNullOrBlank()) {
                            Text(
                                text = "পরিবর্তনসমূহ:\n${ver.changes}",
                                fontSize = 12.sp,
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = "লিংক: ${ver.link}",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            maxLines = 1,
                            modifier = Modifier.clickable {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(ver.link))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "লিংক ওপেন করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserVersionHistoryScreen(viewModel: EchoChatViewModel) {
    val context = LocalContext.current
    val versions by viewModel.versions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "🚀 অ্যাপ সংস্করণ তালিকা (Version History)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "নিচের তালিকা থেকে যেকোনো টাইটেলের উপর ক্লিক করে সরাসরি লিংক ওপেন করতে পারবেন এবং সেই ভার্সনটি ডাউনলোড করতে পারবেন।",
            fontSize = 12.sp,
            color = Color.LightGray,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (versions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("কোনো সংস্করণ তালিকা পাওয়া যায়নি।", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            versions.forEach { ver ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, Color.DarkGray)
                ) {
                    Column(
                        modifier = Modifier
                            .clickable {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(ver.link))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "লিংক ওপেন করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "সংস্করণ: ${ver.versionNumber}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            if (ver.forceUpdate) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Red.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("বাধ্যতামূলক", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            text = ver.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        if (!ver.changes.isNullOrBlank()) {
                            Text(
                                text = "পরিবর্তনসমূহ:\n${ver.changes}",
                                fontSize = 12.sp,
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = ver.link,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppLockScreenOverlay(viewModel: EchoChatViewModel) {
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val savedPIN by viewModel.appLockPIN.collectAsState()

    var enteredPIN by remember { mutableStateOf("") }
    var pinErrorMsg by remember { mutableStateOf<String?>(null) }
    var shakeTrigger by remember { mutableStateOf(false) }

    // Biometric function
    fun triggerBiometric() {
        if (activity != null && isBiometricEnabled) {
            val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
            val biometricPrompt = androidx.biometric.BiometricPrompt(activity, executor,
                object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        // Silent error, let user use PIN instead
                    }

                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        viewModel.setAppLocked(false)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                    }
                })

            val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("EchoChat Unlock")
                .setSubtitle("Authenticate using fingerprint or face")
                .setNegativeButtonText("Use PIN")
                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()

            try {
                biometricPrompt.authenticate(promptInfo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Automatically trigger biometric on screen load
    LaunchedEffect(Unit) {
        triggerBiometric()
    }

    val shakeOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (shakeTrigger) 15f else 0f,
        animationSpec = androidx.compose.animation.core.keyframes {
            durationMillis = 300
            0f at 0
            -15f at 50
            15f at 100
            -10f at 150
            10f at 200
            -5f at 250
            0f at 300
        },
        finishedListener = {
            shakeTrigger = false
        },
        label = "shake_animation"
    )

    // Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(enabled = true, onClick = {}) // Block behind clicks
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().offset(x = shakeOffset.dp)
        ) {
            // Animated lock header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "App Locked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "EchoChat সুরক্ষিত",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = pinErrorMsg ?: "অ্যাপটি আনলক করতে ৪-সংখ্যার PIN লিখুন",
                style = MaterialTheme.typography.bodyMedium,
                color = if (pinErrorMsg != null) MaterialTheme.colorScheme.error else Color.Gray,
                fontWeight = if (pinErrorMsg != null) FontWeight.SemiBold else FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(32.dp))

            // PIN Dots Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val filled = i < enteredPIN.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = if (filled) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Numeric Keypad 1-9
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                val numRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )

                for (row in numRows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (num in row) {
                            KeypadButton(
                                text = num,
                                onClick = {
                                    if (enteredPIN.length < 4) {
                                        pinErrorMsg = null
                                        enteredPIN += num
                                        if (enteredPIN.length == 4) {
                                            if (enteredPIN == savedPIN) {
                                                viewModel.setAppLocked(false)
                                            } else {
                                                shakeTrigger = true
                                                pinErrorMsg = "ভুল PIN, আবার চেষ্টা করুন"
                                                enteredPIN = ""
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("pin_key_$num")
                            )
                        }
                    }
                }

                // Bottom Row (Biometric, 0, Backspace)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Left: Biometric trigger
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBiometricEnabled) {
                            IconButton(
                                onClick = { triggerBiometric() },
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).testTag("pin_key_biometric")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Use Biometrics",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Center: 0
                    KeypadButton(
                        text = "0",
                        onClick = {
                            if (enteredPIN.length < 4) {
                                pinErrorMsg = null
                                enteredPIN += "0"
                                if (enteredPIN.length == 4) {
                                    if (enteredPIN == savedPIN) {
                                        viewModel.setAppLocked(false)
                                    } else {
                                        shakeTrigger = true
                                        pinErrorMsg = "ভুল PIN, আবার চেষ্টা করুন"
                                        enteredPIN = ""
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("pin_key_0")
                    )

                    // Right: Backspace
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                if (enteredPIN.isNotEmpty()) {
                                    enteredPIN = enteredPIN.dropLast(1)
                                }
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).testTag("pin_key_backspace")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "Backspace",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
