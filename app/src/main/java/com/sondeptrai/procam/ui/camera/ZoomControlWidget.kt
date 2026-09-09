package com.sondeptrai.procam.ui.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.procam.ui.theme.ProCamAmber
import kotlin.math.abs

@Composable
fun ZoomControlWidget(
    currentZoom: Float,
    maxZoom: Float,
    isFrontCamera: Boolean,
    onZoomSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isFrontCamera) return // Front camera usually fixed wide lens

    val presets = listOf(1.0f, 2.0f, 3.0f, 5.0f).filter { it <= maxZoom }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x99000000))
            .border(0.5.dp, Color(0x44FFFFFF), RoundedCornerShape(20.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            presets.forEach { preset ->
                val isSelected = abs(currentZoom - preset) < 0.15f
                val isClosestNonPreset = !presets.any { abs(currentZoom - it) < 0.15f } && 
                                        presets.minByOrNull { abs(currentZoom - it) } == preset

                val displayLabel = if (isClosestNonPreset && currentZoom > 1.0f) {
                    String.format("%.1fx", currentZoom)
                } else {
                    "${preset.toInt()}x"
                }

                val bgColor by animateColorAsState(
                    targetValue = if (isSelected || isClosestNonPreset) Color.White else Color.Transparent,
                    label = "zoomBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected || isClosestNonPreset) Color.Black else Color(0xFFC7C7CC),
                    label = "zoomText"
                )

                Box(
                    modifier = Modifier
                        .size(width = if (displayLabel.length > 2) 36.dp else 28.dp, height = 28.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .clickable { onZoomSelected(preset) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayLabel,
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected || isClosestNonPreset) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
