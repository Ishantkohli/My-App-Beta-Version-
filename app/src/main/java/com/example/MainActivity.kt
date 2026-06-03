package com.example

import android.os.Build
import android.os.Bundle
import kotlinx.coroutines.delay
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge drawing
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBg)
                ) { innerPadding ->
                    MainControllerConsole(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        IkVoiceService.isAppInForeground = true
    }

    override fun onStop() {
        super.onStop()
        IkVoiceService.isAppInForeground = false
        try {
            stopService(Intent(this, IkVoiceService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainControllerConsole(
    modifier: Modifier = Modifier,
    viewModel: SystemViewModel = viewModel()
) {
    val activeTab by viewModel.activeTab.collectAsState()
    val isSleeping by viewModel.isSleeping.collectAsState()
    val currentProfile by viewModel.currentProfile.collectAsState()
    val showBootIntro by viewModel.showBootIntro.collectAsState()

    var isDrawerOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Soft minimal tech mesh lines
        MinimalSlateGridBackground()

        if (showBootIntro) {
            BootSplashIntroScreen(
                viewModel = viewModel,
                onDismiss = { viewModel.setShowBootIntro(false) }
            )
        } else if (isSleeping) {
            // Full screen Deep Stealth Sleep view
            StealthSleepScreen(
                onWakeTrigger = { viewModel.toggleSleepMode() }
            )
        } else {
            // Main Standard System Dashboard
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp)
            ) {
                // Top Custom Modern Bar (Adaptive Model info)
                MinimalStatusHeader(
                    viewModel = viewModel,
                    onMenuClick = { isDrawerOpen = !isDrawerOpen },
                    onInstantOptimize = { viewModel.triggerRamClean() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Integrated real-time freq status HUD (Interactive Governor telemetry)
                FrequencyHudCard(
                    viewModel = viewModel,
                    currentProfile = currentProfile
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable/Dynamic Contextual Tab Layout area
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    when (activeTab) {
                        ActiveTab.PERFORMANCE -> PerformanceTunerTab(viewModel)
                        ActiveTab.RAM -> RamOptimizerTab(viewModel)
                        ActiveTab.APPS -> AppManagerTab(viewModel)
                        ActiveTab.TERMINAL -> TerminalTab(viewModel)
                        ActiveTab.SLEEP -> {
                            // Dedicated Deep Sleep entry UI inside the dashboard
                            DeepSleepTriggerTab(onSleepTrigger = { viewModel.toggleSleepMode() })
                        }
                        ActiveTab.ABOUT -> AboutTab(viewModel)
                        ActiveTab.FPS -> FpsMeasureTab(viewModel)
                        ActiveTab.VOICE -> IkVoiceTab(viewModel)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Left System navigation drawer overlay background (press anywhere to close menu section)
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isDrawerOpen = false }
                    .testTag("drawer_overlay_backdrop")
            )
        }

        // Navigation drawer side panel layout (animates from left cleanly and smoothly)
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
            ) + fadeIn(animationSpec = tween(220)),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut(animationSpec = tween(180)),
            modifier = Modifier
                .fillMaxHeight()
                .width(280.dp)
                .align(Alignment.CenterStart)
        ) {
            LeftNavigationDrawer(
                activeTab = activeTab,
                onTabSelected = { tab ->
                    viewModel.selectTab(tab)
                    isDrawerOpen = false
                },
                onCloseClick = { isDrawerOpen = false }
            )
        }
    }
}

@Composable
fun BootSplashIntroScreen(
    viewModel: SystemViewModel,
    onDismiss: () -> Unit
) {
    var rebootTrigger by remember { mutableStateOf(0) }
    var startAnim by remember { mutableStateOf(false) }
    
    // Smoothly animate progress using Jetpack Compose's native Choreographer-linked system to lock refresh at 60Hz/native display rates
    val progress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1800, easing = androidx.compose.animation.core.LinearEasing),
        label = "boot_progress"
    )
    
    LaunchedEffect(rebootTrigger) {
        startAnim = false
        delay(50) // Tiny settle time before initiating hardware-synchronized animation
        startAnim = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040608))
            .testTag("boot_intro_screen")
    ) {
        // Overlay a deep space tech background
        MinimalSlateGridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Title and Subtitle Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "IK ",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "BootLoader",
                        color = CyberPurple,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Handle your Phone in your way.",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }

            // Central Dynamic Animation Preview Container (Always Matrix Style A)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF07090C))
                    .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                MatrixBootloaderAnimation(progress)
            }

            // Bottom Loader & Control Area
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Percentage representation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (progress >= 1f) "CALIBRATION SECURE" else "LOADING CORE PARAMETERS...",
                        color = if (progress >= 1f) CyberGreen else CyberCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom glowing progress indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(CyberCyan, CyberPurple)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Drawer control flow buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Re-run animation to let user test fully
                    Button(
                        onClick = { rebootTrigger++ },
                        modifier = Modifier
                            .weight(0.4f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Text(
                            text = "🔄 RE-BOOT",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Proceed into the system control panel
                    val isFinished = progress >= 1f
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.6f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFinished) CyberGreen.copy(alpha = 0.15f) else CyberCyan.copy(alpha = 0.05f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isFinished) CyberGreen else CyberCyan.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = if (isFinished) "ENTER MAIN SYSTEM" else "SKIP INTRO FLOW",
                            color = if (isFinished) CyberGreen else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MatrixBootloaderAnimation(progress: Float) {
    val bootLogs = remember {
        listOf(
            "INIT" to "BOOTSTRAP TELEMETRY PROTOCOLS FOR SYSTEM...",
            "OK" to "DETECTING SOC ARCHITECTURE: SCHEDULER...",
            "OK" to "ESTABLISHING CORE SYSTEM REGISTERS",
            "OK" to "MOUNTING PHYSICAL KERNEL ALLOCATORS...",
            "OK" to "PARSING SYSTEM INSTALLED PACKAGES",
            "OK" to "SCANNING STORAGE CHANNELS...",
            "OK" to "INJECTING ADAPTIVE VOLTAGE INDICES",
            "OK" to "DEEP CACHE TERMINATOR ONLINE",
            "OK" to "TUNING MEMORY PRESSURES FOR DEVICE",
            "OK" to "COMPILING DIMENSITY SPEED TELEMETRY",
            "OK" to "ESTABLISHING STEALTH POWER GATEWAY",
            "OK" to "CALIBRATING LOW-END DEVICE COMPATIBILITY",
            "SECURE" to "STARTUP PROTOCOL REGISTERED. SYSTEM ONLINE!"
        )
    }

    val visibleCount = (progress * (bootLogs.size + 1)).toInt().coerceIn(0, bootLogs.size)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "CONSOLE DIAGNOSTICS BOOTLOG:",
                color = TextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            for (i in 0 until visibleCount) {
                val (tag, message) = bootLogs[i]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    val tagColor = when (tag) {
                        "OK" -> CyberGreen
                        "INIT" -> CyberCyan
                        else -> CyberPurple
                    }
                    Text(
                        text = "[$tag]",
                        color = tagColor,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = message,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 12.sp
                    )
                }
            }

            // Blinking computer console caret at bottom
            val infiniteTransition = rememberInfiniteTransition(label = "caret")
            val blinkAlpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "caret_blink"
            )

            if (visibleCount < bootLogs.size) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ishant_kohli@boot:~$ ",
                        color = CyberCyan,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(11.dp)
                            .background(CyberCyan.copy(alpha = blinkAlpha))
                    )
                }
            }
        }
    }
}



@Composable
fun MinimalSlateGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val step = maxOf(20f, 60.dp.toPx())
        
        // Horizontal and vertical structural guide offsets with fail-safe steps
        var x = 0f
        while (x < width) {
            drawLine(
                color = Color(0xFF1E293B).copy(alpha = 0.12f),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f
            )
            x += step
        }
        
        var y = 0f
        while (y < height) {
            drawLine(
                color = Color(0xFF1E293B).copy(alpha = 0.12f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            y += step
        }
    }
}

