package com.sondeptrai.procam.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.window.Dialog
import com.sondeptrai.procam.core.camera.AspectRatio
import com.sondeptrai.procam.core.camera.CameraUiState
import com.sondeptrai.procam.core.camera.GridType
import com.sondeptrai.procam.core.camera.PeakingColor
import com.sondeptrai.procam.ui.theme.PeakingCyan
import com.sondeptrai.procam.ui.theme.PeakingGreen
import com.sondeptrai.procam.ui.theme.PeakingRed
import com.sondeptrai.procam.ui.theme.ProCamAmber

@Composable
fun ProCamSettingsModal(
    state: CameraUiState,
    onDismiss: () -> Unit,
    onPeakingToggle: () -> Unit,
    onZebraToggle: () -> Unit,
    onGridToggle: () -> Unit,
    onTiltMeterToggle: () -> Unit,
    onHistogramToggle: () -> Unit,
    onAspectRatioToggle: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF141418))
                .border(1.dp, Color(0xFF2E2E36), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CAMERA SETTINGS",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onDismiss() }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color(0xFF2C2C2E))
                )

                // 1. Focus Peaking
                SettingToggleRow(
                    title = "Focus Peaking",
                    subtitle = "Highlight in-focus edges with colored contours",
                    isChecked = state.isFocusPeakingEnabled,
                    onToggle = onPeakingToggle
                )

                // 2. Zebra Stripes
                SettingToggleRow(
                    title = "Zebra Highlight Warning",
                    subtitle = "Diagonal stripes overlay on overexposed areas",
                    isChecked = state.isZebraEnabled,
                    onToggle = onZebraToggle
                )

                // 3. Grid Overlay
                SettingActionRow(
                    title = "Viewfinder Grid",
                    value = state.gridType.title,
                    onClick = onGridToggle
                )

                // 4. Tiltmeter / Horizon
                SettingToggleRow(
                    title = "Tiltmeter / Horizon Level",
                    subtitle = "Sensor-guided artificial horizon indicator",
                    isChecked = state.isTiltMeterEnabled,
                    onToggle = onTiltMeterToggle
                )

                // 5. Live Histogram
                SettingToggleRow(
                    title = "Live Luminance Histogram",
                    subtitle = "Real-time exposure distribution graph",
                    isChecked = state.isHistogramEnabled,
                    onToggle = onHistogramToggle
                )

                // 6. Aspect Ratio
                SettingActionRow(
                    title = "Aspect Ratio",
                    value = state.aspectRatio.label,
                    onClick = onAspectRatioToggle
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                color = Color(0xFF8E8E93),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ProCamAmber,
                uncheckedThumbColor = Color(0xFF8E8E93),
                uncheckedTrackColor = Color(0xFF2C2C2E)
            )
        )
    }
}

@Composable
private fun SettingActionRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF202026))
                .border(1.dp, Color(0xFF38383A), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = value,
                color = ProCamAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
