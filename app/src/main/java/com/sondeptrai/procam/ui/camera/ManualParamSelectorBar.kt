package com.sondeptrai.procam.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun ManualParamSelectorBar(
    state: CameraUiState,
    onParamSelected: (ManualParameter) -> Unit,
    modifier: Modifier = Modifier
) {
    val info = state.hardwareInfo

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val params = listOf(
            ManualParameter.ISO to if (state.isAutoIso) "AUTO" else "${state.currentIso}",
            ManualParameter.SHUTTER to if (state.isAutoShutter) "AUTO" else info.formatShutterSpeed(state.currentExposureTimeNanos),
            ManualParameter.FOCUS to if (state.isAutoFocus) "AF" else "MF",
            ManualParameter.WB to if (state.isAutoWb) "AWB" else "${state.currentKelvin / 1000}K",
            ManualParameter.EV to info.formatEv(state.currentEvIndex)
        )

        params.forEach { (param, valueText) ->
            val isSelected = state.activeManualParam == param

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) ProCamAmber.copy(alpha = 0.2f) else DslrSurface)
                    .border(
                        1.dp,
                        if (isSelected) ProCamAmber else DslrBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        onParamSelected(if (isSelected) ManualParameter.NONE else param)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = param.label,
                    color = if (isSelected) ProCamAmber else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = valueText,
                    color = if (isSelected) TextPrimary else TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