@Composable
fun MinimalStatusHeader(
    viewModel: SystemViewModel,
    onMenuClick: () -> Unit,
    onInstantOptimize: () -> Unit
) {
    val model = remember { Build.MODEL.trim().uppercase() }
    val modelParts = remember(model) {
        val words = model.split("\\s+".toRegex())
        if (words.size > 1) {
            val lastWord = words.last()
            val rest = words.dropLast(1).joinToString(" ")
            Pair("$rest ", lastWord)
        } else {
            Pair(model, " ACTIVE")
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Elegant 3-line cyber menu button (hamburger style)
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(DarkSurface)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    .testTag("menu_drawer_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Drawer Menu",
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = "SYSTEM ACTIVE",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = modelParts.first,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = modelParts.second,
                        color = CyberGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "App Build By - Ishant Kohli",
                    color = CyberCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    style = TextStyle(
                        shadow = Shadow(
                            color = CyberCyan.copy(alpha = 0.8f),
                            offset = Offset(0f, 0f),
                            blurRadius = 12f
                        )
                    )
                )
            }
        }

        // Speed telemetry action trigger
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(DarkSurface)
                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                .clickable { onInstantOptimize() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚡",
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun LeftNavigationDrawer(
    activeTab: ActiveTab,
    onTabSelected: (ActiveTab) -> Unit,
    onCloseClick: () -> Unit
) {
    Surface(
        color = DarkSurface,
        border = BorderStroke(1.dp, color = CyberCyan.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(vertical = 16.dp, horizontal = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Drawer Title Area
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "IK BOOTLOADER",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "ACTIVE CONTROL MODULE",
                            color = TextSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = onCloseClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkBg)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Menu",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                // Hardware visual banner within the drawer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBg)
                        .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "ACTIVE DEVICE NODE",
                            color = TextSecondary,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL.uppercase()}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Navigation Items List
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ActiveTab.entries.forEach { tab ->
                        val isSelected = activeTab == tab
                        val itemLabel = when (tab) {
                            ActiveTab.PERFORMANCE -> "System Dashboard"
                            ActiveTab.RAM -> "Ram Sweeper"
                            ActiveTab.APPS -> "App Workspace"
                            ActiveTab.TERMINAL -> "Console Terminal"
                            ActiveTab.SLEEP -> "Stealth Sleep"
                            ActiveTab.ABOUT -> "Device Info"
                            ActiveTab.FPS -> "FPS & Stats Gauge"
                            ActiveTab.VOICE -> "IK Voice"
                        }

                        val itemEmoji = when (tab) {
                            ActiveTab.PERFORMANCE -> "📊"
                            ActiveTab.RAM -> "⚡"
                            ActiveTab.APPS -> "📁"
                            ActiveTab.TERMINAL -> "💻"
                            ActiveTab.SLEEP -> "🌙"
                            ActiveTab.ABOUT -> "ℹ️"
                            ActiveTab.FPS -> "📈"
                            ActiveTab.VOICE -> "🎙️"
                        }

                        val textColor = if (isSelected) Color.White else TextSecondary
                        val borderCol = if (isSelected) CyberCyan.copy(alpha = 0.4f) else Color.Transparent
                        val bgCol = if (isSelected) CyberCyan.copy(alpha = 0.1f) else Color.Transparent

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bgCol)
                                .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                                .clickable {
                                    onTabSelected(tab)
                                }
                                .testTag("drawer_tab_${tab.name.lowercase()}"),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = itemEmoji,
                                    fontSize = 18.sp
                                )

                                Text(
                                    text = itemLabel,
                                    color = textColor,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }

                            // Left Cyber Indicator Accent
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .width(3.dp)
                                        .fillMaxHeight(0.5f)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(CyberCyan)
                                )
                            }
                        }
                    }
                }
            }

            // Footer of list
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.05f))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VERSION 3.2.0",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "NODE RUNTIME",
                        color = CyberGreen,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FrequencyHudCard(
    viewModel: SystemViewModel,
    currentProfile: PerformanceProfile
) {
    val simulatedFreq by viewModel.cpuFrequencySim.collectAsState()
    val simulatedTemp by viewModel.cpuTemperature.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
            .testTag("hardware_hud_header"),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Processor Frequency",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = String.format("%.2f", simulatedFreq),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = "GHz",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Box(
                        modifier = Modifier
                            .background(CyberGreen.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "OPTIMIZED",
                            color = CyberGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dimensity 7300-E | ${String.format("%.1f", simulatedTemp)}°C",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Custom slider visualization corresponding to currently selected mode ratio
            val targetRatio = when(currentProfile) {
                PerformanceProfile.ECO -> 0.35f
                PerformanceProfile.BALANCED -> 0.65f
                PerformanceProfile.OVERDRIVE -> 0.95f
            }
            
            val animatedRatio by animateFloatAsState(
                targetValue = targetRatio,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                label = "freq_slide"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF0F172A))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedRatio)
                        .background(CyberGreen)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Power Eco",
                    color = if (currentProfile == PerformanceProfile.ECO) CyberGreen else TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "Balanced",
                    color = if (currentProfile == PerformanceProfile.BALANCED) CyberGreen else TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "Ultra Turbo",
                    color = if (currentProfile == PerformanceProfile.OVERDRIVE) CyberGreen else TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}

// ---------------------- PERFORMANCE TUNER TAB ----------------------

