package com.sondeptrai.procam.core.gl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.max

data class HistogramData(
    val lumaBins: FloatArray = FloatArray(256),
    val maxBinCount: Float = 1.0f
)

object LiveHistogramCalculator {

    /**
     * Compute normalized luminance histogram (256 bins) from a YUV_420_888 Y plane buffer
     * Samples every step pixel to ensure sub-10ms calculation time.
     */
    suspend fun calculateFromYuvYPlane(
        yBuffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int
    ): HistogramData = withContext(Dispatchers.Default) {
        val rawBins = IntArray(256)
        val sampleStep = 8 // Sample 1 out of 64 pixels for blazing fast real-time performance

        val buffer = yBuffer.duplicate()
        var maxCount = 0

        for (row in 0 until height step sampleStep) {
            val rowOffset = row * rowStride
            for (col in 0 until width step sampleStep) {
                val index = rowOffset + col
                if (index < buffer.limit()) {
                    val y = buffer.get(index).toInt() and 0xFF
                    rawBins[y]++
                    if (rawBins[y] > maxCount) {
                        maxCount = rawBins[y]
                    }
                }
            }
        }

        val normalizedBins = FloatArray(256)
        val safeMax = max(maxCount.toFloat(), 1.0f)
        for (i in 0 until 256) {
            normalizedBins[i] = rawBins[i] / safeMax
        }

        HistogramData(
            lumaBins = normalizedBins,
            maxBinCount = safeMax
        )
    }
}
