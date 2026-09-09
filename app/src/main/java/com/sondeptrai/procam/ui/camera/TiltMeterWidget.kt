package com.sondeptrai.procam.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import com.sondeptrai.procam.core.sensor.LevelOrientation
import com.sondeptrai.procam.ui.theme.PeakingGreen

@Composable
fun TiltMeterWidget(
    levelOrientation: LevelOrientation,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f

            val strokeColor = if (levelOrientation.isLevel) PeakingGreen else Color(0x99FFFFFF)
            val strokeWidth = if (levelOrientation.isLevel) 3.5f else 2.0f

            // Rotate based on phone's roll angle
            rotate(degrees = -levelOrientation.rollAngle, pivot = Offset(centerX, centerY)) {
                val wingLength = 60f
                val gap = 30f

                // Left wing
                drawLine(
                    color = strokeColor,
                    start = Offset(centerX - gap - wingLength, centerY),
                    end = Offset(centerX - gap, centerY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                // Right wing
                drawLine(
                    color = strokeColor,
                    start = Offset(centerX + gap, centerY),
                    end = Offset(centerX + gap + wingLength, centerY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                // Center cross/dot
                drawCircle(
                    color = strokeColor,
                    radius = if (levelOrientation.isLevel) 4f else 3f,
                    center = Offset(centerX, centerY)
                )
            }
        }
    }
}
