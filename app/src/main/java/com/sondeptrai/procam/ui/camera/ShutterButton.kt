package com.sondeptrai.procam.ui.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.procam.core.camera.ShootingMode
import com.sondeptrai.procam.ui.theme.ProCamAmber
import com.sondeptrai.procam.ui.theme.ProCamRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ShutterControlSection(
    mode: ShootingMode,
    isRecording: Boolean,
    isFrontCamera: Boolean,
    timerSeconds: Int,
    isDrawerExpanded: Boolean,
    lastCapturedUri: Uri?,
    onShutterClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onFlipCameraClick: () -> Unit,
    onModeDrawerToggle: () -> Unit,
    onTimerCycle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.92f else 1.0f, label = "shutterScale")
    val chevronRotation by animateFloatAsState(targetValue = if (isDrawerExpanded) 180f else 0f, label = "chevronRotate")

    // Load actual thumbnail bitmap from last captured photo
    var thumbnailBitmap by remember(lastCapturedUri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(lastCapturedUri) {
        if (lastCapturedUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(lastCapturedUri)?.use { stream ->
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 4 // downsample for mini thumbnail preview
                        }
                        thumbnailBitmap = BitmapFactory.decodeStream(stream, null, options)
                    }
                } catch (e: Exception) {
                    thumbnailBitmap = null
                }
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(Color.Black)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Gallery Thumbnail (Rounded square previewing last capture)
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1C1C1E))
                .border(1.dp, Color(0xFF38383A), RoundedCornerShape(10.dp))
                .clickable { onGalleryClick() },
            contentAlignment = Alignment.Center
        ) {
            val bmp = thumbnailBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Last captured photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Gallery",
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 2. Mode Expand Drawer Chevron Button (^)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { onModeDrawerToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Toggle Modes Drawer",
                tint = if (isDrawerExpanded) ProCamAmber else Color(0xFFC7C7CC),
                modifier = Modifier
                    .size(24.dp)
                    .rotate(chevronRotation)
            )
        }

        // 3. ProCam Iconic DSLR Shutter Button
        Box(
            modifier = Modifier
                .size(74.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color.Transparent)
                .border(3.5.dp, Color.White, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onShutterClick() }
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            val isVideo = mode == ShootingMode.VIDEO
            val innerColor = if (isVideo) ProCamRed else Color.White
            val innerShape = if (isVideo && isRecording) RoundedCornerShape(8.dp) else CircleShape
            val innerSizeModifier = if (isVideo && isRecording) Modifier.size(26.dp) else Modifier.fillMaxSize()

            Box(
                modifier = innerSizeModifier
                    .clip(innerShape)
                    .background(innerColor)
            )
        }

        // 4. Timer / Anti-Shake Button (⏲)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { onTimerCycle() },
            contentAlignment = Alignment.Center
        ) {
            if (timerSeconds > 0) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(ProCamAmber),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${timerSeconds}s",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Timer",
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 5. Lens / Camera Switcher Badge Button [1x / Front]
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFF1C1C1E))
                .border(1.dp, Color(0xFF38383A), CircleShape)
                .clickable { onFlipCameraClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isFrontCamera) "Front" else "1x",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
