package com.sondeptrai.procam.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.sondeptrai.procam.core.camera.GridType
import com.sondeptrai.procam.ui.theme.ProCamAmber

@Composable
fun ViewfinderOverlay(
    gridType: GridType,
    onFocusPointSelected: (Offset) -> Unit,
    focusReticlePoint: Offset? = null,
    modifier: Modifier = Modifier
) {
    var localTapPosition by remember { mutableStateOf<Offset?>(null) }
    val activePoint = focusReticlePoint ?: localTapPosition

    LaunchedEffect(localTapPosition) {
        if (localTapPosition != null) {
            kotlinx.coroutines.delay(2500)
            localTapPosition = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    localTapPosition = offset
                    onFocusPointSelected(offset)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val gridColor = Color(0x40FFFFFF)
            val strokeWidth = 1.2f

            when (gridType) {
                GridType.RULE_OF_THIRDS -> {
                    val col1 = width / 3f
                    val col2 = (width / 3f) * 2f
                    val row1 = height / 3f
                    val row2 = (height / 3f) * 2f

                    // Vertical lines
                    drawLine(gridColor, Offset(col1, 0f), Offset(col1, height), strokeWidth)
                    drawLine(gridColor, Offset(col2, 0f), Offset(col2, height), strokeWidth)

                    // Horizontal lines
                    drawLine(gridColor, Offset(0f, row1), Offset(width, row1), strokeWidth)
                    drawLine(gridColor, Offset(0f, row2), Offset(width, row2), strokeWidth)
                }

                GridType.GOLDEN_RATIO -> {
                    val phi = 0.618f
                    val col1 = width * (1f - phi)
                    val col2 = width * phi
                    val row1 = height * (1f - phi)
                    val row2 = height * phi

                    drawLine(gridColor, Offset(col1, 0f), Offset(col1, height), strokeWidth)
                    drawLine(gridColor, Offset(col2, 0f), Offset(col2, height), strokeWidth)
                    drawLine(gridColor, Offset(0f, row1), Offset(width, row1), strokeWidth)
                    drawLine(gridColor, Offset(0f, row2), Offset(width, row2), strokeWidth)
                }

                GridType.CROSSHAIR -> {
                    val cx = width / 2f
                    val cy = height / 2f
                    val armLength = 40f

                    drawLine(gridColor, Offset(cx - armLength, cy), Offset(cx + armLength, cy), strokeWidth)
                    drawLine(gridColor, Offset(cx, cy - armLength), Offset(cx, cy + armLength), strokeWidth)
                    drawCircle(gridColor, radius = 25f, center = Offset(cx, cy), style = Stroke(strokeWidth))
                }

                GridType.NONE -> {
                    // No grid
                }
            }

            // Draw interactive focus reticle ring when tapped
            activePoint?.let { pos ->
                val reticleColor = ProCamAmber
                val ringRadius = 50f
                val cornerLength = 15f

                // Central focus ring
                drawCircle(
                    color = reticleColor,
                    radius = ringRadius,
                    center = pos,
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                    )
                )

                // Sun icon indicator for AE metering
                drawCircle(
                    color = reticleColor,
                    radius = 8f,
                    center = Offset(pos.x + ringRadius + 20f, pos.y)
                )
            }
        }
    }
}
