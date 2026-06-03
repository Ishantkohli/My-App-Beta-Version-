package com.example

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class LogCategory {
    ALL, SYSTEM, REMOVALS, WARNINGS
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConsoleEventLogView(viewModel: SystemViewModel) {
    val logs by viewModel.terminalLogs.collectAsState()
    val isScanningPartition by viewModel.isScanningPartition.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val scanPartitionName by viewModel.scanPartitionName.collectAsState()
    val scanCompletedDialog by viewModel.scanCompletedDialog.collectAsState()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    var selectedCategory by remember { mutableStateOf(LogCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var isRecordingSimulatedLogs by remember { mutableStateOf(false) }

    // Blinking cursor state
    var cursorVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            cursorVisible = !cursorVisible
            delay(530)
        }
    }

    // Interactive custom simulator interval
    LaunchedEffect(isRecordingSimulatedLogs) {
        if (isRecordingSimulatedLogs) {
            while (isRecordingSimulatedLogs) {
                delay(3500 + Random.nextLong(2000))
                val randomType = Random.nextInt(3)
                val timePre = "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())}]"
                val newLog = when (randomType) {
                    0 -> {
                        val services = listOf("location", "telecom", "notification", "vibrator", "wifi")
                        val service = services.random()
                        "$timePre [SYSTEM] Polled $service system service. Thread state: BALANCED. Utilization optimized."
                    }
                    1 -> {
                        val packages = listOf("com.facebook.system", "com.sec.android.app.sbrowser", "com.huawei.powergenie", "com.miui.analytics")
                        val pkg = packages.random()
                        "$timePre Success: Cleaned background handles for bloatware packet $pkg. Isolated memory freed."
                    }
                    else -> {
                        val temp = 39f + Random.nextFloat() * 8f
                        if (temp > 44f) {
                            "$timePre WARNING: Core thermal envelope elevated (${String.format("%.1f", temp)}°C). Scale factor safety fallback initialized."
                        } else {
                            "$timePre WARNING: Low garbage collection memory cycle detected. Forcing cache sweep."
                        }
                    }
                }
                viewModel.appendTerminalLog(newLog)
            }
        }
    }

    // Auto-scroll logic
    LaunchedEffect(logs.size, selectedCategory) {
        if (logs.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem((logs.size * 2).coerceAtMost(Int.MAX_VALUE))
            }
        }
    }

    // Classification filter helper
    val filteredLogs = remember(logs, selectedCategory, searchQuery) {
        logs.filter { log ->
            // Search criteria
            val matchesSearch = searchQuery.isBlank() || log.contains(searchQuery, ignoreCase = true)
            if (!matchesSearch) return@filter false

            // Category classification
            when (selectedCategory) {
                LogCategory.ALL -> true
                LogCategory.SYSTEM -> {
                    log.contains("[SYSTEM]", ignoreCase = true) || log.contains("ishant@", ignoreCase = true) || log.contains("kernel", ignoreCase = true)
                }
                LogCategory.REMOVALS -> {
                    log.startsWith("Success:") || log.startsWith("Successfully") || log.contains("[BLOAT]", ignoreCase = true) || log.contains("freed", ignoreCase = true)
                }
                LogCategory.WARNINGS -> {
                    log.startsWith("ERROR:") || log.startsWith("WARNING:") || log.contains("warn", ignoreCase = true) || log.contains("error", ignoreCase = true) || log.contains("fail", ignoreCase = true)
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("console_event_log_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0C10)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isRecordingSimulatedLogs) CyberGreen else CyberPurple)
                    )
                    Column {
                        Text(
                            text = "SYSTEM KERNEL CONSOLE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Real-time thread events, deactivations & safety warnings",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                // Streaming indicator
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = (if (isRecordingSimulatedLogs) CyberGreen else TextSecondary).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, (if (isRecordingSimulatedLogs) CyberGreen else TextSecondary).copy(alpha = 0.2f))
                ) {
                    Text(
                        text = if (isRecordingSimulatedLogs) "LIVE STREAMING" else "BUFFER STANDBY",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecordingSimulatedLogs) CyberGreen else TextSecondary,
                        fontSize = 8.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search filtering field
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("console_search_input"),
                placeholder = {
                    Text(
                        "Search console entries...",
                        color = TextSecondary.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF11141B),
                    unfocusedContainerColor = Color(0xFF11141B),
                    disabledContainerColor = Color(0xFF11141B),
                    focusedIndicatorColor = Color.White.copy(alpha = 0.15f),
                    unfocusedIndicatorColor = Color.White.copy(alpha = 0.05f),
                    cursorColor = CyberCyan,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Fast category filters row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LogCategory.entries.forEach { category ->
                    val isSelected = selectedCategory == category
                    val chipColor = when (category) {
                        LogCategory.ALL -> CyberCyan
                        LogCategory.SYSTEM -> CyberPurple
                        LogCategory.REMOVALS -> CyberGreen
                        LogCategory.WARNINGS -> CyberRed
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) chipColor.copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) chipColor else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = category.name,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = if (isSelected) chipColor else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // -- SYSTEM INTEGRITY HEALTH SCAN COMPONENT --
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.015f))
                    .border(
                        1.dp,
                        if (isScanningPartition) CyberCyan.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.04f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isScanningPartition) CyberCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = if (isScanningPartition) "⏳" else "🛡️", fontSize = 11.sp)
                        }
                        Column {
                            Text(
                                text = "SYSTEM INTEGRITY SHIELD",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isScanningPartition) CyberCyan else Color.White,
                                fontSize = 10.5.sp
                            )
                            Text(
                                text = if (isScanningPartition) "Active block scanning on: $scanPartitionName" else "Audit OS kernel partition tables & sectors for security checks",
                                color = TextSecondary,
                                fontSize = 8.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { viewModel.runSystemPartitionIntegrityScan() },
                        enabled = !isScanningPartition,
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("btn_start_integrity_scan"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan.copy(alpha = 0.08f),
                            disabledContainerColor = Color.White.copy(alpha = 0.02f)
                        ),
                        border = BorderStroke(1.dp, if (isScanningPartition) Color.White.copy(alpha = 0.1f) else CyberCyan.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text(
                            text = if (isScanningPartition) "AUDITING..." else "RUN AUDIT",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            color = if (isScanningPartition) TextSecondary else CyberCyan
                        )
                    }
                }

                if (isScanningPartition) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { scanProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = CyberCyan,
                            trackColor = Color.White.copy(alpha = 0.08f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${(scanProgress * 100).toInt()}%",
                            color = CyberCyan,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable console log screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFF05070B), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                if (filteredLogs.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NO CONSOLE RECORDS MATCHING SEARCH",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary.copy(alpha = 0.5f),
                            fontSize = 8.sp
                        )
                        Text(
                            text = "Awaiting live system event telemetry... ${if (cursorVisible) "_" else ""}",
                            color = TextSecondary.copy(alpha = 0.4f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(filteredLogs) { log ->
                            val color = when {
                                log.contains("WARNING:", ignoreCase = true) || log.contains("error", ignoreCase = true) -> CyberRed
                                log.contains("Success:", ignoreCase = true) || log.contains("freed", ignoreCase = true) -> CyberGreen
                                log.contains("[SYSTEM]", ignoreCase = true) || log.contains("ishant@", ignoreCase = true) -> CyberPurple
                                else -> TextPrimary
                            }

                            Text(
                                text = log,
                                color = color,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 13.sp
                            )
                        }

                        // Display active flashing terminal cursor line
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "root@ik_kernel_tuner:~# ",
                                    color = CyberCyan,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (cursorVisible) "▋" else " ",
                                    color = CyberCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tool action section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Toggle simulated events
                Button(
                    onClick = { isRecordingSimulatedLogs = !isRecordingSimulatedLogs },
                    modifier = Modifier
                        .weight(1.2f)
                        .height(36.dp)
                        .testTag("console_toggle_stream_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = (if (isRecordingSimulatedLogs) CyberRed else CyberGreen).copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, (if (isRecordingSimulatedLogs) CyberRed else CyberGreen).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isRecordingSimulatedLogs) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = "Simulate action",
                        tint = if (isRecordingSimulatedLogs) CyberRed else CyberGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRecordingSimulatedLogs) "HALT SIMULATOR" else "START SIMULATOR",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecordingSimulatedLogs) CyberRed else CyberGreen
                    )
                }

                // Copy buffer
                IconButton(
                    onClick = {
                        if (logs.isNotEmpty()) {
                            val joinedString = logs.joinToString("\n")
                            clipboardManager.setText(AnnotatedString(joinedString))
                            Toast.makeText(context, "Terminal log buffer copied!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Log buffer is empty.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.01f), RoundedCornerShape(8.dp))
                        .testTag("console_copy_buffer_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Copy log buffer",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }

                // Clear log buffer
                IconButton(
                    onClick = {
                        viewModel.clearTerminalLogs()
                        Toast.makeText(context, "Log buffer cleared.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.01f), RoundedCornerShape(8.dp))
                        .testTag("console_clear_buffer_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear log buffer",
                        tint = CyberRed,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Direct simulation actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Inject custom warning button
                Button(
                    onClick = {
                        val timePre = "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())}]"
                        viewModel.appendTerminalLog("$timePre WARNING: Core thermal cluster protective governor triggered automatically at ${39 + Random.nextInt(12)}°C.")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .testTag("simulate_warning_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAmber.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, CyberAmber.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Text(
                        text = "+ WARN EVENT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberAmber
                    )
                }

                // Inject custom removal button
                Button(
                    onClick = {
                        val timePre = "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())}]"
                        val deactPkgs = listOf("com.google.android.apps.gcs", "com.facebook.services", "com.android.providers.partnerbookmarks", "com.sec.android.soagent")
                        viewModel.appendTerminalLog("$timePre Success: Target bloatware frozen completely: ${deactPkgs.random()}. [RECLAIM ACTIVE] freed 38.2 MB.")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .testTag("simulate_removal_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Text(
                        text = "+ REMOVE Success",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberGreen
                    )
                }

                // Inject system event button
                Button(
                    onClick = {
                        val timePre = "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())}]"
                        viewModel.appendTerminalLog("$timePre [SYSTEM] Benchmarked 8 cores. Average load utilization is ${String.format("%.1f", Random.nextFloat() * 100f)}%. Governor optimized.")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .testTag("simulate_system_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPurple.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, CyberPurple.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Text(
                        text = "+ SYSTEM EVENT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberPurple
                    )
                }
            }

            if (scanCompletedDialog != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissScanDialog() },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🛡️", fontSize = 16.sp)
                            Text(
                                text = "OS INTEGRITY REPORT COMPLETE",
                                color = CyberCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                    },
                    text = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .background(Color(0xFF05070B), RoundedCornerShape(10.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        ) {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                            ) {
                                Text(
                                    text = scanCompletedDialog ?: "",
                                    color = CyberGreen,
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    },
                    containerColor = Color(0xFF0F1219),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.border(1.dp, CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                    confirmButton = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        ) {
                            // Copy Button
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(scanCompletedDialog ?: ""))
                                    Toast.makeText(context, "Integrity report copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.1f)),
                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(36.dp).testTag("dialog_copy_scan_report_btn")
                            ) {
                                Text(
                                    text = "COPY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Close Button
                            Button(
                                onClick = { viewModel.dismissScanDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(36.dp).testTag("dialog_close_scan_report_btn")
                            ) {
                                Text(
                                    text = "CLOSE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}
