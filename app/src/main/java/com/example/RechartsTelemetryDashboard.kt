package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.ui.theme.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun RechartsTelemetryDashboard(viewModel: SystemViewModel) {
    val telemetryHistory by viewModel.telemetryHistory.collectAsState()
    val cpuTempThreshold by viewModel.cpuTempThreshold.collectAsState()
    val memUsageThreshold by viewModel.memUsageThreshold.collectAsState()
    
    val latestPoint = telemetryHistory.lastOrNull()
    val isCpuTempExceeded = latestPoint?.let { it.cpuTemp > cpuTempThreshold } ?: false
    val isMemExceeded = latestPoint?.let { it.memoryUsage > memUsageThreshold } ?: false
    
    // Series toggles - clickable legends (Recharts Signature Feature)
    var cpuEnabled by remember { mutableStateOf(true) }
    var memEnabled by remember { mutableStateOf(true) }
    var batteryTempEnabled by remember { mutableStateOf(true) }
    var cpuTempEnabled by remember { mutableStateOf(false) }

    // Touch interaction state
    var activeIndex by remember { mutableStateOf<Int?>(null) }
    var touchX by remember { mutableStateOf(0f) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recharts_dashboard_card"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isCpuTempExceeded || isMemExceeded) CyberRed.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "REAL-TIME TELEMETRY ENGINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Recharts-inspired dynamic hardware multi-series monitor",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
                
                // Active count badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberCyan.copy(alpha = 0.08f))
                        .border(1.dp, CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CyberGreen)
                        )
                        Text(
                            text = "LIVE FEED",
                            color = CyberGreen,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Threshold Warning Notification Banner
            AnimatedVisibility(
                visible = isCpuTempExceeded || isMemExceeded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberRed.copy(alpha = 0.08f))
                        .border(1.dp, CyberRed.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CyberRed)
                    )
                    Text(
                        text = buildString {
                            append("CRITICAL LOG TRIGGER MET:")
                            if (isCpuTempExceeded) append(" CPU TEMP TRIPPED (${String.format("%.1f", latestPoint?.cpuTemp)}°C > ${String.format("%.1f", cpuTempThreshold)}°C)")
                            if (isCpuTempExceeded && isMemExceeded) append(" &")
                            if (isMemExceeded) append(" MEMORY POOL SATURATION (${latestPoint?.memoryUsage?.roundToInt()}% > ${memUsageThreshold.roundToInt()}%)")
                        },
                        fontSize = 9.sp,
                        color = CyberRed,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Clickable Legends Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // CPU Legend
                LegendItem(
                    label = "CPU UTIL",
                    color = CyberCyan,
                    isEnabled = cpuEnabled,
                    onClick = { cpuEnabled = !cpuEnabled }
                )
                // Memory Legend
                LegendItem(
                    label = "RAM USED",
                    color = if (isMemExceeded) CyberRed else CyberGreen,
                    isEnabled = memEnabled,
                    onClick = { memEnabled = !memEnabled }
                )
                // Battery Temp Legend
                LegendItem(
                    label = "BAT TEMP",
                    color = CyberAmber,
                    isEnabled = batteryTempEnabled,
                    onClick = { batteryTempEnabled = !batteryTempEnabled }
                )
                // CPU Temp Legend
                LegendItem(
                    label = "CPU TEMP",
                    color = if (isCpuTempExceeded) CyberRed else CyberPurple,
                    isEnabled = cpuTempEnabled,
                    onClick = { cpuTempEnabled = !cpuTempEnabled }
                )
            }

            // Interactive Tooltip Panel (if a point is selected)
            AnimatedVisibility(
                visible = activeIndex != null && activeIndex!! < telemetryHistory.size,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                activeIndex?.let { index ->
                    if (index < telemetryHistory.size) {
                        val point = telemetryHistory[index]
                        val secondsAgo = (telemetryHistory.size - 1 - index) * 1.2
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SNAPSHOT T-${String.format("%.1f", secondsAgo)}s",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "HISTORICAL METRIC CORRELATION",
                                    fontSize = 8.sp,
                                    color = TextSecondary.copy(alpha = 0.6f)
                                )
                            }
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (cpuEnabled) {
                                    TooltipMetricBadge("CPU", "${point.cpuUsage.roundToInt()}%", CyberCyan)
                                }
                                if (memEnabled) {
                                    TooltipMetricBadge("RAM", "${point.memoryUsage.roundToInt()}%", if (point.memoryUsage > memUsageThreshold) CyberRed else CyberGreen)
                                }
                                if (batteryTempEnabled) {
                                    TooltipMetricBadge("BAT", "${String.format("%.1f", point.batteryTemp)}°C", CyberAmber)
                                }
                                if (cpuTempEnabled) {
                                    TooltipMetricBadge("CPU_T", "${String.format("%.1f", point.cpuTemp)}°C", if (point.cpuTemp > cpuTempThreshold) CyberRed else CyberPurple)
                                }
                            }
                        }
                    }
                }
            }

            // Fallback if there is not enough history data yet
            if (telemetryHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            color = CyberCyan.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "AWAITING CORE TELEMETRY PACKETS...",
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Interactive Canvas Chart
                val maxPointCount = 30
                val gridLineCount = 5
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .pointerInput(telemetryHistory) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val sectionWidth = size.width / (telemetryHistory.size.coerceAtLeast(2) - 1).toFloat()
                                    val rawIndex = (offset.x / sectionWidth).roundToInt()
                                    activeIndex = rawIndex.coerceIn(0, telemetryHistory.size - 1)
                                    touchX = offset.x
                                },
                                onDragEnd = {
                                    activeIndex = null
                                },
                                onDragCancel = {
                                    activeIndex = null
                                },
                                onDrag = { change, dragAmount ->
                                    touchX = (touchX + dragAmount.x).coerceIn(0f, size.width.toFloat())
                                    val sectionWidth = size.width / (telemetryHistory.size.coerceAtLeast(2) - 1).toFloat()
                                    val rawIndex = (touchX / sectionWidth).roundToInt()
                                    activeIndex = rawIndex.coerceIn(0, telemetryHistory.size - 1)
                                }
                            )
                        }
                        .pointerInput(telemetryHistory) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val sectionWidth = size.width / (telemetryHistory.size.coerceAtLeast(2) - 1).toFloat()
                                    val rawIndex = (offset.x / sectionWidth).roundToInt()
                                    if (activeIndex == rawIndex) {
                                        activeIndex = null
                                    } else {
                                        activeIndex = rawIndex.coerceIn(0, telemetryHistory.size - 1)
                                        touchX = offset.x
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("telemetry_canvas")
                    ) {
                        val width = size.width
                        val height = size.height
                        
                        // Margin on the left for vertical Y labels, margin on bottom for horizontal X ticks
                        val paddingLeft = 32.dp.toPx()
                        val paddingBottom = 16.dp.toPx()
                        val chartWidth = width - paddingLeft
                        val chartHeight = height - paddingBottom
                        
                        // 1. Draw Grid Lines (Recharts look)
                        for (i in 0 until gridLineCount) {
                            val ratio = i / (gridLineCount - 1).toFloat()
                            val y = ratio * chartHeight
                            
                            // Horizontal dashed lines
                            drawLine(
                                color = Color.White.copy(alpha = 0.04f),
                                start = Offset(paddingLeft, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )
                            
                            // Let's add Y Labels
                            val valuePct = ((1f - ratio) * 100f).roundToInt()
                            // Also support temperature labeling (e.g. 50C to 20C)
                            val valueLabel = if (cpuTempEnabled || batteryTempEnabled) {
                                "${valuePct / 2 + 15}°" // Range approx 15 to 65 °C
                            } else {
                                "$valuePct%"
                            }
                        }

                        // 2. Prepare coordinate scale mapper
                        val dataSize = telemetryHistory.size
                        val stepX = if (dataSize > 1) chartWidth / (dataSize - 1) else chartWidth
                        
                        // Functions to translate percentage/temperature to Y coordinates in the canvas
                        fun getYMetric(pct: Float): Float {
                            val ratio = pct.coerceIn(0f, 100f) / 100f
                            return chartHeight - ratio * chartHeight
                        }

                        fun getYTemp(temp: Float): Float {
                            // Map temperature range 15°C to 65°C to canvas 0% to 100%
                            val ratio = ((temp - 15f) / 50f).coerceIn(0f, 1f)
                            return chartHeight - ratio * chartHeight
                        }

                        // Helper list of series configs to loop over
                        data class SeriesConfig(
                            val enabled: Boolean,
                            val color: Color,
                            val values: List<Float>,
                            val isTemp: Boolean
                        )

                        val seriesList = listOf(
                            SeriesConfig(cpuEnabled, CyberCyan, telemetryHistory.map { it.cpuUsage }, false),
                            SeriesConfig(memEnabled, if (isMemExceeded) CyberRed else CyberGreen, telemetryHistory.map { it.memoryUsage }, false),
                            SeriesConfig(batteryTempEnabled, CyberAmber, telemetryHistory.map { it.batteryTemp }, true),
                            SeriesConfig(cpuTempEnabled, if (isCpuTempExceeded) CyberRed else CyberPurple, telemetryHistory.map { it.cpuTemp }, true)
                        )

                        // 3. Draw Curves & Under-Sheds (with monotonic cubic bezier curves)
                        seriesList.forEach { series ->
                            if (series.enabled && series.values.isNotEmpty()) {
                                val strokePath = Path()
                                val fillPath = Path()
                                
                                val firstX = paddingLeft
                                val firstY = if (series.isTemp) getYTemp(series.values[0]) else getYMetric(series.values[0])
                                
                                strokePath.moveTo(firstX, firstY)
                                fillPath.moveTo(paddingLeft, chartHeight) // Anchor under-fill to bottom
                                fillPath.lineTo(firstX, firstY)

                                for (i in 1 until series.values.size) {
                                    val x0 = paddingLeft + (i - 1) * stepX
                                    val y0 = if (series.isTemp) getYTemp(series.values[i - 1]) else getYMetric(series.values[i - 1])
                                    
                                    val x1 = paddingLeft + i * stepX
                                    val y1 = if (series.isTemp) getYTemp(series.values[i]) else getYMetric(series.values[i])
                                    
                                    val ctrlX1 = x0 + (x1 - x0) / 2f
                                    val ctrlY1 = y0
                                    val ctrlX2 = x0 + (x1 - x0) / 2f
                                    val ctrlY2 = y1
                                    
                                    strokePath.cubicTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, x1, y1)
                                    fillPath.cubicTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, x1, y1)
                                }
                                
                                val lastX = paddingLeft + (series.values.size - 1) * stepX
                                fillPath.lineTo(lastX, chartHeight)
                                fillPath.close()

                                // Draw Gradient Area Shield (Recharts under-glow)
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            series.color.copy(alpha = 0.12f),
                                            series.color.copy(alpha = 0.00f)
                                        ),
                                        startY = 0f,
                                        endY = chartHeight
                                    )
                                )

                                // Draw Smooth Line
                                drawPath(
                                    path = strokePath,
                                    color = series.color,
                                    style = Stroke(
                                        width = 1.8.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }

                        // 4. Draw Interaction Hover Cursor (Recharts style vertical tracker line)
                        activeIndex?.let { index ->
                            if (index < telemetryHistory.size) {
                                val cursorX = paddingLeft + index * stepX
                                
                                // Vertical line
                                drawLine(
                                    color = Color.White.copy(alpha = 0.2f),
                                    start = Offset(cursorX, 0f),
                                    end = Offset(cursorX, chartHeight),
                                    strokeWidth = 1.dp.toPx()
                                )

                                // Highlight dot on active curves
                                val point = telemetryHistory[index]
                                seriesList.forEach { series ->
                                    if (series.enabled) {
                                        val cursorY = if (series.isTemp) getYTemp(series.values[index]) else getYMetric(series.values[index])
                                        
                                        // Outer glowing circle
                                        drawCircle(
                                            color = series.color.copy(alpha = 0.25f),
                                            radius = 7.dp.toPx(),
                                            center = Offset(cursorX, cursorY)
                                        )
                                        // Inner solid circle
                                        drawCircle(
                                            color = series.color,
                                            radius = 3.5.dp.toPx(),
                                            center = Offset(cursorX, cursorY)
                                        )
                                        // White target dot core
                                        drawCircle(
                                            color = Color.White,
                                            radius = 1.5.dp.toPx(),
                                            center = Offset(cursorX, cursorY)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Native Compose Text Canvas Overlays for Y labels (rendering clean, monospace markers)
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(32.dp)
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        for (i in 0 until gridLineCount) {
                            val valuePct = ((gridLineCount - 1 - i) / (gridLineCount - 1).toFloat() * 100f).roundToInt()
                            val prefixText = if (cpuTempEnabled || batteryTempEnabled) {
                                "${valuePct / 2 + 15}°"
                            } else {
                                "$valuePct%"
                            }
                            Text(
                                text = prefixText,
                                fontSize = 8.sp,
                                color = TextSecondary.copy(alpha = 0.5f),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Time marker labels along X axis at the bottom right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomEnd)
                            .padding(start = 36.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "T-30s AGO",
                            fontSize = 8.sp,
                            color = TextSecondary.copy(alpha = 0.4f),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberCyan.copy(alpha = 0.05f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "TAP & DRAG TO SCRUB RECORDINGS",
                                fontSize = 7.5.sp,
                                color = CyberCyan.copy(alpha = 0.7f),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "NOW",
                            fontSize = 8.sp,
                            color = CyberCyan.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            var showThresholdConfig by remember { mutableStateOf(false) }

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showThresholdConfig = !showThresholdConfig }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showThresholdConfig) Icons.Default.KeyboardArrowUp else Icons.Default.Settings,
                            contentDescription = "Settings Icon",
                            tint = CyberCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "SAFETY ALERT THRESHOLDS CONFIG",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyan
                        )
                    }
                    Text(
                        text = if (showThresholdConfig) "COLLAPSE" else "EXPAND CONFIG",
                        fontSize = 8.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                AnimatedVisibility(
                    visible = showThresholdConfig,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Slider for CPU Temperature safety limit
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "CPU TEMP TRIGGER LIMIT",
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${String.format("%.1f", cpuTempThreshold)}°C",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCpuTempExceeded) CyberRed else CyberCyan
                                )
                            }
                            Slider(
                                value = cpuTempThreshold,
                                onValueChange = { viewModel.updateCpuTempThreshold(it) },
                                valueRange = 30f..55f,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (isCpuTempExceeded) CyberRed else CyberCyan,
                                    activeTrackColor = if (isCpuTempExceeded) CyberRed else CyberCyan,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                                ),
                                modifier = Modifier.height(24.dp).testTag("slider_cpu_threshold")
                            )
                        }

                        // Slider for Memory Usage safety limit
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "RAM SATURATION TRIGGER LIMIT",
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${memUsageThreshold.roundToInt()}%",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMemExceeded) CyberRed else CyberGreen
                                )
                            }
                            Slider(
                                value = memUsageThreshold,
                                onValueChange = { viewModel.updateMemUsageThreshold(it) },
                                valueRange = 50f..95f,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (isMemExceeded) CyberRed else CyberCyan,
                                    activeTrackColor = if (isMemExceeded) CyberRed else CyberCyan,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                                ),
                                modifier = Modifier.height(24.dp).testTag("slider_mem_threshold")
                            )
                        }
                    }
                }
            }

            var showReportDialog by remember { mutableStateOf(false) }
            var generatedReportText by remember { mutableStateOf("") }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(10.dp))

            // Health Diagnostics Trigger Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.015f))
                    .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
                    .clickable {
                        generatedReportText = viewModel.generateSystemHealthReport()
                        showReportDialog = true
                        val timePre = "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())}]"
                        val latestPoint = telemetryHistory.lastOrNull()
                        val score = if (latestPoint != null) {
                            var tmp = 100
                            if (latestPoint.cpuTemp > cpuTempThreshold) tmp -= 20
                            if (latestPoint.memoryUsage > memUsageThreshold) tmp -= 20
                            tmp
                        } else 100
                        viewModel.appendTerminalLog("$timePre [SYSTEM] Diagnostics trigger executed. Safety Index Score is $score%. Diagnostics report prepared.")
                    }
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GENERATE WELLNESS REPORT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
                    Text(
                        text = "Sum diagnostics log from active trend buffer",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Button(
                    onClick = {
                        generatedReportText = viewModel.generateSystemHealthReport()
                        showReportDialog = true
                        val timePre = "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())}]"
                        val latestPoint = telemetryHistory.lastOrNull()
                        val score = if (latestPoint != null) {
                            var tmp = 100
                            if (latestPoint.cpuTemp > cpuTempThreshold) tmp -= 20
                            if (latestPoint.memoryUsage > memUsageThreshold) tmp -= 20
                            tmp
                        } else 100
                        viewModel.appendTerminalLog("$timePre [SYSTEM] Diagnostics trigger executed. Safety Index Score is $score%. Diagnostics report prepared.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(28.dp).testTag("btn_generate_health_report")
                ) {
                    Text(
                        text = "RUN REPORT",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (showReportDialog) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                
                AlertDialog(
                    onDismissRequest = { showReportDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "📊", fontSize = 16.sp)
                            Text(
                                text = "SYSTEM HEALTH REPORT",
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
                                    text = generatedReportText,
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        ) {
                            // Inject to Console Button
                            Button(
                                onClick = {
                                    generatedReportText.split("\n").forEach { line ->
                                        if (line.isNotBlank() && !line.startsWith("===") && !line.startsWith("---")) {
                                            viewModel.appendTerminalLog(line)
                                        }
                                    }
                                    android.widget.Toast.makeText(context, "Full diagnostic log injected to kernel console!", android.widget.Toast.LENGTH_SHORT).show()
                                    showReportDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPurple.copy(alpha = 0.1f)),
                                border = BorderStroke(1.dp, CyberPurple.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.1f).height(34.dp).testTag("dialog_inject_report_btn")
                            ) {
                                Text(
                                    text = "INJECT CONSOLE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberPurple,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Copy Button
                            Button(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(generatedReportText))
                                    android.widget.Toast.makeText(context, "Report copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen.copy(alpha = 0.1f)),
                                border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(0.9f).height(34.dp).testTag("dialog_copy_report_btn")
                            ) {
                                Text(
                                    text = "COPY",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Dismiss Button
                            Button(
                                onClick = { showReportDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(0.7f).height(34.dp).testTag("dialog_close_report_btn")
                            ) {
                                Text(
                                    text = "CLOSE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    },
                    dismissButton = null
                )
            }
        }
    }
}

@Composable
fun LegendItem(
    label: String,
    color: Color,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .background(if (isEnabled) color.copy(alpha = 0.05f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isEnabled) color.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.04f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isEnabled) color else color.copy(alpha = 0.2f))
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (isEnabled) color else TextSecondary.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun TooltipMetricBadge(
    title: String,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$title:",
            fontSize = 8.sp,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            fontFamily = FontFamily.Monospace
        )
    }
}
