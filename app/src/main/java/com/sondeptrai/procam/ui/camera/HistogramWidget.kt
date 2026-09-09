package com.sondeptrai.procam.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.sondeptrai.procam.core.gl.HistogramData
import com.sondeptrai.procam.ui.theme.DslrBorder
import com.sondeptrai.procam.ui.theme.ProCamAmber

@Composable
fun HistogramWidget(
    histogramData: HistogramData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(110.dp)
            .height(55.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x99141418))
            .border(1.dp, DslrBorder, RoundedCornerShape(6.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val bins = histogramData.lumaBins
            val stepX = width / 256f

            val path = Path()
            path.moveTo(0f, height)

            for (i in 0 until 256) {
                val x = i * stepX
                val normalizedY = (1f - bins[i].coerceIn(0f, 1f)) * height
                path.lineTo(x, normalizedY)
            }
            path.lineTo(width, height)
            path.close()

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ProCamAmber.copy(alpha = 0.75f),
                        ProCamAmber.copy(alpha = 0.15f)
                    )
                )
            )
        }
    }
}
