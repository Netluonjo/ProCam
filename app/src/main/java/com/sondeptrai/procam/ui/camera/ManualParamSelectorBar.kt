package com.sondeptrai.procam.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.procam.core.camera.CameraUiState
import com.sondeptrai.procam.core.camera.ManualParameter
import com.sondeptrai.procam.ui.theme.ProCamAmber
import com.sondeptrai.procam.ui.theme.ProCamAmberDim
import com.sondeptrai.procam.ui.theme.TextMuted
import com.sondeptrai.procam.ui.theme.TextSecondary

@Composable
fun ManualParamSelectorBar(
    state: CameraUiState,
    onParamSelected: (ManualParameter) -> Unit,
    modifier: Modifier = Modifier
) {
    val info = state.hardwareInfo

    val aeText = info.formatEv(state.currentEvIndex)
    val secText = if (state.isAutoShutter) "AUTO" else info.formatShutterSpeed(state.currentExposureTimeNanos)
    val isoText = if (state.isAutoIso) "AUTO" else "${state.currentIso}"
    val afText = if (state.isAutoFocus) "AUTO" else {
        val distMeters = if (state.currentFocusDistance > 0f) 1f / state.currentFocusDistance else 99f
        String.format("%.2f", distMeters)
    }
    val awbText = if (state.isAutoWb) "AUTO" else "${state.currentKelvin}K"
    val lockText = when {
        state.isAeAfLocked && state.isWbLocked -> "ALL-L"
        state.isAeAfLocked -> "AE-L"
        state.isWbLocked -> "WB-L"
        else -> "OFF"
    }

    val items = listOf(
        ParamItemData(ManualParameter.AE, "AE", aeText),
        ParamItemData(ManualParameter.SHUTTER, "SEC", secText),
        ParamItemData(ManualParameter.ISO, "ISO", isoText),
        ParamItemData(ManualParameter.FOCUS, "AF", afText),
        ParamItemData(ManualParameter.WB, "AWB", awbText),
        ParamItemData(ManualParameter.LOCK, "E/F", lockText)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.Black),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = state.activeManualParam == item.param

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable {
                        onParamSelected(if (isSelected) ManualParameter.NONE else item.param)
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item.label,
                        color = if (isSelected) ProCamAmber else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.value,
                        color = if (isSelected) Color.White else Color(0xFFC7C7CC),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                // Active bottom highlight indicator
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(0.8f)
                            .height(2.dp)
                            .background(ProCamAmber)
                    )
                }
            }

            // Subtle vertical divider between columns
            if (index < items.size - 1) {
                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .height(24.dp)
                        .background(Color(0xFF2C2C2E))
                )
            }
        }
    }
}

private data class ParamItemData(
    val param: ManualParameter,
    val label: String,
    val value: String
)
