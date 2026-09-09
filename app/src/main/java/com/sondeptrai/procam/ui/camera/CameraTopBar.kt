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
import com.sondeptrai.procam.core.camera.CameraUiState
import com.sondeptrai.procam.ui.theme.ProCamAmber
import com.sondeptrai.procam.ui.theme.TextMuted
import com.sondeptrai.procam.ui.theme.TextSecondary

@Composable
fun CameraTopBar(
    state: CameraUiState,
    onFlashToggle: () -> Unit,
    onRawToggle: () -> Unit,
    onTiffToggle: () -> Unit,
    onSbrtToggle: () -> Unit,
    onAebToggle: () -> Unit,
    onSetToggle: () -> Unit,
    onFlipCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        // --- 1. Primary Top Status Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash Mode Button [⚡ Auto / On / Off]
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onFlashToggle() }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val flashIcon = when (state.flashMode) {
                    1 -> Icons.Default.FlashAuto
                    2 -> Icons.Default.FlashOn
                    3 -> Icons.Default.Highlight
                    else -> Icons.Default.FlashOff
                }
                val flashLabel = when (state.flashMode) {
                    1 -> "Auto"
                    2 -> "On"
                    3 -> "Torch"
                    else -> "Off"
                }
                val isFlashActive = state.flashMode > 0

                Icon(
                    imageVector = flashIcon,
                    contentDescription = "Flash",
                    tint = if (isFlashActive) ProCamAmber else Color(0xFF8E8E93),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = flashLabel,
                    color = if (isFlashActive) ProCamAmber else Color(0xFF8E8E93),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Center Format & Feature Badges: [RAW]  [TIFF]  [SBRT]  [AEB]
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // RAW
                TopBarPillButton(
                    label = "RAW",
                    isActive = state.isRawEnabled,
                    onClick = onRawToggle
                )

                // TIFF
                TopBarPillButton(
                    label = "TIFF",
                    isActive = state.isTiffEnabled,
                    onClick = onTiffToggle
                )

                // SBRT (Smart Burst)
                TopBarPillButton(
                    label = "SBRT",
                    isActive = state.isSbrtEnabled,
                    onClick = onSbrtToggle
                )

                // AEB (Auto Exposure Bracketing)
                TopBarPillButton(
                    label = "AEB",
                    isActive = state.isAebEnabled,
                    onClick = onAebToggle
                )
            }

            // Right SET Button (Iconic ProCam Settings Pill)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, if (state.isSetMenuOpen) ProCamAmber else Color(0xFF48484A), RoundedCornerShape(12.dp))
                    .clickable { onSetToggle() }
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SET",
                    color = if (state.isSetMenuOpen) ProCamAmber else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // --- 2. Sub-header Info Strip (Below Primary Top Bar) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(Color(0xE60A0A0C))
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Camera Switch Icon
            Icon(
                imageVector = Icons.Default.FlipCameraAndroid,
                contentDescription = "Switch Camera",
                tint = Color(0xFFC7C7CC),
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onFlipCamera() }
            )

            // Center Info: 12MP | Auto Exposure | Auto WB
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.sensorMegapixels,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = if (state.isAutoIso && state.isAutoShutter) "Auto" else "Manual",
                    color = if (state.isAutoIso && state.isAutoShutter) Color(0xFF34C759) else ProCamAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = if (state.isAutoWb) "Auto" else "WB-M",
                    color = if (state.isAutoWb) Color(0xFF34C759) else ProCamAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Battery and Free Storage Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = "Battery",
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(14.dp)
                )

                Text(
                    text = "${state.freeStorageGb}GB",
                    color = Color(0xFF8E8E93),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun TopBarPillButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) ProCamAmber.copy(alpha = 0.25f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isActive) ProCamAmber else Color(0xFF8E8E93),
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}
