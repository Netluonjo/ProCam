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

data class FocusStop(val label: String, val diopters: Float, val isAuto: Boolean = false)

val HALIDE_FOCUS_VALUES = listOf(
    FocusStop("AUTO", -1f, isAuto = true),
    FocusStop("MACRO", 10.0f),
    FocusStop("0.15m", 6.67f),
    FocusStop("0.20m", 5.00f),
    FocusStop("0.25m", 4.00f),
    FocusStop("0.30m", 3.33f),
    FocusStop("0.40m", 2.50f),
    FocusStop("0.50m", 2.00f),
    FocusStop("0.70m", 1.43f),
    FocusStop("1.0m", 1.00f),
    FocusStop("1.5m", 0.67f),
    FocusStop("2.0m", 0.50f),
    FocusStop("3.0m", 0.33f),
    FocusStop("5.0m", 0.20f),
    FocusStop("∞", 0.00f)
)

@Composable
fun HalideFocusDial(
    state: CameraUiState,
    onFocusChanged: (Float, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemWidthDp = 58.dp
    val itemWidthPx = with(density) { itemWidthDp.toPx() }

    val currentStopIndex = if (state.isAutoFocus) {
        0
    } else {
        val found = HALIDE_FOCUS_VALUES.drop(1).minByOrNull { abs(it.diopters - state.currentFocusDistance) }
        val idx = HALIDE_FOCUS_VALUES.indexOf(found)
        if (idx != -1) idx else 7 // default ~0.5m
    }

    var dragOffset by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0F12))
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Header with Label, Value readout & Quick presets ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Label
            Text(
                text = "LENS FOCUS",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Value Readout
            val focusReadout = if (state.isAutoFocus) {
                "AUTO AF"
            } else if (state.currentFocusDistance <= 0.1f) {
                "MF ∞"
            } else if (state.currentFocusDistance >= 8.0f) {
                "MF MACRO"
            } else {
                val meters = 1.0f / state.currentFocusDistance
                String.format("MF %.2fm", meters)
            }

            Text(
                text = focusReadout,
                color = Color(0xFFFFB800),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.weight(1f))

            // Quick Preset Pills: [ AUTO ] [ MACRO ] [ 0.5m ] [ 1.0m ] [ ∞ ]
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // AUTO Button
                FocusPresetButton(
                    label = "AUTO",
                    isSelected = state.isAutoFocus,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFocusChanged(0f, true)
                    }
                )

                // MACRO Button
                val isMacro = !state.isAutoFocus && state.currentFocusDistance >= 8.0f
                FocusPresetButton(
                    label = "MACRO",
                    isSelected = isMacro,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFocusChanged(10.0f, false)
                    }
                )

                // 0.5m Button
                val isHalfM = !state.isAutoFocus && abs(state.currentFocusDistance - 2.0f) < 0.3f
                FocusPresetButton(
                    label = "0.5m",
                    isSelected = isHalfM,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFocusChanged(2.0f, false)
                    }
                )

                // 1.0m Button
                val isOneM = !state.isAutoFocus && abs(state.currentFocusDistance - 1.0f) < 0.2f
                FocusPresetButton(
                    label = "1.0m",
                    isSelected = isOneM,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFocusChanged(1.0f, false)
                    }
                )

                // Infinity Button
                val isInf = !state.isAutoFocus && state.currentFocusDistance <= 0.1f
                FocusPresetButton(
                    label = "∞",
                    isSelected = isInf,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFocusChanged(0.0f, false)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- 2. 3D Rotary Drum Wheel for Focus ---
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
                                val nextIdx = (currentStopIndex + stepsToMove).coerceIn(0, HALIDE_FOCUS_VALUES.size - 1)
                                if (nextIdx != currentStopIndex) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val stop = HALIDE_FOCUS_VALUES[nextIdx]
                                    onFocusChanged(stop.diopters, stop.isAuto)
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
                val h = size.height
                val cx = w / 2f

                val visibleCount = (w / (2f * itemWidthPx)).toInt() + 2

                for (offset in -visibleCount..visibleCount) {
                    val idx = currentStopIndex + offset
                    if (idx in HALIDE_FOCUS_VALUES.indices) {
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

            // Number/Text Labels in 3D perspective
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 28.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (offset in -3..3) {
                    val idx = currentStopIndex + offset
                    if (idx in HALIDE_FOCUS_VALUES.indices) {
                        val stop = HALIDE_FOCUS_VALUES[idx]
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

                        Box(
                            modifier = Modifier
                                .width(itemWidthDp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onFocusChanged(stop.diopters, stop.isAuto)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stop.label,
                                color = if (isCenter) Color(0xFFFFB800) else Color.White.copy(alpha = alpha),
                                fontSize = (12 * scale).sp,
                                fontWeight = if (isCenter) FontWeight.Black else FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
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
                            val stop = HALIDE_FOCUS_VALUES[currentStopIndex - 1]
                            onFocusChanged(stop.diopters, stop.isAuto)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Focus Step",
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
                        if (currentStopIndex < HALIDE_FOCUS_VALUES.size - 1) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val stop = HALIDE_FOCUS_VALUES[currentStopIndex + 1]
                            onFocusChanged(stop.diopters, stop.isAuto)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Focus Step",
                    tint = if (currentStopIndex < HALIDE_FOCUS_VALUES.size - 1) Color(0xFF8E8E93) else Color(0xFF2C2C2E),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FocusPresetButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFFFB800) else Color(0xFF24242A),
        label = "focus_preset_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.Black else Color.White,
        label = "focus_preset_text"
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
