package com.sondeptrai.procam.ui.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.procam.core.camera.CameraUiState
import kotlin.math.abs

// Standard photographic ISO stops matching Halide Mark II
val HALIDE_ISO_VALUES = listOf(
    "AUTO" to -1,
    "50" to 50,
    "64" to 64,
    "80" to 80,
    "100" to 100,
    "125" to 125,
    "160" to 160,
    "200" to 200,
    "250" to 250,
    "320" to 320,
    "400" to 400,
    "500" to 500,
    "640" to 640,
    "800" to 800,
    "1000" to 1000,
    "1250" to 1250,
    "1600" to 1600,
    "2000" to 2000,
    "2500" to 2500,
    "3200" to 3200,
    "6400" to 6400
)

// Halide signature warm amber accent
val HalideGold = Color(0xFFFF9500)
val HalideDark = Color(0xFF0C0C0E)
val HalideSurface = Color(0xFF18181C)

@Composable
fun HalideIsoDial(
    state: CameraUiState,
    onIsoChanged: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemSpacingDp = 46.dp
    val itemSpacingPx = with(density) { itemSpacingDp.toPx() }

    val stepCount = HALIDE_ISO_VALUES.size
    val currentIndex = if (state.isAutoIso) {
        0
    } else {
        val found = HALIDE_ISO_VALUES.indexOfFirst { it.second == state.currentIso }
        if (found != -1) found else 4 // default ~100
    }.coerceIn(0, stepCount - 1)

    var dragOffset by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(HalideDark)
            .padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Halide Header: Value readout & AUTO Switch Chip ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ISO Label & Active Value
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ISO ",
                    color = Color(0xFF8E8E93),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (state.isAutoIso) "AUTO (${state.currentIso})" else "${state.currentIso}",
                    color = if (state.isAutoIso) Color(0xFF34C759) else HalideGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Halide Iconic AUTO Toggle Pill
            val autoBg by animateColorAsState(
                targetValue = if (state.isAutoIso) HalideGold else HalideSurface,
                label = "autoBg"
            )
            val autoTextColor by animateColorAsState(
                targetValue = if (state.isAutoIso) Color.Black else Color(0xFF8E8E93),
                label = "autoText"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(autoBg)
                    .border(1.dp, if (state.isAutoIso) HalideGold else Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (state.isAutoIso) {
                            // Switch to manual ISO 100
                            onIsoChanged(100, false)
                        } else {
                            // Switch to Auto ISO
                            onIsoChanged(state.currentIso, true)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AUTO",
                    color = autoTextColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // --- 2. Halide 3D Rotary Drum Dial ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .pointerInput(stepCount, currentIndex) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragOffset = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount
                            val stepsToMove = (-dragOffset / itemSpacingPx).toInt()
                            if (stepsToMove != 0) {
                                val newIdx = (currentIndex + stepsToMove).coerceIn(0, stepCount - 1)
                                if (newIdx != currentIndex) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val (_, targetIso) = HALIDE_ISO_VALUES[newIdx]
                                    if (targetIso == -1) {
                                        onIsoChanged(state.currentIso, true)
                                    } else {
                                        onIsoChanged(targetIso, false)
                                    }
                                    dragOffset += stepsToMove * itemSpacingPx
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Background ticks & drum graduation lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val centerX = width / 2f
                val centerY = 16.dp.toPx()

                val visibleCount = (width / (2f * itemSpacingPx)).toInt() + 2

                for (offset in -visibleCount..visibleCount) {
                    val idx = currentIndex + offset
                    if (idx in 0 until stepCount) {
                        val tickX = centerX + (offset * itemSpacingPx) + dragOffset
                        val isCenter = offset == 0 && abs(dragOffset) < itemSpacingPx / 2f
                        val isMajorStop = idx == 0 || HALIDE_ISO_VALUES[idx].second in listOf(100, 200, 400, 800, 1600, 3200, 6400)

                        val tickH = if (isMajorStop) 12.dp.toPx() else 6.dp.toPx()
                        val tickColor = if (isCenter) HalideGold else if (isMajorStop) Color(0xFFC7C7CC) else Color(0xFF48484A)

                        drawLine(
                            color = tickColor,
                            start = Offset(tickX, centerY - tickH),
                            end = Offset(tickX, centerY),
                            strokeWidth = if (isCenter) 2.dp.toPx() else 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Engraved ISO Numbers (3D Rolling Cylindrical perspective)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val visibleIndices = (-3..3)
                for (offset in visibleIndices) {
                    val idx = currentIndex + offset
                    if (idx in 0 until stepCount) {
                        val (label, targetIso) = HALIDE_ISO_VALUES[idx]
                        val isCenter = offset == 0

                        // 3D perspective scale and alpha falloff
                        val distance = abs(offset)
                        val alpha = when (distance) {
                            0 -> 1.0f
                            1 -> 0.70f
                            2 -> 0.40f
                            else -> 0.15f
                        }
                        val fontSize = when (distance) {
                            0 -> 14.sp
                            1 -> 12.sp
                            2 -> 10.sp
                            else -> 9.sp
                        }
                        val fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Medium
                        val textColor = if (isCenter) {
                            if (state.isAutoIso) Color(0xFF34C759) else HalideGold
                        } else {
                            Color.White.copy(alpha = alpha)
                        }

                        Box(
                            modifier = Modifier
                                .width(itemSpacingDp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (targetIso == -1) {
                                        onIsoChanged(state.currentIso, true)
                                    } else {
                                        onIsoChanged(targetIso, false)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = textColor,
                                fontSize = fontSize,
                                fontWeight = fontWeight,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(itemSpacingDp))
                    }
                }
            }

            // Center Cursor / Index Indicator (Halide Marker)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp)
                    .width(3.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(HalideGold)
            )

            // Left Nudge Arrow Button (‹)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(36.dp)
                    .clickable {
                        if (currentIndex > 0) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val (_, iso) = HALIDE_ISO_VALUES[currentIndex - 1]
                            onIsoChanged(if (iso == -1) state.currentIso else iso, iso == -1)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous ISO",
                    tint = if (currentIndex > 0) Color(0xFF8E8E93) else Color(0xFF2C2C2E),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Right Nudge Arrow Button (›)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(36.dp)
                    .clickable {
                        if (currentIndex < stepCount - 1) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val (_, iso) = HALIDE_ISO_VALUES[currentIndex + 1]
                            onIsoChanged(if (iso == -1) state.currentIso else iso, iso == -1)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next ISO",
                    tint = if (currentIndex < stepCount - 1) Color(0xFF8E8E93) else Color(0xFF2C2C2E),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
