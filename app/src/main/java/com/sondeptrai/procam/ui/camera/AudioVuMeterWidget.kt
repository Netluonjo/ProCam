package com.sondeptrai.procam.ui.camera

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.procam.ui.theme.ProCamGreen
import com.sondeptrai.procam.ui.theme.ProCamRed
import kotlin.random.Random

@Composable
fun AudioVuMeterWidget(
    decibelLevel: Float,
    modifier: Modifier = Modifier
) {
    val totalSegments = 16
    val greenSegments = 10
    val yellowSegments = 4
    val redSegments = 2

    // Normalize level between 0f and 1f (simulated stereo L and R)
    val activeCountL = (decibelLevel.coerceIn(0.1f, 1.0f) * totalSegments).toInt()
    val activeCountR = ((decibelLevel * 0.9f + 0.05f).coerceIn(0.1f, 1.0f) * totalSegments).toInt()

    Box(
        modifier = modifier
            .width(36.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xB3000000))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Mic Icon
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Audio input",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )

            // Segmented Stereo Bars (L and R)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Channel L
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom)
                ) {
                    for (i in totalSegments - 1 downTo 0) {
                        val isLit = i < activeCountL
                        val segmentColor = when {
                            i >= greenSegments + yellowSegments -> ProCamRed
                            i >= greenSegments -> Color(0xFFFFCC00)
                            else -> ProCamGreen
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (isLit) segmentColor else Color(0x33545458))
                        )
                    }
                }

                // Channel R
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom)
                ) {
                    for (i in totalSegments - 1 downTo 0) {
                        val isLit = i < activeCountR
                        val segmentColor = when {
                            i >= greenSegments + yellowSegments -> ProCamRed
                            i >= greenSegments -> Color(0xFFFFCC00)
                            else -> ProCamGreen
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (isLit) segmentColor else Color(0x33545458))
                        )
                    }
                }
            }

            // Channel Labels: L and R
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "L",
                    color = Color(0xFF8E8E93),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "R",
                    color = Color(0xFF8E8E93),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