@Composable
fun PerformanceTunerTab(viewModel: SystemViewModel) {
    val coreUtils by viewModel.coreUtilizations.collectAsState()
    val currentProfile by viewModel.currentProfile.collectAsState()
    val availableRam by viewModel.ramAvailableGb.collectAsState()
    val totalRam by viewModel.ramTotalGb.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val cpuFrequencySim by viewModel.cpuFrequencySim.collectAsState()
    val cpuTemperature by viewModel.cpuTemperature.collectAsState()
    val isRamOptimizing by viewModel.isRamOptimizing.collectAsState()
    val detectedProcessorName by viewModel.detectedProcessorName.collectAsState()
    val detectedProcessorSpeed by viewModel.detectedProcessorSpeed.collectAsState()
    val hiddenPackages by viewModel.hiddenPackages.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("performance_tuner_tab"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // RECHARTS TELEMETRY INSTRUMENT PANEL
        item {
            RechartsTelemetryDashboard(viewModel)
        }

        // REAL-TIME CONSOLE EVENT LOG WIDGET
        item {
            ConsoleEventLogView(viewModel)
        }

        // CARD 1: SYSTEM STATUS CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("system_status_card"),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan)
                            )
                            Text(
                                text = "SYSTEM STATUS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        // Status badge indicating everything is pristine or warning
                        val tempStatus = when {
                            cpuTemperature > 45f -> "THERMAL WARNING"
                            else -> "OPTIMAL HEAT"
                        }
                        val statusColor = when {
                            cpuTemperature > 45f -> CyberRed
                            else -> CyberGreen
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tempStatus,
                                fontSize = 8.sp,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Real-time active kernel telemetry node & device parameters",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Hardware stats layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Temp Pod
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🌡️ THERMAL", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            val tempColor = if (cpuTemperature > 43f) CyberRed else if (cpuTemperature > 38f) CyberAmber else CyberGreen
                            Text(
                                text = "${String.format("%.1f", cpuTemperature)} °C",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = tempColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        // Frequency/Clock Pod
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("⚡ CPU CLOCK", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format("%.2f", cpuFrequencySim)} GHz",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        // Profile Pod
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                                // Custom ripple support for quick overlay activation
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🛡️ GOVERNOR", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            val profileShort = when (currentProfile) {
                                PerformanceProfile.ECO -> "ECO"
                                PerformanceProfile.BALANCED -> "BALANCED"
                                PerformanceProfile.OVERDRIVE -> "OVERDRIVE"
                            }
                            val profileColor = when (currentProfile) {
                                PerformanceProfile.ECO -> CyberGreen
                                PerformanceProfile.BALANCED -> CyberCyan
                                PerformanceProfile.OVERDRIVE -> CyberRed
                            }
                            Text(
                                text = profileShort,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = profileColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // RAM utilization sub-section
                    val totalRamSafety = if (totalRam <= 0.1f) 8.0f else totalRam
                    val usedRam = (totalRamSafety - availableRam).coerceAtLeast(0.1f)
                    val progressPct = (usedRam / totalRamSafety).coerceIn(0.01f, 1.0f)
                    val progressColor = if (progressPct > 0.85f) CyberRed else if (progressPct > 0.65f) CyberAmber else CyberGreen
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RAM MEMORY ALLOCATION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${String.format("%.2f", usedRam)} / ${String.format("%.1f", totalRamSafety)} GB (${String.format("%.0f", progressPct * 100)}%)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = progressColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progressPct },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = progressColor,
                            trackColor = Color(0xFF0F172A)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Octa core active states
                    Text(
                        text = "OCTA-CORE CORRELATION STATE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            for (i in 0..3) {
                                CoreStatusIndicator(index = i, load = coreUtils.getOrNull(i) ?: 0.2f)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            for (i in 4..7) {
                                CoreStatusIndicator(index = i, load = coreUtils.getOrNull(i) ?: 0.2f)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    // Purge GC action button
                    Button(
                        onClick = { viewModel.triggerRamClean() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("clean_ram_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.4f))
                    ) {
                        if (isRamOptimizing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CyberGreen, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PURGING ACTIVE BUFFER STATES...",
                                color = CyberGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Text(
                                text = "PURGE SYSTEM HEAP CACHE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
        
        // CARD 2: BLOATWARE MANAGER CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bloatware_manager_card"),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CyberAmber)
                            )
                            Text(
                                text = "BLOATWARE MANAGER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberAmber,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        val bloatCount = installedApps.count { it.isBloatware }
                        val hiddenCount = installedApps.count { it.isBloatware && hiddenPackages.contains(it.packageName) }
                        val activeBloatCount = (bloatCount - hiddenCount).coerceAtLeast(0)
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (activeBloatCount > 0) CyberRed.copy(alpha = 0.1f) else CyberGreen.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (activeBloatCount > 0) "$activeBloatCount DEFECTIVE" else "INTEGRITY SECURE",
                                fontSize = 8.sp,
                                color = if (activeBloatCount > 0) CyberRed else CyberGreen,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Scans pre-installed telemetry handles & secondary background services",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // App counters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val totalApps = installedApps.size
                        val systemApps = installedApps.count { it.isSystem }
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.015f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("SCANNED REGISTRY", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$totalApps Packages",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.015f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("RECLAIM RISK", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(2.dp))
                            val bloatCount = installedApps.count { it.isBloatware }
                            Text(
                                text = "$bloatCount Candidates",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bloatCount > 0) CyberAmber else CyberGreen
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    // Render list of up to 3 bloatware candidates
                    val bloatList = installedApps.filter { it.isBloatware }.take(3)
                    if (bloatList.isNotEmpty()) {
                        Text(
                            text = "DETECTOR RISK HEURISTICS:",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0C1017), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            bloatList.forEach { app ->
                                val isHidden = hiddenPackages.contains(app.packageName)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = app.packageName,
                                            fontSize = 8.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isHidden) CyberGreen.copy(alpha = 0.1f) else CyberRed.copy(alpha = 0.1f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isHidden) "FROZEN" else "UNSAFE",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isHidden) CyberGreen else CyberRed,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty states handling
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0C1017), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✨ Zero residual manufacturer bloatware found on platform.",
                                fontSize = 10.sp,
                                color = CyberGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    // Redirection Action Button
                    Button(
                        onClick = { viewModel.selectTab(ActiveTab.APPS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("manage_bloatware_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "LAUNCH DEEP APP WORKSPACE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
        
        // CARD 3: PROCESSOR OPTIMIZER CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("processor_optimizer_card"),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CyberRed)
                            )
                            Text(
                                text = "PROCESSOR OPTIMIZER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberRed,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Text(
                            text = "8-CORE ACTIVE",
                            fontSize = 8.sp,
                            color = CyberRed,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberRed.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Custom schedule governor to scale Dimensity silicon frequency",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Silicon hardware description
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.015f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.015f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(CyberRed.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤖", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (detectedProcessorName.isNotBlank()) detectedProcessorName else "MediaTek Dimensity Custom SoC",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Calculated Target: ${if (detectedProcessorSpeed.isNotBlank()) detectedProcessorSpeed else "2.50 GHz Max Scope"}",
                                fontSize = 9.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    // Profile dynamic list options
                    PerformanceProfile.entries.forEach { profile ->
                        val isSelected = currentProfile == profile
                        val optionBg = if (isSelected) Color.White.copy(alpha = 0.03f) else Color.Transparent
                        val optionBorder = if (isSelected) {
                            when (profile) {
                                PerformanceProfile.ECO -> CyberGreen.copy(alpha = 0.4f)
                                PerformanceProfile.BALANCED -> CyberCyan.copy(alpha = 0.4f)
                                PerformanceProfile.OVERDRIVE -> CyberRed.copy(alpha = 0.4f)
                            }
                        } else Color.White.copy(alpha = 0.02f)
                        
                        val accentColor = when (profile) {
                            PerformanceProfile.ECO -> CyberGreen
                            PerformanceProfile.BALANCED -> CyberCyan
                            PerformanceProfile.OVERDRIVE -> CyberRed
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(optionBg)
                                .border(1.dp, optionBorder, RoundedCornerShape(14.dp))
                                .clickable { viewModel.selectProfile(profile) }
                                .padding(12.dp)
                                .testTag("profile_card_${profile.name.lowercase()}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) accentColor else Color.White,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Text(
                                    text = when (profile) {
                                        PerformanceProfile.ECO -> "Caps clock state at 60%. Cool thermal containment."
                                        PerformanceProfile.BALANCED -> "Scales frequency dynamically based on user app loads."
                                        PerformanceProfile.OVERDRIVE -> "Sets full clock speeds. Optimizes gaming thread loops."
                                    },
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.selectProfile(profile) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = accentColor,
                                    unselectedColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }
        
        // INTEGRATED RE-DESIGNED AI VOICE ASSISTANT CONSOLE
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("speech_assistant_card"),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "IK VOX-AI ASSISTANT CONSOLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val systemContext = LocalContext.current
                    val serviceActive by com.example.IkVoiceService.isVoiceServiceActive.collectAsState()
                    val assistantStatus by com.example.IkVoiceService.voiceAssistantStatus.collectAsState()
                    val rmsDb by com.example.IkVoiceService.rmsDbLive.collectAsState()
                    val lastSpeech by com.example.IkVoiceService.lastRecognizedText.collectAsState()
                    val ttsEnabled by com.example.IkVoiceService.isTtsFeedbackEnabled.collectAsState()

                    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            com.example.IkVoiceService.startServiceSafely(systemContext)
                        } else {
                            Toast.makeText(systemContext, "Audio Record permission is required for Voice control.", Toast.LENGTH_LONG).show()
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0C101F))
                                .border(1.dp, if (serviceActive) CyberCyan else Color.White.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val pulseScale by animateFloatAsState(
                                targetValue = if (serviceActive && assistantStatus == "LISTENING") {
                                    1.0f + (rmsDb.coerceAtLeast(0f) / 11f)
                                } else if (serviceActive && assistantStatus == "SPEAKING") {
                                    1.25f
                                } else 1.0f,
                                animationSpec = spring(stiffness = Spring.StiffnessLow),
                                label = "vocal_pulse"
                            )
                            val pulseColor = when (assistantStatus) {
                                "LISTENING" -> CyberCyan
                                "SPEAKING" -> CyberAmber
                                "PROCESSING" -> CyberGreen
                                else -> Color.White.copy(alpha = 0.2f)
                            }
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(pulseColor.copy(alpha = 0.4f))
                                    .border(1.dp, pulseColor, CircleShape)
                            )
                            Text(
                                text = when (assistantStatus) {
                                    "LISTENING" -> "🎙️"
                                    "SPEAKING" -> "🔊"
                                    "PROCESSING" -> "⚡"
                                    else -> "💤"
                                },
                                fontSize = 16.sp
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "ASSISTANT STATUS:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when (assistantStatus) {
                                                "LISTENING" -> CyberCyan.copy(alpha = 0.15f)
                                                "SPEAKING" -> CyberAmber.copy(alpha = 0.15f)
                                                "PROCESSING" -> CyberGreen.copy(alpha = 0.15f)
                                                else -> Color.White.copy(alpha = 0.05f)
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = assistantStatus,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (assistantStatus) {
                                            "LISTENING" -> CyberCyan
                                            "SPEAKING" -> CyberAmber
                                            "PROCESSING" -> CyberGreen
                                            else -> TextSecondary
                                        },
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            
                            val displaySpeechText = if (lastSpeech.isNotEmpty()) "\"$lastSpeech\"" else "Awaiting 'ishu...' voice trigger."
                            Text(
                                text = displaySpeechText,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.02f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Strategy B Continuous Listener",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Always listening in the background for commands starting with 'IK'",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }

                        Switch(
                            checked = serviceActive,
                            onCheckedChange = { active ->
                                if (active) {
                                    val audioPermission = android.Manifest.permission.RECORD_AUDIO
                                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                        systemContext, audioPermission
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (hasPermission) {
                                        com.example.IkVoiceService.startServiceSafely(systemContext)
                                    } else {
                                        permissionLauncher.launch(audioPermission)
                                    }
                                } else {
                                    try {
                                        systemContext.stopService(Intent(systemContext, IkVoiceService::class.java))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberCyan,
                                checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.02f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Robotic Vocal Responses (TTS)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Verbal audio confirmation from Jarvis/IK engine",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }

                        Switch(
                            checked = ttsEnabled,
                            onCheckedChange = { enabled ->
                                com.example.IkVoiceService._isTtsFeedbackEnabled.value = enabled
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberAmber,
                                checkedTrackColor = CyberAmber.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "SUPPORTED PHRASES (ENGLISH / HINDI / HINGLISH):",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "• English: \"ishu clear ram\", \"ishu turn on/off center fps\", \"ishu open terminal\", \"ishu do deep clean\"",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "• Hindi: \"ishu ram clear karo\", \"ishu dashboard kholo\", \"ishu fps chalu karo\", \"ishu app workspace kholo\"",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CoreStatusIndicator(index: Int, load: Float) {
    val barColor = if (load > 0.8f) CyberRed else if (load > 0.5f) CyberAmber else CyberGreen
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "C_${index}",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(26.dp)
        )

        Box(
            modifier = Modifier
                .weight(1.0f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF0F172A))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(load)
                    .background(barColor)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = "${String.format("%.0f", load * 100)}%",
            fontSize = 9.sp,
            color = barColor,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(28.dp)
        )
    }
}

// ---------------------- APP MANAGER TAB ----------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManagerTab(viewModel: SystemViewModel) {
    val apps by viewModel.installedApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoadingApps.collectAsState()
    var showBloatOnly by remember { mutableStateOf(true) }

    var pendingUninstallAllConfirm by remember { mutableStateOf(false) }
    var pendingAppUninstall by remember { mutableStateOf<AppInfo?>(null) }
    
    // Batch selection states
    var selectedPackages by remember { mutableStateOf(setOf<String>()) }
    var pendingBatchUninstallConfirm by remember { mutableStateOf(false) }

    val filteredApps = remember(apps, searchQuery, showBloatOnly) {
        apps.filter { app ->
            val matchesSearch = app.label.contains(searchQuery, ignoreCase = true) || 
                                app.packageName.contains(searchQuery, ignoreCase = true)
            val matchesType = if (showBloatOnly) app.isBloatware else true
            matchesSearch && matchesType
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_manager_tab")
    ) {
        // Search text field
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = TextPrimary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            placeholder = { Text("Search packages...", color = TextSecondary, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = CyberCyan) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Selector and bulk commands copy controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { showBloatOnly = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showBloatOnly) CyberCyan.copy(alpha = 0.1f) else Color.Transparent
                    ),
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (showBloatOnly) CyberCyan else Color.White.copy(alpha = 0.05f)),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Bloatware", fontSize = 10.sp, color = if (showBloatOnly) CyberCyan else TextSecondary, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = { showBloatOnly = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!showBloatOnly) CyberCyan.copy(alpha = 0.1f) else Color.Transparent
                    ),
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (!showBloatOnly) CyberCyan else Color.White.copy(alpha = 0.05f)),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("All Apps", fontSize = 10.sp, color = if (!showBloatOnly) CyberCyan else TextSecondary, fontFamily = FontFamily.Monospace)
                }
            }

            IconButton(
                onClick = { pendingUninstallAllConfirm = true },
                modifier = Modifier
                    .size(34.dp)
                    .background(CyberRed.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .border(1.dp, CyberRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Uninstall and Hide All Bloatware",
                    tint = CyberRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚠️", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Realme Custom Carrier Bloatware",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberAmber,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "System-level apps require USB ADB execution. Tap ADB Copy to get deep shell injection payload commands.",
                        fontSize = 9.sp,
                        color = TextSecondary,
                        lineHeight = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Batch Selection Controls Bar
        AnimatedVisibility(
            visible = filteredApps.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.015f))
                    .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val visiblePackageNames = filteredApps.map { it.packageName }.toSet()
                val allFilteredSelected = visiblePackageNames.isNotEmpty() && visiblePackageNames.all { it in selectedPackages }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable {
                        selectedPackages = if (allFilteredSelected) {
                            selectedPackages - visiblePackageNames
                        } else {
                            selectedPackages + visiblePackageNames
                        }
                    }
                ) {
                    Checkbox(
                        checked = allFilteredSelected,
                        onCheckedChange = { checked ->
                            selectedPackages = if (checked) {
                                selectedPackages + visiblePackageNames
                            } else {
                                selectedPackages - visiblePackageNames
                            }
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = CyberCyan,
                            uncheckedColor = Color.White.copy(alpha = 0.2f),
                            checkmarkColor = Color.Black
                        ),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = if (allFilteredSelected) "DESELECT ALL" else "SELECT ALL VISIBLE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (allFilteredSelected) CyberCyan else TextSecondary
                    )
                }

                if (selectedPackages.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedPackages.size} SELECTED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        
                        Button(
                            onClick = { pendingBatchUninstallConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberRed.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, CyberRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.height(28.dp).testTag("batch_uninstall_btn")
                        ) {
                            Text(
                                text = "DEACTIVATE",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberRed,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    Text(
                        text = "0 SELECTED",
                        fontSize = 9.sp,
                        color = TextSecondary.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyberCyan)
            }
        } else {
            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "System package scanner completed.\nNo targets found.",
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        AppControlItem(
                            app = app,
                            isSelected = app.packageName in selectedPackages,
                            onSelectedChange = { isChecked ->
                                selectedPackages = if (isChecked) {
                                    selectedPackages + app.packageName
                                } else {
                                    selectedPackages - app.packageName
                                }
                            }
                        ) {
                            pendingAppUninstall = app
                        }
                    }
                }
            }
        }
    }

    if (pendingUninstallAllConfirm) {
        CyberConfirmationDialog(
            title = "Bulk Purge Overwrite",
            message = "Are you sure you want to batch uninstall, clear, and isolate all detected manufacturer bloatware applications from your system repository? Deactivating security handles in carrier services cannot be automatically undone without full system reconstruction.",
            warningLevel = true,
            confirmLabel = "PURGE ALL",
            onConfirm = {
                viewModel.uninstallAllBloatware()
                pendingUninstallAllConfirm = false
            },
            onDismiss = {
                pendingUninstallAllConfirm = false
            }
        )
    }

    if (pendingBatchUninstallConfirm) {
        CyberConfirmationDialog(
            title = "Batch Isolation Override",
            message = "Are you sure you want to batch deactivate and isolate the ${selectedPackages.size} selected packages from the system repository simultaneously? Freezing multiple carrier dependencies might affect subsystem operations.",
            warningLevel = true,
            confirmLabel = "PROCEED_BATCH",
            onConfirm = {
                viewModel.uninstallBatchPackages(selectedPackages.toList())
                selectedPackages = emptySet()
                pendingBatchUninstallConfirm = false
            },
            onDismiss = {
                pendingBatchUninstallConfirm = false
            }
        )
    }

    pendingAppUninstall?.let { app ->
        CyberConfirmationDialog(
            title = "Isolate Package",
            message = "Are you absolutely sure you want to deactivate and isolate '${app.label}' (${app.packageName})? Freezing critical vendor assets or secondary services might cause unstable performance on some subsystems.",
            warningLevel = true,
            confirmLabel = "PROCEED_DELETE",
            onConfirm = {
                if (app.isSystem) {
                    viewModel.uninstallSystemApp(app.packageName)
                } else {
                    viewModel.uninstallUserApp(app.packageName)
                }
                pendingAppUninstall = null
            },
            onDismiss = {
                pendingAppUninstall = null
            }
        )
    }
}

@Composable
fun AppControlItem(
    app: AppInfo,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onUninstallClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_${app.packageName}")
            .clickable { onSelectedChange(!isSelected) },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isSelected) CyberCyan.copy(alpha = 0.5f) else if (app.isBloatware) CyberAmber.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = CyberCyan,
                        uncheckedColor = Color.White.copy(alpha = 0.15f),
                        checkmarkColor = Color.Black
                    ),
                    modifier = Modifier.testTag("checkbox_${app.packageName}").size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.label,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (app.isBloatware) CyberAmber.copy(alpha = 0.12f) else CyberCyan.copy(alpha = 0.12f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (app.isBloatware) "BLOAT" else if (app.isSystem) "SYSTEM" else "USER",
                                fontSize = 8.sp,
                                color = if (app.isBloatware) CyberAmber else CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Text(
                        text = app.packageName,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (app.isSystem) {
                Button(
                    onClick = onUninstallClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberRed.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CyberRed.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "UNINSTALL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberRed,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Button(
                    onClick = onUninstallClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberRed.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CyberRed.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "DELETE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberRed,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// ---------------------- RAM OPTIMIZER TAB ----------------------

@Composable
fun RamOptimizerTab(viewModel: SystemViewModel) {
    val totalRam by viewModel.ramTotalGb.collectAsState()
    val availableRam by viewModel.ramAvailableGb.collectAsState()
    val isOptimizing by viewModel.isRamOptimizing.collectAsState()
    val currentEngine by viewModel.ramOptimizationEngine.collectAsState()
    val expansionGb by viewModel.ramExpansionGb.collectAsState()
    val smartBoost by viewModel.smartBoostEnabled.collectAsState()
    val autoCleanThreshold by viewModel.autoCleanThreshold.collectAsState()

    val totalRamSafety = if (totalRam <= 0.1f) 8.0f else totalRam
    val usedRam = (totalRamSafety - availableRam).coerceAtLeast(0.1f)
    val progressPct = (usedRam / totalRamSafety).coerceIn(0.01f, 1.0f)
    val progressColor = if (progressPct > 0.85f) CyberRed else if (progressPct > 0.65f) CyberAmber else CyberGreen

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ram_optimizer_tab")
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Glowing Ram Status telemetry speedometer/ring
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF07090C)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, progressColor.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DYNAMIC MEMORY SUPERVISOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Large circular progress indicator simulating RAM load
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progressPct },
                            modifier = Modifier.size(110.dp),
                            color = progressColor,
                            strokeWidth = 8.dp,
                            trackColor = Color.White.copy(alpha = 0.05f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${String.format("%.0f", progressPct * 100)}%",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "ALLOCATED",
                                fontSize = 8.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Installed", color = TextSecondary, fontSize = 9.sp)
                            Text("${String.format("%.2f", totalRam)} GB", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Occupied Space", color = TextSecondary, fontSize = 9.sp)
                            Text("${String.format("%.2f", usedRam)} GB", color = progressColor, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Free Space", color = TextSecondary, fontSize = 9.sp)
                            Text("${String.format("%.2f", availableRam)} GB", color = CyberGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Action panel with quick boost actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "MEMTUNING QUICK INTERACTION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triggerRamClean() },
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.4f)),
                            enabled = !isOptimizing
                        ) {
                            Text(
                                "DEEP MEMPURGE",
                                fontSize = 9.sp,
                                color = CyberGreen,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Button(
                            onClick = { viewModel.runMemoryDefrag() },
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                            enabled = !isOptimizing
                        ) {
                            if (isOptimizing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CyberCyan, strokeWidth = 2.dp)
                            } else {
                                Text(
                                    "DEFRAG CACHE",
                                    fontSize = 9.sp,
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // SWAP / RAM Expansion selection block
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "VIRTUAL RAM EXPANSION (ZRAM/SWAP)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Convert standard system space to dynamic paging swap files.",
                                fontSize = 9.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val expansions = listOf(0, 2, 4, 6, 8)
                        expansions.forEach { size ->
                            val isSelected = size == expansionGb
                            val textLabel = if (size == 0) "OFF" else "+${size}G"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) CyberCyan.copy(alpha = 0.15f) else Color.Transparent)
                                    .border(1.dp, if (isSelected) CyberCyan else Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                    .clickable { viewModel.setRamExpansion(size) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = textLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) CyberCyan else Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tuning Engine Modes
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "MEMORY EXPANSION TUNING ENGINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val engines = listOf(
                        "STANDARD" to "Standard memory scheduling governor.",
                        "COMPRESSION" to "Agile system page compression in ZRAM.",
                        "PINNING" to "Keep active system and foreground tasks pinned.",
                        "LMK" to "Aggressive freeze on idle background packages."
                    )

                    engines.forEach { (engineKey, description) ->
                        val isSelected = currentEngine == engineKey
                        val bgAlpha = if (isSelected) 0.04f else 0f
                        val borderColor = if (isSelected) CyberCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.02f)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = bgAlpha))
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                .clickable { viewModel.setRamOptimizationEngine(engineKey) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = engineKey,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) CyberCyan else Color.White,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Text(
                                    text = description,
                                    fontSize = 9.sp,
                                    color = TextSecondary
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setRamOptimizationEngine(engineKey) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CyberCyan,
                                    unselectedColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Smart Booster toggles
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "INTELLIGENT TELEMETRY RULES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(0.75f)) {
                            Text("Automatic Smart Boost", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("Auto-triggers clean procedures on background system state change.", color = TextSecondary, fontSize = 9.sp)
                        }
                        Switch(
                            checked = smartBoost,
                            onCheckedChange = { viewModel.setSmartBoostEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CyberCyan,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = Color(0xFF0F172A)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Auto Clean Threshold Limit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("${String.format("%.0f", autoCleanThreshold * 100)}%", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = autoCleanThreshold,
                            onValueChange = { viewModel.setAutoCleanThreshold(it) },
                            valueRange = 0.5f..0.95f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyberCyan,
                                activeTrackColor = CyberCyan,
                                inactiveTrackColor = Color(0xFF0F172A)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ---------------------- ABOUT TAB ----------------------

@Composable
fun AboutTab(viewModel: SystemViewModel) {
    val totalRam by viewModel.ramTotalGb.collectAsState()
    val expansionGb by viewModel.ramExpansionGb.collectAsState()
    val procName by viewModel.detectedProcessorName.collectAsState()
    val procSpeed by viewModel.detectedProcessorSpeed.collectAsState()
    val procMaxSpeed by viewModel.detectedProcessorMaxSpeed.collectAsState()
    val ramType by viewModel.detectedRamType.collectAsState()
    val storageType by viewModel.detectedStorageType.collectAsState()
    val storageCapacity by viewModel.detectedStorageCapacityGb.collectAsState()
    val storageWrite by viewModel.detectedStorageWriteSpeed.collectAsState()
    val storageRead by viewModel.detectedStorageReadSpeed.collectAsState()
    val isBenchmarkRunning by viewModel.isBenchmarkRunning.collectAsState()

    val appVersionName by viewModel.appVersionName.collectAsState()
    val appVersionCode by viewModel.appVersionCode.collectAsState()

    // New premium states collected
    val displayRefreshRate by viewModel.displayRefreshRate.collectAsState()
    val displayResolution by viewModel.displayResolution.collectAsState()
    val displayDensity by viewModel.displayDensity.collectAsState()
    val batteryHealth by viewModel.batteryHealth.collectAsState()
    val batteryVoltage by viewModel.batteryVoltage.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val estimatedGpuCore by viewModel.estimatedGpuCore.collectAsState()
    val thermalThrottlingRoom by viewModel.thermalThrottlingRoom.collectAsState()
    val detectionStatus by viewModel.detectionStatus.collectAsState()

    val formattedCombined = String.format("%.1f", totalRam + expansionGb)
    val readSpeedVal = if (storageRead > 0f) String.format("%.1f MB/s", storageRead) else "Measuring..."
    val writeSpeedVal = if (storageWrite > 0f) String.format("%.1f MB/s", storageWrite) else "Measuring..."

    val aboutItems = remember(
        totalRam, expansionGb, procName, procSpeed, procMaxSpeed, ramType, 
        storageType, storageCapacity, storageWrite, storageRead,
        displayRefreshRate, displayResolution, displayDensity,
        batteryHealth, batteryVoltage, batteryLevel,
        estimatedGpuCore, thermalThrottlingRoom, detectionStatus
    ) {
        listOf(
            "Diagnostic Probe Status" to detectionStatus,
            "Manufacturer" to Build.MANUFACTURER,
            "Device Model" to Build.MODEL,
            "OS Version" to "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            "Processor" to procName,
            "Processor Speed" to procSpeed,
            "Processor Max Speed" to procMaxSpeed,
            "Estimated GPU core" to estimatedGpuCore,
            "RAM" to "${String.format("%.1f", totalRam)} GB Physical + ${expansionGb} GB Virtual Boost",
            "RAM Space" to "$formattedCombined GB Configured Combined Space",
            "RAM Type" to ramType,
            "Storage Type" to storageType,
            "Storage Capacity" to "$storageCapacity GB High-Performance Storage",
            "Storage Read Speed" to readSpeedVal,
            "Storage Write Speed" to writeSpeedVal,
            "Display Resolution" to displayResolution,
            "Display Density" to displayDensity,
            "Display Hardware Refresh" to displayRefreshRate,
            "Thermal Protection State" to thermalThrottlingRoom,
            "Battery Level" to batteryLevel,
            "Battery Voltage Indicator" to batteryVoltage,
            "Battery Cell Health" to batteryHealth,
            "Board ID" to Build.BOARD,
            "Fingerprint" to Build.FINGERPRINT
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("about_tab")
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Detailed Phone Spec Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF07090C)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HARDWARE PLATFORM: ${Build.MANUFACTURER.uppercase()} ${Build.MODEL.uppercase()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (isBenchmarkRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = CyberCyan,
                                strokeWidth = 1.5.dp
                            )
                        } else {
                            Text(
                                text = "⚡ ACTIVE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Spec-calibrated and adapted for ${Build.MANUFACTURER} ${Build.MODEL}. Real-time system profiles dynamically scale core schedules, thread configurations, and RAM sweep timers based on live active storage IO and memory pressure indices.",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { viewModel.detectAndOptimizeState() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)),
                        enabled = !isBenchmarkRunning
                    ) {
                        Text(
                            text = if (isBenchmarkRunning) "BENCHMARKING IO SHELF..." else "RUN REAL-TIME HARDWARE IO TEST",
                            fontSize = 9.sp,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Startup boot loader preview card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CyberPurple.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "IK BOOT DIAGNOSTICS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberPurple,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "PREVIEW",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Preview the clean, supercharged console diagnostics start screen which showcases real-time system loading parameters on startup without lag or stutter.",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.setShowBootIntro(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyberPurple.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Intro Preview",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LAUNCH SYSTEM BOOTLOADER",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // OTA Multicast Update Administration Engine Card
        item {
            val isBroadcasting by viewModel.isBroadcasting.collectAsState()
            val broadcastProgress by viewModel.broadcastProgress.collectAsState()
            val broadcastLogs by viewModel.broadcastLogs.collectAsState()

            var targetVerInput by remember { mutableStateOf("1.5.8") }
            var changeNotesInput by remember { mutableStateOf("Deploy telemetry diagnostics and OTA engine updates.") }

            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            LaunchedEffect(broadcastLogs.size) {
                if (broadcastLogs.isNotEmpty()) {
                    listState.animateScrollToItem(broadcastLogs.size - 1)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(
                    1.dp, 
                    if (isBroadcasting) CyberPurple.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "📡", fontSize = 14.sp)
                            Text(
                                text = "IK-OTA DEPLOYMENT DISPATCH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberPurple,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isBroadcasting) CyberPurple.copy(alpha = 0.15f) else CyberGreen.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isBroadcasting) "BROADCASTING" else "GATEWAY ACTIVE",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBroadcasting) CyberPurple else CyberGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Simulate and trigger instantaneous Over-the-Air updates to all active client platforms. Deploys version upgrades and monitors real-time transaction nodes.",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isBroadcasting) {
                        // Inputs Box
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Target version row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "TARGET VERSION:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.width(100.dp)
                                )
                                TextField(
                                    value = targetVerInput,
                                    onValueChange = { targetVerInput = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("ota_version_input")
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Black,
                                        unfocusedContainerColor = Color.Black,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    singleLine = true
                                )
                            }

                            // Changelog row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "RELEASE NOTES:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.width(100.dp).padding(top = 8.dp)
                                )
                                TextField(
                                    value = changeNotesInput,
                                    onValueChange = { changeNotesInput = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .testTag("ota_notes_input")
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
                                    textStyle = TextStyle(
                                        color = TextSecondary,
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.SansSerif
                                    ),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Black,
                                        unfocusedContainerColor = Color.Black,
                                        focusedTextColor = TextSecondary,
                                        unfocusedTextColor = TextSecondary,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    maxLines = 2
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Trigger Button
                        Button(
                            onClick = {
                                viewModel.triggerUpdateBroadcast(targetVerInput, changeNotesInput)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("ota_broadcast_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPurple.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, CyberPurple.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "BROADCAST NEW UPDATE (v$targetVerInput)",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberPurple,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        // Broadcasting state visualization
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Progress bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DOWNSTREAM SEEDING PROGRESS",
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberPurple,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${(broadcastProgress * 100).toInt()}%",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberPurple,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            LinearProgressIndicator(
                                progress = { broadcastProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = CyberPurple,
                                trackColor = Color.White.copy(alpha = 0.05f)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Miniature console log block of transmission details
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black)
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                    .padding(8.dp)
                            ) {
                                androidx.compose.foundation.lazy.LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(broadcastLogs) { log ->
                                        Text(
                                            text = log,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 8.sp,
                                            color = if (log.contains("SUCCESS")) CyberGreen else if (log.contains("NET") || log.contains("SYSTEM")) CyberCyan else TextSecondary,
                                            lineHeight = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        items(aboutItems) { (label, value) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.02f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(0.35f)
                    )
                    Text(
                        text = value,
                        fontSize = 10.sp,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.65f),
                        lineHeight = 12.sp
                    )
                }
            }
        }

        // Bottom App Version, Developer & Update Date info
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "IK SYSTEM CONSOLE ENVIRONMENT DETAILS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("App Version", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("v$appVersionName (Build $appVersionCode)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Developer", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("Ishant Kohli", color = CyberGreen, fontWeight = FontWeight.Bold, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Last Update Date", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("May 27, 2026 UTC", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// ---------------------- DEEP SLEEP TRIGGER ----------------------

@Composable
fun DeepSleepTriggerTab(onSleepTrigger: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(CyberCyan.copy(alpha = 0.08f))
                    .border(1.dp, CyberCyan, CircleShape)
                    .clickable { onSleepTrigger() },
                contentAlignment = Alignment.Center
            ) {
                Text("🌙", fontSize = 36.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ACTIVATE STEALTH DEEP SLEEP",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Suspends background core calculations, screen telemetry loops, and visual overlays to fully protect processor states.",
                color = TextSecondary,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )
        }
    }
}

// ---------------------- FULL COCONUT STEALTH WAKE SCREEN ----------------------

@Composable
fun StealthSleepScreen(onWakeTrigger: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAnimation by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radial_glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07090C))
            .testTag("stealth_sleep_screen"),
        contentAlignment = Alignment.Center
    ) {
        MinimalSlateGridBackground()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "HYPER SLEEP ACTIVATED",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CyberGreen,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp
            )
            Text(
                text = "System telemetry paused. Tap node to wake.",
                fontSize = 10.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(80.dp))

            // Pulse node trigger button
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Glow
                Box(
                    modifier = Modifier
                        .size(110.dp * glowAnimation)
                        .blur(16.dp)
                        .background(CyberGreen.copy(alpha = 0.12f), CircleShape)
                )

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(DarkSurfaceElevated, DarkSurface)))
                        .border(3.dp, Color(0xFF0F1115), CircleShape)
                        .border(1.dp, CyberGreen.copy(alpha = 0.4f), CircleShape)
                        .clickable { onWakeTrigger() }
                        .testTag("wake_system_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🌙",
                            fontSize = 28.sp
                        )
                        Text(
                            text = "DEEP SLEEP",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))

            Text(
                text = "\"Suspends background execution until the core is awakened again.\"",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 30.dp),
                lineHeight = 14.sp
            )
        }
    }
}

// ---------------------- MODERN MINIMAL SYSTEM BOTTOM BAR ----------------------

@Composable
fun MinimalBottomAppBar(
    activeTab: ActiveTab,
    onTabSelected: (ActiveTab) -> Unit
) {
    Surface(
        color = DarkBg,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActiveTab.entries.forEach { tab ->
                val isSelected = activeTab == tab
                val itemLabel = when (tab) {
                    ActiveTab.PERFORMANCE -> "SYSTEM"
                    ActiveTab.RAM -> "RAM"
                    ActiveTab.APPS -> "APPS"
                    ActiveTab.TERMINAL -> "TERM"
                    ActiveTab.SLEEP -> "STEALTH"
                    ActiveTab.ABOUT -> "ABOUT"
                    ActiveTab.FPS -> "FPS"
                    ActiveTab.VOICE -> "VOICE"
                }

                val itemEmoji = when (tab) {
                    ActiveTab.PERFORMANCE -> "📊"
                    ActiveTab.RAM -> "⚡"
                    ActiveTab.APPS -> "📁"
                    ActiveTab.TERMINAL -> "💻"
                    ActiveTab.SLEEP -> "🌙"
                    ActiveTab.ABOUT -> "ℹ️"
                    ActiveTab.FPS -> "📈"
                    ActiveTab.VOICE -> "🎙️"
                }

                val textColor = if (isSelected) CyberCyan else TextSecondary

                Column(
                    modifier = Modifier
                        .width(48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(tab) }
                        .testTag("tab_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Modern pill backing on select
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 26.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (isSelected) CyberCyan.copy(alpha = 0.15f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = itemEmoji,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = itemLabel,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun SleepBackupTab(onSleepTrigger: () -> Unit) {
    DeepSleepTriggerTab(onSleepTrigger)
}

@Composable
fun TerminalTab(viewModel: SystemViewModel) {
    val logs by viewModel.terminalLogs.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    var pendingTerminalCmdToConfirm by remember { mutableStateOf<String?>(null) }

    // Auto-scroll to end when terminal logs update
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        // Glowing Terminal Window Frame
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF07090C)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Console Status Bar Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(CyberGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SYSTEM SHELL EXECUTOR [CONNECTED]",
                            color = CyberGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "PORT: LOCALHOST",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable logs dump
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(logs) { log ->
                        val textColor = when {
                            log.startsWith("ERROR:") -> CyberRed
                            log.startsWith("Success:") -> CyberGreen
                            log.startsWith("ishant@") -> CyberCyan
                            log.contains("[BLOAT]") -> CyberAmber
                            log.contains("[SYSTEM]") -> CyberPurple
                            else -> TextPrimary
                        }
                        Text(
                            text = log,
                            color = textColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 1.dp),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input console bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$ ",
                color = CyberCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )
            
            Spacer(modifier = Modifier.width(4.dp))

            // Standard styled cyber text field
            androidx.compose.foundation.text.BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(CyberCyan),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp)
                    .testTag("terminal_command_input"),
                decorationBox = { innerTextField ->
                    if (inputText.isEmpty()) {
                        Text(
                            text = "Enter command (e.g. help, neofetch)...",
                            color = TextSecondary.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val cleanCmd = inputText.trim()
                        val args = cleanCmd.split("\\s+".toRegex())
                        val primaryCmd = args.getOrNull(0)?.lowercase() ?: ""
                        
                        val isDestructive = primaryCmd == "uninstall" || 
                                            primaryCmd == "restoresystem" || 
                                            primaryCmd == "rm" || 
                                            primaryCmd == "reboot" || 
                                            cleanCmd.contains("pm uninstall") || 
                                            cleanCmd.contains("rm ") ||
                                            cleanCmd.contains("dd ") ||
                                            cleanCmd.contains("mkfs") ||
                                            cleanCmd.contains("format ")
                        
                        if (isDestructive) {
                            pendingTerminalCmdToConfirm = cleanCmd
                        } else {
                            viewModel.executeTerminalCommand(inputText)
                            inputText = ""
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "RUN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }

    pendingTerminalCmdToConfirm?.let { cmd ->
        CyberConfirmationDialog(
            title = "Security Intercept",
            message = "The command sequence you initiated contains low-level system deconstruction parameters: '$cmd'. Direct binary modifications, sweeps, or package deletions carry high security and data loss risks.",
            warningLevel = true,
            confirmLabel = "EXECUTE",
            onConfirm = {
                viewModel.executeTerminalCommand(cmd)
                inputText = ""
                pendingTerminalCmdToConfirm = null
            },
            onDismiss = {
                pendingTerminalCmdToConfirm = null
            }
        )
    }
}

@Composable
fun FpsMeasureTab(viewModel: SystemViewModel) {
    val context = LocalContext.current
    val isFpsOverlayActive by viewModel.isFpsOverlayActive.collectAsState()
    
    // In-app live FPS measurement
    var liveInAppFps by remember { mutableStateOf(60) }
    var hasPermission by remember { mutableStateOf(viewModel.checkOverlayPermission()) }

    // Periodically inspect permission to auto-enable toggle smoothly when user returns
    LaunchedEffect(Unit) {
        while (true) {
            hasPermission = viewModel.checkOverlayPermission()
            delay(1000)
        }
    }

    // Jetpack Compose native frame-ticker loop for perfect safety and automatic cancellation
    LaunchedEffect(Unit) {
        var frames = 0
        var lastTime = android.os.SystemClock.uptimeMillis()
        while (true) {
            withFrameMillis { frameTime ->
                frames++
                val elapsed = frameTime - lastTime
                if (elapsed >= 1000) {
                    liveInAppFps = ((frames * 1000) / elapsed).toInt().coerceIn(1, 240)
                    frames = 0
                    lastTime = frameTime
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("fps_measure_container"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Dynamic Graphic Header Widget (Showing realtime app rate)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LOCAL GRAPHICS PIPE ENGINE",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Huge Circular gauge for live in-app frame rate
                    Box(
                        modifier = Modifier.size(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse_gauge")
                        val outerGlowAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.05f,
                            targetValue = 0.22f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "alpha"
                        )

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidthVal = 8.dp.toPx()
                            val sweepAngle = (liveInAppFps / 120f).coerceIn(0f, 1f) * 240f
                            
                            // Background guide arc
                            drawArc(
                                color = Color.White.copy(alpha = 0.04f),
                                startAngle = 150f,
                                sweepAngle = 240f,
                                useCenter = false,
                                style = Stroke(width = strokeWidthVal),
                                size = size
                            )

                            // Glowing current rate arc
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(CyberCyan, CyberPurple, CyberCyan)
                                ),
                                startAngle = 150f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidthVal),
                                size = size
                            )
                        }

                        // Inner Numbers Display
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$liveInAppFps",
                                color = CyberCyan,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "ACTIVE FPS",
                                color = TextSecondary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Current app container drawing frame metrics. Perfectly optimized to run without latency or UI resource hogging.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Game FPS Engine & Calibration Card
        item {
            val fpsTargetProfile by viewModel.fpsTargetProfile.collectAsState()
            
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CyberPurple.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "GAME FPS ENGINE & CALIBRATION",
                        color = CyberPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Text(
                        text = "Calibrate the active overlay to sync precisely with your in-game frame locks. This solves system compositor refresh rate mismatch in full-screen games.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    
                    val maxFps by viewModel.maxSupportedFps.collectAsState()
                    val baseOptions = remember { listOf(240, 165, 144, 120, 90, 60, 30) }
                    val options = remember(maxFps) {
                        val list = mutableListOf("Auto")
                        baseOptions.forEach { rate ->
                            if (rate <= maxFps) {
                                list.add("$rate FPS Lock")
                            }
                        }
                        if (maxFps > 30 && maxFps !in baseOptions) {
                            list.add(1, "$maxFps FPS Lock")
                        }
                        list
                    }
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val chunks = options.chunked(4)
                        chunks.forEach { rowOptions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowOptions.forEach { option ->
                                    val isSelected = fpsTargetProfile == option
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) CyberPurple.copy(alpha = 0.15f) else DarkBg)
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) CyberPurple else Color.White.copy(alpha = 0.05f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.setFpsTargetProfile(option) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = option.replace(" Lock", ""),
                                            color = if (isSelected) CyberPurple else TextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                if (rowOptions.size < 4) {
                                    repeat(4 - rowOptions.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Global Overlay Toggle & Permission Setup Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SYSTEM FLOATING OVERLAY",
                                color = CyberCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Real-time overlay shown over other games/apps.",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }

                        Switch(
                            checked = isFpsOverlayActive,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    if (hasPermission) {
                                        viewModel.toggleFpsOverlay(true)
                                    } else {
                                        // Send to permission settings
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        ).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        try {
                                            context.startActivity(intent)
                                            Toast.makeText(context, "Please enable 'Display over other apps' setting first", Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Overlay settings page is not available. Simulating active status...", Toast.LENGTH_LONG).show()
                                            viewModel.toggleFpsOverlay(true)
                                        }
                                    }
                                } else {
                                    viewModel.toggleFpsOverlay(false)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberCyan,
                                checkedTrackColor = CyberCyan.copy(alpha = 0.25f),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                                uncheckedTrackColor = DarkBg
                            ),
                            modifier = Modifier.testTag("overlay_system_toggle")
                        )
                    }

                    if (!hasPermission) {
                        // Caution notification explaining display over apps requirement
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberAmber.copy(alpha = 0.08f))
                                .border(1.dp, CyberAmber.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "⚠️",
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                                Column {
                                    Text(
                                        text = "OVERLAY PERMISSION REQUIRED",
                                        color = CyberAmber,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "To draw the real-time FPS overlay over other games, you must grant the 'Display over other apps' permission in Android Settings.",
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            ).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberAmber.copy(alpha = 0.12f)),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, CyberAmber.copy(alpha = 0.5f)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp).testTag("grant_permission_btn")
                                    ) {
                                        Text(
                                            text = "GRANT PERMISSION NOW",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberAmber,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Success block
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberGreen.copy(alpha = 0.06f))
                                .border(1.dp, CyberGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "✅ Permissions cleared: Ready to draw real-time telemetry over other applications.",
                                color = CyberGreen,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // 3. Overlay guide and Interactive demonstration
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "OVERLAY DRAG & DISMISS GUIDE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "• Click overlay anytime to cycle between FULL DETAILS and MINIMAL mode (raw numbers only, hiding labels like FPS and temperature to keep games ultra-immersive).\n" +
                               "• Drag the overlay window freely with your finger to position it anywhere on screen.\n" +
                               "• Symmetrical telemetry uses minimal memory structure keeping performance lag-free.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Live Interactive Simulated Overlay Preview
                    Text(
                        text = "INTERACTIVE SIMULATED PREVIEW:",
                        color = CyberPurple,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    var isSimulatedMinimal by remember { mutableStateOf(false) }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .clickable { isSimulatedMinimal = !isSimulatedMinimal }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.8f))
                                .border(1.dp, if (isSimulatedMinimal) CyberPurple else CyberCyan, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (isSimulatedMinimal) {
                                Text("$liveInAppFps", color = CyberCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text("|", color = Color.White.copy(alpha = 0.15f), fontSize = 11.sp)
                                Text("38°", color = CyberPurple, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text("|", color = Color.White.copy(alpha = 0.15f), fontSize = 11.sp)
                                Text("18%", color = CyberGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            } else {
                                Text("FPS: $liveInAppFps", color = CyberCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text("|", color = Color.White.copy(alpha = 0.15f), fontSize = 11.sp)
                                Text("TEMP: 38.6°C", color = CyberPurple, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text("|", color = Color.White.copy(alpha = 0.15f), fontSize = 11.sp)
                                Text("GPU: 18%", color = CyberGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Text(
                        text = "^ Click simulated overlay to test modes",
                        color = TextSecondary,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ---------------------- IK VOICE TAB ----------------------

@Composable
fun IkVoiceTab(viewModel: SystemViewModel) {
    val systemContext = LocalContext.current
    val serviceActive by com.example.IkVoiceService.isVoiceServiceActive.collectAsState()
    val isUserVoiceActive by com.example.IkVoiceService.isUserVoiceActive.collectAsState()
    val assistantStatus by com.example.IkVoiceService.voiceAssistantStatus.collectAsState()
    val rmsDb by com.example.IkVoiceService.rmsDbLive.collectAsState()
    val lastSpeech by com.example.IkVoiceService.lastRecognizedText.collectAsState()
    val ttsEnabled by com.example.IkVoiceService.isTtsFeedbackEnabled.collectAsState()

    // Automatic permission request on entering tab
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            com.example.IkVoiceService.startServiceSafely(systemContext)
        } else {
            Toast.makeText(systemContext, "Audio Record permission is required for Voice control.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val audioPermission = android.Manifest.permission.RECORD_AUDIO
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            systemContext, audioPermission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            permissionLauncher.launch(audioPermission)
        } else {
            if (!serviceActive) {
                com.example.IkVoiceService.startServiceSafely(systemContext)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ik_voice_tab")
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing ARC visual reaction (changing from white to all rainbow colors)
        item {
            Spacer(modifier = Modifier.height(12.dp))
            VoiceArcReaction(rmsDb = rmsDb, isListening = serviceActive && isUserVoiceActive && assistantStatus == "LISTENING")
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Active Voice Red button activation under the arc/reaction
        item {
            if (!isUserVoiceActive || !serviceActive) {
                Button(
                    onClick = {
                        val audioPermission = android.Manifest.permission.RECORD_AUDIO
                        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                            systemContext, audioPermission
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            com.example.IkVoiceService._isUserVoiceActive.value = true
                            com.example.IkVoiceService._voiceAssistantStatus.value = "IDLE"
                            com.example.IkVoiceService.startServiceSafely(systemContext, forceListen = true)
                            Toast.makeText(systemContext, "Voice controller reactivated!", Toast.LENGTH_SHORT).show()
                        } else {
                            permissionLauncher.launch(audioPermission)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .widthIn(min = 180.dp)
                        .height(44.dp)
                        .testTag("reactivate_voice_button"),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Activate Voice",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Active Voice",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberGreen.copy(alpha = 0.08f))
                        .border(1.dp, CyberGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CyberGreen)
                    )
                    Text(
                        text = "VOICE ENGINE ACTIVE",
                        color = CyberGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "INTELLIGENT RECOGNITION CONSOLE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "STATUS:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (!isUserVoiceActive) CyberRed.copy(alpha = 0.15f)
                                            else when (assistantStatus) {
                                                "LISTENING" -> CyberCyan.copy(alpha = 0.15f)
                                                "SPEAKING" -> CyberAmber.copy(alpha = 0.15f)
                                                "PROCESSING" -> CyberGreen.copy(alpha = 0.15f)
                                                else -> Color.White.copy(alpha = 0.05f)
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (!isUserVoiceActive) "INACTIVE (CLOSED)" else assistantStatus,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isUserVoiceActive) CyberRed
                                        else when (assistantStatus) {
                                            "LISTENING" -> CyberCyan
                                            "SPEAKING" -> CyberAmber
                                            "PROCESSING" -> CyberGreen
                                            else -> TextSecondary
                                        },
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val displaySpeechText = if (lastSpeech.isNotEmpty()) "\"$lastSpeech\"" else "Awaiting voice command input..."
                            Text(
                                text = "Last spoken: $displaySpeechText",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.02f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Robotic Vocal Responses (TTS)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Speak voice instructions audibly",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }

                        Switch(
                            checked = ttsEnabled,
                            onCheckedChange = { enabled ->
                                com.example.IkVoiceService._isTtsFeedbackEnabled.value = enabled
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberAmber,
                                checkedTrackColor = CyberAmber.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                            )
                        )
                    }
                }
            }
        }

        // Voice commands list card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "REGISTERED SPEECH PHRASES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    // English section
                    Text(
                        text = "🇬🇧 ENGLISH PHRASES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 6.dp, bottom = 16.dp)
                    ) {
                        VoiceCommandItem(command = "ishu clean ram", desc = "Triggers complete garbage collection and sweeps active processes")
                        VoiceCommandItem(command = "ishu clean cache", desc = "Reclaims allocated local memory & junk cache structures")
                        VoiceCommandItem(command = "ishu turn on fps / ishu show fps", desc = "Enables real-time performance monitor hardware overlay")
                        VoiceCommandItem(command = "ishu turn off fps / ishu hide fps", desc = "Removes floating benchmark metrics HUD overlay")
                        VoiceCommandItem(command = "ishu open terminal", desc = "Launches deep system console code compiler tab")
                        VoiceCommandItem(command = "ishu sleep / ishu deep sleep", desc = "Engages low-power stealth standby hibernation mode")
                        VoiceCommandItem(command = "ishu close", desc = "Stops microphone continuous loop (closes Voice assistant)")
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                    // Hindi section
                    Text(
                        text = "🇮🇳 HINDI PHRASES (हिंदी)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        VoiceCommandItem(command = "ishu ram clean kar / ishu ram clear karo", desc = "Purges background cache channels and frees RAM")
                        VoiceCommandItem(command = "ishu cache saaf kar / ishu cache clean kar", desc = "Reclaims database buffer page locks and metadata cache")
                        VoiceCommandItem(command = "ishu dashboard kholo", desc = "Navigates instantly back to System Performance dashboard")
                        VoiceCommandItem(command = "ishu terminal kholo / ishu console open kar", desc = "Opens the manual administrative console interface")
                        VoiceCommandItem(command = "ishu fps chalu karo / ishu fps laga", desc = "Installs floating FPS overlays above gameplay")
                        VoiceCommandItem(command = "ishu fps band kar / ishu fps hata", desc = "Terminates active telemetry system graphics overlays")
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceCommandItem(command: String, desc: String) {
    Column {
        Text(
            text = command,
            color = CyberCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = desc,
            color = TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun VoiceArcReaction(rmsDb: Float, isListening: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_color")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val pulseScale by animateFloatAsState(
        targetValue = if (isListening) 1.0f + (rmsDb.coerceAtLeast(0f) / 12f) else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "arc_pulse"
    )

    Box(
        modifier = Modifier
            .size(160.dp)
            .scale(pulseScale),
        contentAlignment = Alignment.Center
    ) {
        val brushColors = listOf(
            Color.White,
            Color(0xFFFF3366), // Neon Red Pink
            Color(0xFF33CCFF), // Neon Cyan
            Color(0xFF33FF99), // Neon Green
            Color(0xFFFFCC33), // Golden Yellow
            Color(0xFFCC33FF), // Purple
            Color.White
        )
        
        val sweepGradient = Brush.sweepGradient(
            colors = brushColors,
            center = Offset(110.dp.value, 110.dp.value)
        )

        Box(
            modifier = Modifier
                .size(110.dp)
                .blur(20.dp)
                .clip(CircleShape)
                .background(sweepGradient)
                .scale(0.85f)
        )

        Canvas(modifier = Modifier.size(130.dp)) {
            val sizePx = size.width
            val styleStroke = Stroke(width = 4.dp.toPx())
            
            drawArc(
                brush = Brush.sweepGradient(brushColors, center = Offset(sizePx / 2, sizePx / 2)),
                startAngle = phase,
                sweepAngle = 270f,
                useCenter = false,
                style = styleStroke,
                topLeft = Offset(8.dp.toPx(), 8.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(sizePx - 16.dp.toPx(), sizePx - 16.dp.toPx())
            )

            drawArc(
                brush = Brush.sweepGradient(listOf(CyberCyan, CyberPurple, CyberCyan), center = Offset(sizePx / 2, sizePx / 2)),
                startAngle = -phase * 1.5f,
                sweepAngle = 140f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx()),
                topLeft = Offset(18.dp.toPx(), 18.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(sizePx - 36.dp.toPx(), sizePx - 36.dp.toPx())
            )

            drawArc(
                color = Color.White.copy(alpha = 0.6f),
                startAngle = phase * 2f,
                sweepAngle = 45f,
                useCenter = false,
                style = Stroke(width = 1.5.dp.toPx()),
                topLeft = Offset(26.dp.toPx(), 26.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(sizePx - 52.dp.toPx(), sizePx - 52.dp.toPx())
            )
        }

        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F1115))
                .border(1.2.dp, Color.White.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = "Microphone Status Ticks",
                tint = if (isListening) CyberCyan else CyberRed,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun CyberConfirmationDialog(
    title: String,
    message: String,
    warningLevel: Boolean = false,
    confirmLabel: String = "CONFIRM",
    cancelLabel: String = "CANCEL",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (warningLevel) "⚠️" else "🛡️",
                    fontSize = 18.sp
                )
                Text(
                    text = title.uppercase(),
                    color = if (warningLevel) CyberRed else CyberCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Text(
                text = message,
                color = TextPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                lineHeight = 16.sp
            )
        },
        containerColor = DarkSurfaceElevated,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 6.dp,
        modifier = Modifier.border(
            width = 1.dp,
            color = if (warningLevel) CyberRed.copy(alpha = 0.3f) else CyberCyan.copy(alpha = 0.3f),
            shape = RoundedCornerShape(20.dp)
        ),
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (warningLevel) CyberRed.copy(alpha = 0.15f) else CyberCyan.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    width = 1.6.dp,
                    color = if (warningLevel) CyberRed else CyberCyan
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = confirmLabel,
                    color = if (warningLevel) CyberRed else CyberCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = cancelLabel,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    )
}
