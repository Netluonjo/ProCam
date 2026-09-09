package com.sondeptrai.procam.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.procam.core.camera.CameraUiState
import com.sondeptrai.procam.core.camera.ManualParameter
import com.sondeptrai.procam.ui.theme.*

@Composable
fun ManualControlDial(
    state: CameraUiState,
    onIsoChanged: (Int, Boolean) -> Unit,
    onShutterChanged: (Long, Boolean) -> Unit,
    onFocusChanged: (Float, Boolean) -> Unit,
    onKelvinChanged: (Int, Boolean) -> Unit,
    onEvChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.activeManualParam == ManualParameter.NONE) return

    val info = state.hardwareInfo

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DslrDarkGrey.copy(alpha = 0.92f))
            .border(1.dp, DslrBorder)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Value Header & Auto Switch Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Label & Current Value display
            val (paramLabel, currentValueText, isAuto) = when (state.activeManualParam) {
                ManualParameter.ISO -> Triple(
                    "ISO",
                    if (state.isAutoIso) "AUTO (${state.currentIso})" else "${state.currentIso}",
                    state.isAutoIso
                )
                ManualParameter.SHUTTER -> Triple(
                    "SHUTTER",
                    if (state.isAutoShutter) "AUTO (${info.formatShutterSpeed(state.currentExposureTimeNanos)})"
                    else info.formatShutterSpeed(state.currentExposureTimeNanos),
                    state.isAutoShutter
                )
                ManualParameter.FOCUS -> Triple(
                    "FOCUS",
                    if (state.isAutoFocus) "AUTO AF" else String.format("%.2f m", if (state.currentFocusDistance > 0) 1f / state.currentFocusDistance else 99f),
                    state.isAutoFocus
                )
                ManualParameter.WB -> Triple(
                    "WHITE BALANCE",
                    if (state.isAutoWb) "AUTO AWB" else "${state.currentKelvin} K",
                    state.isAutoWb
                )
                ManualParameter.EV -> Triple(
                    "EXPOSURE COMP",
                    info.formatEv(state.currentEvIndex),
                    false
                )
                ManualParameter.NONE -> Triple("", "", false)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$paramLabel: ",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = currentValueText,
                    color = ProCamAmber,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Dedicated AUTO Toggle Pill
            if (state.activeManualParam != ManualParameter.EV) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isAuto) ProCamAmber else DslrSurface)
                        .border(1.dp, if (isAuto) ProCamAmber else DslrBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            when (state.activeManualParam) {
                                ManualParameter.ISO -> onIsoChanged(state.currentIso, !isAuto)
                                ManualParameter.SHUTTER -> onShutterChanged(state.currentExposureTimeNanos, !isAuto)
                                ManualParameter.FOCUS -> onFocusChanged(state.currentFocusDistance, !isAuto)
                                ManualParameter.WB -> onKelvinChanged(state.currentKelvin, !isAuto)
                                else -> {}
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "AUTO",
                        color = if (isAuto) DslrBlack else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Precision Mechanical Slider Bar
        when (state.activeManualParam) {
            ManualParameter.ISO -> {
                val minIso = info.isoRange.lower.toFloat()
                val maxIso = info.isoRange.upper.toFloat()
                Slider(
                    value = state.currentIso.toFloat().coerceIn(minIso, maxIso),
                    onValueChange = { onIsoChanged(it.toInt(), false) },
                    valueRange = minIso..maxIso,
                    colors = SliderDefaults.colors(
                        thumbColor = ProCamAmber,
                        activeTrackColor = ProCamAmber,
                        inactiveTrackColor = DslrSurface
                    )
                )
            }
            ManualParameter.SHUTTER -> {
                val minExp = info.exposureTimeRange.lower.toFloat()
                val maxExp = 1_000_000_000f // 1 second for standard slider range
                Slider(
                    value = state.currentExposureTimeNanos.toFloat().coerceIn(minExp, maxExp),
                    onValueChange = { onShutterChanged(it.toLong(), false) },
                    valueRange = minExp..maxExp,
                    colors = SliderDefaults.colors(
                        thumbColor = ProCamAmber,
                        activeTrackColor = ProCamAmber,
                        inactiveTrackColor = DslrSurface
                    )
                )
            }
            ManualParameter.FOCUS -> {
                val maxDist = info.minFocusDistance // Diopters
                Slider(
                    value = state.currentFocusDistance.coerceIn(0f, maxDist),
                    onValueChange = { onFocusChanged(it, false) },
                    valueRange = 0f..maxDist,
                    colors = SliderDefaults.colors(
                        thumbColor = PeakingGreen,
                        activeTrackColor = PeakingGreen,
                        inactiveTrackColor = DslrSurface
                    )
                )
            }
            ManualParameter.WB -> {
                Slider(
                    value = state.currentKelvin.toFloat().coerceIn(2000f, 10000f),
                    onValueChange = { onKelvinChanged(it.toInt(), false) },
                    valueRange = 2000f..10000f,
                    colors = SliderDefaults.colors(
                        thumbColor = ProCamAmber,
                        activeTrackColor = ProCamAmber,
                        inactiveTrackColor = DslrSurface
                    )
                )
            }
            ManualParameter.EV -> {
                val minEv = info.aeCompensationRange.lower.toFloat()
                val maxEv = info.aeCompensationRange.upper.toFloat()
                Slider(
                    value = state.currentEvIndex.toFloat().coerceIn(minEv, maxEv),
                    onValueChange = { onEvChanged(it.toInt()) },
                    valueRange = minEv..maxEv,
                    steps = (maxEv - minEv).toInt() - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = ProCamAmber,
                        activeTrackColor = ProCamAmber,
                        inactiveTrackColor = DslrSurface
                    )
                )
            }
            ManualParameter.NONE -> {}
        }
    }
}
