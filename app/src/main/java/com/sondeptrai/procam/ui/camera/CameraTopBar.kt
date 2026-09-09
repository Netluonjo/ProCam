package com.sondeptrai.procam.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
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
import com.sondeptrai.procam.core.camera.AspectRatio
import com.sondeptrai.procam.core.camera.CameraUiState
import com.sondeptrai.procam.core.camera.GridType
import com.sondeptrai.procam.ui.theme.*

@Composable
fun CameraTopBar(
    state: CameraUiState,
    onFlashToggle: () -> Unit,
    onRawToggle: () -> Unit,
    onAspectRatioToggle: () -> Unit,
    onPeakingToggle: () -> Unit,
    onZebraToggle: () -> Unit,
    onGridToggle: () -> Unit,
    onTiltMeterToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xCC0A0A0C))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flash Mode
        Icon(
            imageVector = when (state.flashMode) {
                1 -> Icons.Default.FlashAuto
                2 -> Icons.Default.FlashOn
                3 -> Icons.Default.Highlight
                else -> Icons.Default.FlashOff
            },
            contentDescription = "Flash",
            tint = if (state.flashMode > 0) ProCamAmber else TextSecondary,
            modifier = Modifier
                .size(22.dp)
                .clickable { onFlashToggle() }
        )

        // RAW / JPG Toggle Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (state.isRawEnabled) ProCamAmber else DslrSurface)
                .border(1.dp, if (state.isRawEnabled) ProCamAmber else DslrBorder, RoundedCornerShape(6.dp))
                .clickable { onRawToggle() }
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = if (state.isRawEnabled) "RAW" else "JPG",
                color = if (state.isRawEnabled) DslrBlack else TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Aspect Ratio Toggle
        Text(
            text = state.aspectRatio.label,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onAspectRatioToggle() }
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )

        // Focus Peaking Button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (state.isFocusPeakingEnabled) PeakingGreen.copy(alpha = 0.2f) else Color.Transparent)
                .border(
                    1.dp,
                    if (state.isFocusPeakingEnabled) PeakingGreen else DslrBorder,
                    RoundedCornerShape(6.dp)
                )
                .clickable { onPeakingToggle() }
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (state.isFocusPeakingEnabled) PeakingGreen else TextMuted)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "PEAK",
                color = if (state.isFocusPeakingEnabled) PeakingGreen else TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Zebra Stripes Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (state.isZebraEnabled) ProCamAmber.copy(alpha = 0.2f) else Color.Transparent)
                .border(
                    1.dp,
                    if (state.isZebraEnabled) ProCamAmber else DslrBorder,
                    RoundedCornerShape(6.dp)
                )
                .clickable { onZebraToggle() }
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = "ZEBRA",
                color = if (state.isZebraEnabled) ProCamAmber else TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Grid Selector
        Icon(
            imageVector = Icons.Default.GridOn,
            contentDescription = "Grid",
            tint = if (state.gridType != GridType.NONE) ProCamAmber else TextSecondary,
            modifier = Modifier
                .size(20.dp)
                .clickable { onGridToggle() }
        )
    }
}
