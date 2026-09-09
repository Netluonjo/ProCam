package com.sondeptrai.procam.ui.camera

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.procam.core.camera.CameraUiState
import com.sondeptrai.procam.core.camera.ManualParameter
import com.sondeptrai.procam.ui.theme.ProCamAmber
import com.sondeptrai.procam.ui.theme.ProCamRed
import kotlin.math.roundToInt

// Shutter steps definitions (Pair of Display Text and Exposure Time in Nanoseconds)
private val SHUTTER_STEPS = listOf(
    "AUTO" to -1L,
    "1/8000" to 125_000L,
    "1/4000" to 250_000L,
    "1/2000" to 500_000L,
    "1/1000" to 1_000_000L,
    "1/500" to 2_000_000L,
    "1/250" to 4_000_000L,
    "1/125" to 8_000_000L,
    "1/60" to 16_666_666L,
    "1/30" to 33_333_333L,
    "1/15" to 66_666_666L,
    "1/8" to 125_000_000L,
    "1/4" to 250_000_000L,
    "1/2" to 500_000_000L,
    "1\"" to 1_000_000_000L,
    "2\"" to 2_000_000_000L,
    "4\"" to 4_000_000_000L,
    "8\"" to 8_000_000_000L,
    "15\"" to 15_000_000_000L,
    "30\"" to 30_000_000_000L
)

