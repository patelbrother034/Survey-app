package com.example.ui.widgets

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GpsPoint
import com.example.data.model.SurveyEntry
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.sqrt

@Composable
fun InteractiveMap(
    entries: List<SurveyEntry>,
    gpsPoints: List<GpsPoint>,
    filterUser: String? = null,
    filterVillage: String? = null,
    modifier: Modifier = Modifier
) {
    var zoomScale by remember { mutableStateOf(1.2f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedEntry by remember { mutableStateOf<SurveyEntry?>(null) }
    
    // Pulse animation for markers
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    // Filter data
    val filteredEntries = remember(entries, filterUser, filterVillage) {
        entries.filter { ent ->
            val uMatch = filterUser == null || ent.surveyorName.contains(filterUser, true) || ent.surveyorId == filterUser
            val vMatch = filterVillage == null || ent.village.contains(filterVillage, true)
            uMatch && vMatch
        }
    }

    val filteredGpsPoints = remember(gpsPoints, filterUser) {
        if (filterUser == null) gpsPoints else gpsPoints.filter { it.userId == filterUser }
    }

    // Determine bounding boxes to auto-fit
    val baseLat = 23.2156
    val baseLng = 72.6369

    val textMeasurer = rememberTextMeasurer()

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF141F1A))) {
        // Map Canvas Drawing Panel
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(filteredEntries) {
                    detectTapGestures { tapOffset ->
                        // Reverse project to find clicked elements
                        val width = size.width.toFloat()
                        val height = size.height.toFloat()
                        val cx = width / 2f + panOffset.x
                        val cy = height / 2f + panOffset.y

                        var clicked: SurveyEntry? = null
                        var bestDist = 24.dp.toPx()

                        for (ent in filteredEntries) {
                            // Project (lat, lng) to canvas pixels
                            val dx = (ent.longitude - baseLng) * 4500f * zoomScale
                            val dy = -(ent.latitude - baseLat) * 4500f * zoomScale
                            val markerPos = Offset(cx + dx.toFloat(), cy + dy.toFloat())

                            val dist = sqrt((tapOffset.x - markerPos.x) * (tapOffset.x - markerPos.x) + 
                                            (tapOffset.y - markerPos.y) * (tapOffset.y - markerPos.y))
                            if (dist < bestDist) {
                                bestDist = dist
                                clicked = ent
                            }
                        }
                        selectedEntry = clicked
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val cx = width / 2f + panOffset.x
            val cy = height / 2f + panOffset.y

            // Draw topography grid (latitude/longitude guidelines)
            val gridSpacing = 80.dp.toPx() * zoomScale
            val gridColor = Color(0x1A2E8B57)
            val gridStroke = Stroke(width = 1.dp.toPx())

            if (gridSpacing >= 20f && !gridSpacing.isNaN() && !gridSpacing.isInfinite()) {
                // Horizontal gridlines
                var y = cy % gridSpacing
                while (y < 0) {
                    y += gridSpacing
                }
                while (y < height) {
                    drawLine(gridColor, Offset(0f, y), Offset(width, y), strokeWidth = gridStroke.width)
                    y += gridSpacing
                }
                // Vertical gridlines
                var x = cx % gridSpacing
                while (x < 0) {
                    x += gridSpacing
                }
                while (x < width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, height), strokeWidth = gridStroke.width)
                    x += gridSpacing
                }
            }

            // Draw basic topological landscape water body reference lines
            val riverPath = Path().apply {
                moveTo(cx - 300f * zoomScale, cy + 400f * zoomScale)
                quadraticTo(
                    cx - 50f * zoomScale, cy - 100f * zoomScale,
                    cx + 400f * zoomScale, cy - 300f * zoomScale
                )
            }
            drawPath(
                riverPath,
                color = Color(0x332E8B57), // translucent green river
                style = Stroke(width = 24.dp.toPx() * zoomScale, cap = StrokeCap.Round)
            )

            // Draw GPS Route lines (connections between tracking points of same user)
            val userGpsGroups = filteredGpsPoints.groupBy { it.userId }
            val routeColor = Color(0xAA4CAF50)
            val routeStroke = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))

            userGpsGroups.forEach { (_, points) ->
                if (points.size >= 2) {
                    val sortedPoints = points.sortedBy { it.timestamp }
                    for (i in 0 until sortedPoints.size - 1) {
                        val p1 = sortedPoints[i]
                        val p2 = sortedPoints[i + 1]

                        val dx1 = (p1.longitude - baseLng) * 4500f * zoomScale
                        val dy1 = -(p1.latitude - baseLat) * 4500f * zoomScale
                        val pos1 = Offset(cx + dx1.toFloat(), cy + dy1.toFloat())

                        val dx2 = (p2.longitude - baseLng) * 4500f * zoomScale
                        val dy2 = -(p2.latitude - baseLat) * 4500f * zoomScale
                        val pos2 = Offset(cx + dx2.toFloat(), cy + dy2.toFloat())

                        // Check if points are on screen before drawing
                        if (pos1.x in -100f..width + 100f && pos1.y in -100f..height + 100f) {
                            drawLine(routeColor, pos1, pos2, strokeWidth = routeStroke.width, pathEffect = routeStroke.pathEffect)
                        }
                    }
                }
            }

            // Draw GPS historic points themselves (tiny green circles)
            filteredGpsPoints.forEach { pt ->
                val dx = (pt.longitude - baseLng) * 4500f * zoomScale
                val dy = -(pt.latitude - baseLat) * 4500f * zoomScale
                val pos = Offset(cx + dx.toFloat(), cy + dy.toFloat())

                if (pos.x in 0f..width && pos.y in 0f..height) {
                    drawCircle(
                        color = Color(0x8081C784),
                        radius = 4.dp.toPx(),
                        center = pos
                    )
                }
            }

            // Draw Survey entries (Pulsing high visibility markers)
            filteredEntries.forEach { ent ->
                val dx = (ent.longitude - baseLng) * 4500f * zoomScale
                val dy = -(ent.latitude - baseLat) * 4500f * zoomScale
                val pos = Offset(cx + dx.toFloat(), cy + dy.toFloat())

                if (pos.x in -50f..width+50f && pos.y in -50f..height+50f) {
                    // 1. Draw pulsing outer circle animation
                    drawCircle(
                        color = Color(0xFFFFB74D).copy(alpha = pulseAlpha),
                        radius = pulseRadius * zoomScale,
                        center = pos
                    )
                    // 2. Draw solid primary marker center
                    val markerColor = if (ent.isDraft) Color(0xFFFFA726) else Color(0xFFE53935)
                    drawCircle(
                        color = markerColor,
                        radius = 7.dp.toPx(),
                        center = pos
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = pos
                    )

                    // 3. Label text (Village/File number)
                    val label = ent.fileNumber ?: ent.village
                    val textTopLeft = Offset(pos.x + 8.dp.toPx(), pos.y - 8.dp.toPx())
                    try {
                        val textLayoutResult = textMeasurer.measure(
                            text = label,
                            style = TextStyle(color = Color.White, fontSize = 10.sp, background = Color(0xAA000000))
                        )
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = textTopLeft
                        )
                    } catch (e: Exception) {
                        // Safe fallback: omit label if text measurement or drawing fails under extreme constraints
                    }
                }
            }
        }

        // Overlay Map Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .background(Color(0xE61A2722), RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            IconButton(onClick = { zoomScale = (zoomScale * 1.2f).coerceAtMost(5.0f) }) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
            }
            IconButton(onClick = { zoomScale = (zoomScale / 1.2f).coerceAtLeast(0.3f) }) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
            }
            IconButton(onClick = {
                zoomScale = 1.2f
                panOffset = Offset.Zero
            }) {
                Icon(Icons.Default.MyLocation, contentDescription = "Recenter Map", tint = Color.White)
            }
        }

        // Map Calibration Info Label
        Text(
            text = "Offline Topography Layer: Gandhinagar Core (Grid scale: ${(5 * zoomScale).toInt()} km)",
            color = Color(0xFF81C784),
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color(0x99000000), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        // Selected Entry details overlay popup
        AnimatedVisibility(
            visible = selectedEntry != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            selectedEntry?.let { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFA1A2722)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        IconButton(
                            onClick = { selectedEntry = null },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                        }

                        Row(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                            // Map Mock image preview
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                                    .align(Alignment.CenterVertically),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Image, contentDescription = "Image preview", tint = Color.Gray, modifier = Modifier.size(36.dp))
                                Text(
                                    text = "PHOTO",
                                    color = Color.LightGray,
                                    fontSize = 9.sp,
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = "File: ${entry.fileNumber ?: "Independent GPS capture"}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "Respondent: ${entry.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFA7FFEB)
                                )
                                Text(
                                    text = "Village/District: ${entry.village}, ${entry.district}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Surveyed by: ${entry.surveyorName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB0BEC5)
                                )
                                Text(
                                    text = "GPS Coordinate: ${String.format("%.4f", entry.latitude)}, ${String.format("%.4f", entry.longitude)} (±${String.format("%.1f", entry.accuracy)}m)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF81C784)
                                )
                                Text(
                                    text = "Date: ${SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(entry.timestamp)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
