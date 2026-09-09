package com.sondeptrai.procam.ui.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
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

// Standard photographic EV stops matching Halide Mark II (step = 1/3 EV)
val HALIDE_EV_VALUES = listOf(
    "-4.0" to -12,
    "-3.5" to -11,
    "-3.0" to -9,
    "-2.5" to -8,
    "-2.0" to -6,
    "-1.5" to -5,
    "-1.0" to -3,
    "-0.5" to -2,
    "0.0" to 0,
    "+0.5" to 2,
    "+1.0" to 3,
    "+1.5" to 5,
    "+2.0" to 6,
    "+2.5" to 8,
    "+3.0" to 9,
    "+3.5" to 11,
    "+4.0" to 12
)

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
    onEvChanged: (Int) -> Unit = {},
    initialModeIsEv: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isEvMode by remember(initialModeIsEv) { mutableStateOf(initialModeIsEv) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemSpacingDp = 44.dp
    val itemSpacingPx = with(density) { itemSpacingDp.toPx() }

    val valuesList = if (isEvMode) HALIDE_EV_VALUES else HALIDE_ISO_VALUES
    val stepCount = valuesList.size

    val currentIndex = if (isEvMode) {
        val found = HALIDE_EV_VALUES.indexOfFirst { it.second == state.currentEvIndex }
        if (found != -1) found else 8 // index 8 is "0.0"
    } else {
        if (state.isAutoIso) {
            0
        } else {
            val found = HALIDE_ISO_VALUES.indexOfFirst { it.second == state.currentIso }
            if (found != -1) found else 4 // default ~100
        }
    }.coerceIn(0, stepCount - 1)

    // Remember updated states for drag gesture so coroutine doesn't restart when index changes
    val currentValuesList by rememberUpdatedState(valuesList)
    val currentStepCount by rememberUpdatedState(stepCount)
    val currentIdx by rememberUpdatedState(currentIndex)
    val currentOnEvChanged by rememberUpdatedState(onEvChanged)
    val currentOnIsoChanged by rememberUpdatedState(onIsoChanged)
    val currentIsEvMode by rememberUpdatedState(isEvMode)
    val currentIsoVal by rememberUpdatedState(state.currentIso)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(HalideDark)
            .padding(top = 4.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Halide Header: Mode tabs, Quick Presets (+3.0), and AUTO Switch Pill ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Mode Tabs [ EV ] & [ ISO ]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // EV Tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isEvMode) HalideGold else HalideSurface)
                        .clickable { isEvMode = true }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "EV (BÙ SÁNG)",
                        color = if (isEvMode) Color.Black else Color(0xFF8E8E93),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // ISO Tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isEvMode) HalideGold else HalideSurface)
                        .clickable { isEvMode = false }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "ISO",
                        color = if (!isEvMode) Color.Black else Color(0xFF8E8E93),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Center Quick Presets (when in EV mode: tap to instantly jump to +1, +2, +3.0)
            if (isEvMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("0.0" to 0, "+1.0" to 3, "+2.0" to 6, "+3.0" to 9).forEach { (presetLabel, presetIndex) ->
                        val isPresetActive = state.currentEvIndex == presetIndex
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isPresetActive) HalideGold else Color(0xFF242428))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onEvChanged(presetIndex)
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = presetLabel,
                                color = if (isPresetActive) Color.Black else if (presetIndex == 9) HalideGold else Color.White,
                                fontSize = 10.sp,
                                fontWeight = if (isPresetActive || presetIndex == 9) FontWeight.ExtraBold else FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            } else {
                // ISO Readout
                Text(
                    text = if (state.isAutoIso) "AUTO (${state.currentIso})" else "${state.currentIso}",
                    color = HalideGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Right: AUTO Reset Pill
            val isAutoActive = if (isEvMode) state.currentEvIndex == 0 else state.isAutoIso
            val autoBg by animateColorAsState(
                targetValue = if (isAutoActive) HalideGold else HalideSurface,
                label = "autoBg"
            )
            val autoTextColor by animateColorAsState(
                targetValue = if (isAutoActive) Color.Black else Color(0xFF8E8E93),
                label = "autoText"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(autoBg)
                    .border(1.dp, if (isAutoActive) HalideGold else Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (isEvMode) {
                            onEvChanged(0) // Reset EV to neutral 0.0
                        } else {
                            if (state.isAutoIso) {
                                onIsoChanged(100, false)
                            } else {
                                onIsoChanged(state.currentIso, true)
                            }
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AUTO",
                    color = autoTextColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // --- 2. Halide 3D Rotary Drum Dial ---
        var dragAccumulator by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .pointerInput(isEvMode) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragAccumulator += dragAmount
                            val stepDelta = (-dragAccumulator / itemSpacingPx).toInt()
                            if (stepDelta != 0) {
                                val targetIdx = (currentIdx + stepDelta).coerceIn(0, currentStepCount - 1)
                                dragAccumulator += stepDelta * itemSpacingPx
                                if (targetIdx != currentIdx) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (currentIsEvMode) {
                                        currentOnEvChanged(HALIDE_EV_VALUES[targetIdx].second)
                                    } else {
                                        val targetIso = HALIDE_ISO_VALUES[targetIdx].second
                                        currentOnIsoChanged(if (targetIso == -1) currentIsoVal else targetIso, targetIso == -1)
                                    }
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
                        val tickX = centerX + (offset * itemSpacingPx)
                        val isCenter = offset == 0
                        val isMajorStop = if (isEvMode) {
                            val target = HALIDE_EV_VALUES[idx].first
                            target in listOf("-4.0", "-3.0", "-2.0", "-1.0", "0.0", "+1.0", "+2.0", "+3.0", "+4.0")
                        } else {
                            idx == 0 || HALIDE_ISO_VALUES[idx].second in listOf(100, 200, 400, 800, 1600, 3200, 6400)
                        }

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

            // Engraved Numbers (3D Rolling Cylindrical perspective)
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
                        val (label, _) = valuesList[idx]
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
                            if (isEvMode) {
                                if (state.currentEvIndex == 0) Color(0xFF34C759) else HalideGold
                            } else {
                                if (state.isAutoIso) Color(0xFF34C759) else HalideGold
                            }
                        } else {
                            Color.White.copy(alpha = alpha)
                        }

                        Box(
                            modifier = Modifier
                                .width(itemSpacingDp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (isEvMode) {
                                        val (_, targetEv) = HALIDE_EV_VALUES[idx]
                                        onEvChanged(targetEv)
                                    } else {
                                        val (_, targetIso) = HALIDE_ISO_VALUES[idx]
                                        if (targetIso == -1) {
                                            onIsoChanged(state.currentIso, true)
                                        } else {
                                            onIsoChanged(targetIso, false)
                                        }
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
                            if (isEvMode) {
                                val (_, ev) = HALIDE_EV_VALUES[currentIndex - 1]
                                onEvChanged(ev)
                            } else {
                                val (_, iso) = HALIDE_ISO_VALUES[currentIndex - 1]
                                onIsoChanged(if (iso == -1) state.currentIso else iso, iso == -1)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Stop",
                    tint = if (currentIndex > 0) Color(0xFF8E8E93) else Color(0xFF2C2C2E),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Right Nudge Arrow Button (›) - click to step towards +3.0
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(36.dp)
                    .clickable {
                        if (currentIndex < stepCount - 1) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (isEvMode) {
                                val (_, ev) = HALIDE_EV_VALUES[currentIndex + 1]
                                onEvChanged(ev)
                            } else {
                                val (_, iso) = HALIDE_ISO_VALUES[currentIndex + 1]
                                onIsoChanged(if (iso == -1) state.currentIso else iso, iso == -1)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Stop",
                    tint = if (currentIndex < stepCount - 1) Color(0xFF8E8E93) else Color(0xFF2C2C2E),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
