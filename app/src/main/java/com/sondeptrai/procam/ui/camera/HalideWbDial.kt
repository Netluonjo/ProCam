package com.sondeptrai.procam.ui.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

data class WbStop(
    val label: String,
    val kelvin: Int,
    val isAuto: Boolean = false,
    val indicatorColor: Color = Color.White
)

val HALIDE_WB_VALUES = listOf(
    WbStop("AUTO", -1, isAuto = true, indicatorColor = Color(0xFFFFB800)),
    WbStop("2000K", 2000, indicatorColor = Color(0xFFFF8000)),
    WbStop("2400K", 2400, indicatorColor = Color(0xFFFF9933)),
    WbStop("2800K", 2800, indicatorColor = Color(0xFFFFB366)),
    WbStop("3200K", 3200, indicatorColor = Color(0xFFFFCC99)),
    WbStop("3600K", 3600, indicatorColor = Color(0xFFFFE0B2)),
    WbStop("4000K", 4000, indicatorColor = Color(0xFFFFF0D0)),
    WbStop("4500K", 4500, indicatorColor = Color(0xFFFFFBE8)),
    WbStop("5000K", 5000, indicatorColor = Color(0xFFFFFFFF)),
    WbStop("5500K", 5500, indicatorColor = Color(0xFFF0F6FF)),
    WbStop("6000K", 6000, indicatorColor = Color(0xFFE0EEFF)),
    WbStop("6500K", 6500, indicatorColor = Color(0xFFCCE4FF)),
    WbStop("7000K", 7000, indicatorColor = Color(0xFFB8D8FF)),
    WbStop("7500K", 7500, indicatorColor = Color(0xFFA3CCFF)),
    WbStop("8000K", 8000, indicatorColor = Color(0xFF8EC0FF)),
    WbStop("8500K", 8500, indicatorColor = Color(0xFF7AB4FF)),
    WbStop("9000K", 9000, indicatorColor = Color(0xFF66A8FF)),
    WbStop("10000K", 10000, indicatorColor = Color(0xFF529CFF))
)

