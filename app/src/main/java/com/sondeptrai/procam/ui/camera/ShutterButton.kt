package com.sondeptrai.procam.ui.camera

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sondeptrai.procam.core.camera.ShootingMode
import com.sondeptrai.procam.ui.theme.*

@Composable
fun ShutterControlSection(
    mode: ShootingMode,
    isRecording: Boolean,
    onShutterClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onFlipCameraClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.92f else 1.0f, label = "shutterScale")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gallery Thumbnail Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DslrSurface)
                .border(1.dp, DslrBorder, RoundedCornerShape(12.dp))
                .clickable { onGalleryClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = "Gallery",
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        // Mechanical DSLR Shutter Button
        Box(
            modifier = Modifier
                .size(76.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color.Transparent)
                .border(4.dp, Color(0xFFE0E0E0), CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onShutterClick() }
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            val innerColor = when (mode) {
                ShootingMode.VIDEO -> if (isRecording) ProCamRed else ProCamRed.copy(alpha = 0.8f)
                ShootingMode.SLOW_SHUTTER, ShootingMode.ANTI_SHAKE -> ProCamAmber
                else -> Color.White
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(if (mode == ShootingMode.VIDEO && isRecording) RoundedCornerShape(8.dp) else CircleShape)
                    .background(innerColor)
            )
        }

        // Camera Flip Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(DslrSurface)
                .border(1.dp, DslrBorder, CircleShape)
                .clickable { onFlipCameraClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FlipCameraAndroid,
                contentDescription = "Flip Camera",
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