// ISO steps definitions
private val ISO_STEPS = listOf(
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

// Kelvin White Balance steps
private val KELVIN_STEPS = listOf(
    "AUTO" to -1,
    "2000K" to 2000,
    "2500K" to 2500,
    "3000K" to 3000,
    "3500K" to 3500,
    "4000K" to 4000,
    "4500K" to 4500,
    "5000K" to 5000,
    "5500K" to 5500,
    "6000K" to 6000,
    "6500K" to 6500,
    "7000K" to 7000,
    "7500K" to 7500,
    "8000K" to 8000,
    "8500K" to 8500,
    "9000K" to 9000,
    "9500K" to 9500,
    "10000K" to 10000
)

// Focus steps (0.0 to 1.0)
private val FOCUS_STEPS = listOf(
    "AUTO" to -1f,
    "0.00" to 0.00f,
    "0.05" to 0.05f,
    "0.10" to 0.10f,
    "0.15" to 0.15f,
    "0.20" to 0.20f,
    "0.25" to 0.25f,
    "0.30" to 0.30f,
    "0.35" to 0.35f,
    "0.40" to 0.40f,
    "0.45" to 0.45f,
    "0.50" to 0.50f,
    "0.55" to 0.55f,
    "0.60" to 0.60f,
    "0.65" to 0.65f,
    "0.70" to 0.70f,
    "0.75" to 0.75f,
    "0.80" to 0.80f,
    "0.85" to 0.85f,
    "0.90" to 0.90f,
    "0.95" to 0.95f,
    "1.00" to 1.00f
)

@Composable
fun ProCamRulerDial(
    state: CameraUiState,
    onIsoChanged: (Int, Boolean) -> Unit,
    onShutterChanged: (Long, Boolean) -> Unit,
    onFocusChanged: (Float, Boolean) -> Unit,
    onKelvinChanged: (Int, Boolean) -> Unit,
    onEvChanged: (Int) -> Unit,
    onToggleAeAfLock: () -> Unit,
    onToggleWbLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.activeManualParam == ManualParameter.NONE) return

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val tickSpacingDp = 14.dp
    val tickSpacingPx = with(density) { tickSpacingDp.toPx() }

    // Prepare steps based on active parameter
    val (stepCount, currentIndex, onStepSelected) = when (state.activeManualParam) {
        ManualParameter.AE -> {
            val minEv = state.hardwareInfo.aeCompensationRange.lower
            val maxEv = state.hardwareInfo.aeCompensationRange.upper
            val count = (maxEv - minEv) + 1
            val idx = (state.currentEvIndex - minEv).coerceIn(0, count - 1)
            Triple(count, idx) { newIdx: Int ->
                val ev = minEv + newIdx
                onEvChanged(ev)
            }
        }
        ManualParameter.SHUTTER -> {
            val count = SHUTTER_STEPS.size
            val idx = if (state.isAutoShutter) {
                0
            } else {
                val found = SHUTTER_STEPS.indexOfFirst { it.second == state.currentExposureTimeNanos }
                if (found != -1) found else 8 // default ~1/60s
            }.coerceIn(0, count - 1)
            Triple(count, idx) { newIdx: Int ->
                val (label, nanos) = SHUTTER_STEPS[newIdx]
                if (nanos == -1L) {
                    onShutterChanged(state.currentExposureTimeNanos, true)
                } else {
                    onShutterChanged(nanos, false)
                }
            }
        }
        ManualParameter.ISO -> {
            val count = ISO_STEPS.size
            val idx = if (state.isAutoIso) {
                0
            } else {
                val found = ISO_STEPS.indexOfFirst { it.second == state.currentIso }
                if (found != -1) found else 4 // default ~100
            }.coerceIn(0, count - 1)
            Triple(count, idx) { newIdx: Int ->
                val (label, iso) = ISO_STEPS[newIdx]
                if (iso == -1) {
                    onIsoChanged(state.currentIso, true)
                } else {
                    onIsoChanged(iso, false)
                }
            }
        }
        ManualParameter.FOCUS -> {
            val count = FOCUS_STEPS.size
            val idx = if (state.isAutoFocus) {
                0
            } else {
                val rounded = (state.currentFocusDistance * 20f).roundToInt() + 1
                rounded.coerceIn(1, count - 1)
            }.coerceIn(0, count - 1)
            Triple(count, idx) { newIdx: Int ->
                val (label, dist) = FOCUS_STEPS[newIdx]
                if (dist == -1f) {
                    onFocusChanged(0f, true)
                } else {
                    val actualDiopters = dist * state.hardwareInfo.minFocusDistance
                    onFocusChanged(actualDiopters, false)
                }
            }
        }
        ManualParameter.WB -> {
            val count = KELVIN_STEPS.size
            val idx = if (state.isAutoWb) {
                0
            } else {
                val found = KELVIN_STEPS.indexOfFirst { it.second == state.currentKelvin }
                if (found != -1) found else 8 // default 5500K
            }.coerceIn(0, count - 1)
            Triple(count, idx) { newIdx: Int ->
                val (label, kelvin) = KELVIN_STEPS[newIdx]
                if (kelvin == -1) {
                    onKelvinChanged(state.currentKelvin, true)
                } else {
                    onKelvinChanged(kelvin, false)
                }
            }
        }
        ManualParameter.LOCK -> {
            // E/F lock doesn't use the ruler, shows quick lock switches
            Triple(0, 0) { _: Int -> }
        }
        ManualParameter.NONE -> Triple(0, 0) { _: Int -> }
    }

    if (state.activeManualParam == ManualParameter.LOCK) {
        // Special Lock Panel for E/F
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color.Black)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (state.isAeAfLocked) ProCamAmber else Color(0xFF1E1E24))
                    .clickable { onToggleAeAfLock() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (state.isAeAfLocked) "AE/AF LOCKED" else "LOCK AE/AF",
                    color = if (state.isAeAfLocked) Color.Black else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (state.isWbLocked) ProCamAmber else Color(0xFF1E1E24))
                    .clickable { onToggleWbLock() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (state.isWbLocked) "WB LOCKED" else "LOCK WB",
                    color = if (state.isWbLocked) Color.Black else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        return
    }

    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color.Black)
            .pointerInput(stepCount, currentIndex) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount
                        val stepsToMove = (-dragAccumulator / tickSpacingPx).toInt()
                        if (stepsToMove != 0) {
                            val newIdx = (currentIndex + stepsToMove).coerceIn(0, stepCount - 1)
                            if (newIdx != currentIndex) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onStepSelected(newIdx)
                                dragAccumulator += stepsToMove * tickSpacingPx
                            }
                        }
                    }
                )
            }
    ) {
        // 1. Mechanical graduated tick marks (Canvas)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f

            val visibleCount = (width / (2f * tickSpacingPx)).toInt() + 2

            for (offset in -visibleCount..visibleCount) {
                val tickIdx = currentIndex + offset
                if (tickIdx in 0 until stepCount) {
                    val tickX = centerX + (offset * tickSpacingPx) + dragAccumulator

                    // Decide tick height: major every 5 steps or center
                    val isMajor = tickIdx % 5 == 0 || tickIdx == 0
                    val tickHeight = if (isMajor) 20.dp.toPx() else 10.dp.toPx()
                    val tickWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
                    val tickColor = if (isMajor) Color(0xFFE5E5EA) else Color(0xFF545458)

                    drawLine(
                        color = tickColor,
                        start = Offset(tickX, centerY - tickHeight / 2f),
                        end = Offset(tickX, centerY + tickHeight / 2f),
                        strokeWidth = tickWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // 2. Fixed Center Red Needle (Iconic ProCam Red Marker)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(2.5.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(ProCamRed)
        )

        // 3. Left Nudge Button (‹)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(36.dp)
                .clickable {
                    if (currentIndex > 0) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onStepSelected(currentIndex - 1)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous",
                tint = if (currentIndex > 0) Color(0xFF8E8E93) else Color(0xFF2C2C2E),
                modifier = Modifier.size(20.dp)
            )
        }

        // 4. Right Nudge Button (›)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(36.dp)
                .clickable {
                    if (currentIndex < stepCount - 1) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onStepSelected(currentIndex + 1)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next",
                tint = if (currentIndex < stepCount - 1) Color(0xFF8E8E93) else Color(0xFF2C2C2E),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