@Composable
fun HalideWbDial(
    state: CameraUiState,
    onKelvinChanged: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemWidthDp = 62.dp
    val itemWidthPx = with(density) { itemWidthDp.toPx() }

    val currentStopIndex = if (state.isAutoWb) {
        0
    } else {
        val found = HALIDE_WB_VALUES.drop(1).minByOrNull { abs(it.kelvin - state.currentKelvin) }
        val idx = HALIDE_WB_VALUES.indexOf(found)
        if (idx != -1) idx else 9 // default ~5500K
    }

    var dragOffset by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0F12))
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Header with Label, Value readout & Quick preset pills ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Label
            Text(
                text = "WHITE BALANCE",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Value Readout
            val wbReadout = if (state.isAutoWb) {
                "AUTO AWB"
            } else {
                val tag = when {
                    state.currentKelvin <= 3200 -> "💡 TUNGSTEN"
                    state.currentKelvin <= 4500 -> "💡 FLUOR"
                    state.currentKelvin <= 5800 -> "☀️ SUN"
                    state.currentKelvin <= 6800 -> "☁️ CLOUD"
                    state.currentKelvin <= 7800 -> "🌲 SHADE"
                    else -> "🌌 TWILIGHT"
                }
                "${state.currentKelvin}K $tag"
            }

            Text(
                text = wbReadout,
                color = Color(0xFFFFB800),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.weight(1f))

            // Quick Preset Pills (Horizontal scrollable if narrow)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // AUTO Button
                WbPresetButton(
                    label = "AUTO",
                    isSelected = state.isAutoWb,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onKelvinChanged(5500, true)
                    }
                )

                // 2800K Tungsten
                val is2800 = !state.isAutoWb && abs(state.currentKelvin - 2800) < 300
                WbPresetButton(
                    label = "2800K 💡",
                    isSelected = is2800,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onKelvinChanged(2800, false)
                    }
                )

                // 4000K Fluorescent
                val is4000 = !state.isAutoWb && abs(state.currentKelvin - 4000) < 300
                WbPresetButton(
                    label = "4000K 💡",
                    isSelected = is4000,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onKelvinChanged(4000, false)
                    }
                )

                // 5500K Daylight
                val is5500 = !state.isAutoWb && abs(state.currentKelvin - 5500) < 300
                WbPresetButton(
                    label = "5500K ☀️",
                    isSelected = is5500,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onKelvinChanged(5500, false)
                    }
                )

                // 6500K Cloudy
                val is6500 = !state.isAutoWb && abs(state.currentKelvin - 6500) < 300
                WbPresetButton(
                    label = "6500K ☁️",
                    isSelected = is6500,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onKelvinChanged(6500, false)
                    }
                )

                // 7500K Shade
                val is7500 = !state.isAutoWb && abs(state.currentKelvin - 7500) < 300
                WbPresetButton(
                    label = "7500K 🌲",
                    isSelected = is7500,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onKelvinChanged(7500, false)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- 2. 3D Rotary Drum Wheel for White Balance ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .pointerInput(currentStopIndex) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragOffset = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount
                            val stepsToMove = (-dragOffset / itemWidthPx).toInt()
                            if (stepsToMove != 0) {
                                val nextIdx = (currentStopIndex + stepsToMove).coerceIn(0, HALIDE_WB_VALUES.size - 1)
                                if (nextIdx != currentStopIndex) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val stop = HALIDE_WB_VALUES[nextIdx]
                                    onKelvinChanged(if (stop.isAuto) 5500 else stop.kelvin, stop.isAuto)
                                    dragOffset += stepsToMove * itemWidthPx
                                }
                            }
                        }
                    )
                }
        ) {
            // Tick Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val cx = w / 2f

                val visibleCount = (w / (2f * itemWidthPx)).toInt() + 2

                for (offset in -visibleCount..visibleCount) {
                    val idx = currentStopIndex + offset
                    if (idx in HALIDE_WB_VALUES.indices) {
                        val tickX = cx + (offset * itemWidthPx) + dragOffset
                        val isCenter = idx == currentStopIndex

                        val tickHeight = if (isCenter) 22.dp.toPx() else 12.dp.toPx()
                        val tickWidth = if (isCenter) 2.5.dp.toPx() else 1.2.dp.toPx()
                        val tickColor = if (isCenter) Color(0xFFFFB800) else Color.White.copy(alpha = 0.35f)

                        drawLine(
                            color = tickColor,
                            start = Offset(tickX, 8.dp.toPx()),
                            end = Offset(tickX, 8.dp.toPx() + tickHeight),
                            strokeWidth = tickWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Number/Text Labels in 3D perspective with temperature color dot
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 28.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (offset in -3..3) {
                    val idx = currentStopIndex + offset
                    if (idx in HALIDE_WB_VALUES.indices) {
                        val stop = HALIDE_WB_VALUES[idx]
                        val isCenter = idx == currentStopIndex
                        val alpha = when (abs(offset)) {
                            0 -> 1.0f
                            1 -> 0.65f
                            2 -> 0.35f
                            else -> 0.15f
                        }
                        val scale = when (abs(offset)) {
                            0 -> 1.15f
                            1 -> 0.90f
                            2 -> 0.75f
                            else -> 0.60f
                        }

                        Column(
                            modifier = Modifier
                                .width(itemWidthDp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onKelvinChanged(if (stop.isAuto) 5500 else stop.kelvin, stop.isAuto)
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stop.label,
                                color = if (isCenter) Color(0xFFFFB800) else Color.White.copy(alpha = alpha),
                                fontSize = (11 * scale).sp,
                                fontWeight = if (isCenter) FontWeight.Black else FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // Temperature Color indicator dot
                            Box(
                                modifier = Modifier
                                    .size((4 * scale).dp)
                                    .clip(CircleShape)
                                    .background(stop.indicatorColor.copy(alpha = alpha))
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(itemWidthDp))
                    }
                }
            }

            // Fixed Center Indicator Line
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(2.5.dp)
                    .height(26.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color(0xFFFFB800))
            )

            // Left Nudge arrow (‹)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(36.dp)
                    .clickable {
                        if (currentStopIndex > 0) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val stop = HALIDE_WB_VALUES[currentStopIndex - 1]
                            onKelvinChanged(if (stop.isAuto) 5500 else stop.kelvin, stop.isAuto)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Kelvin Step",
                    tint = if (currentStopIndex > 0) Color(0xFF8E8E93) else Color(0xFF2C2C2E),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Right Nudge arrow (›)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(36.dp)
                    .clickable {
                        if (currentStopIndex < HALIDE_WB_VALUES.size - 1) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val stop = HALIDE_WB_VALUES[currentStopIndex + 1]
                            onKelvinChanged(if (stop.isAuto) 5500 else stop.kelvin, stop.isAuto)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Kelvin Step",
                    tint = if (currentStopIndex < HALIDE_WB_VALUES.size - 1) Color(0xFF8E8E93) else Color(0xFF2C2C2E),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun WbPresetButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFFFB800) else Color(0xFF24242A),
        label = "wb_preset_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.Black else Color.White,
        label = "wb_preset_text"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFFFFB800) else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
