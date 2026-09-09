package com.sondeptrai.procam.core.computational

import android.graphics.Bitmap
import android.graphics.Color
import com.sondeptrai.procam.core.camera.SlowShutterMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class FrameStackingEngine {
    private var accumulatedBitmap: Bitmap? = null
    private var frameCount: Int = 0
    private val lock = Any()

    fun reset() {
        synchronized(lock) {
            accumulatedBitmap?.recycle()
            accumulatedBitmap = null
            frameCount = 0
        }
    }

    /**
     * Stacks a new frame onto the accumulated buffer using the selected mode.
     */
    suspend fun processFrame(newFrame: Bitmap, mode: SlowShutterMode): Bitmap = withContext(Dispatchers.Default) {
        synchronized(lock) {
            val width = newFrame.width
            val height = newFrame.height

            if (accumulatedBitmap == null || accumulatedBitmap!!.width != width || accumulatedBitmap!!.height != height) {
                accumulatedBitmap = newFrame.copy(Bitmap.Config.ARGB_8888, true)
                frameCount = 1
                return@withContext accumulatedBitmap!!
            }

            val currentAcc = accumulatedBitmap!!
            val pixelsAcc = IntArray(width * height)
            val pixelsNew = IntArray(width * height)

            currentAcc.getPixels(pixelsAcc, 0, width, 0, 0, width, height)
            newFrame.getPixels(pixelsNew, 0, width, 0, 0, width, height)

            when (mode) {
                SlowShutterMode.LIGHT_TRAILS -> {
                    // Maximum luminance blend: keep the brightest pixels (Headlights, Star trails)
                    for (i in pixelsAcc.indices) {
                        val c1 = pixelsAcc[i]
                        val c2 = pixelsNew[i]

                        val r = max(Color.red(c1), Color.red(c2))
                        val g = max(Color.green(c1), Color.green(c2))
                        val b = max(Color.blue(c1), Color.blue(c2))

                        pixelsAcc[i] = Color.rgb(r, g, b)
                    }
                }
                SlowShutterMode.MOTION_BLUR -> {
                    // Running weighted average to create buttery smooth water/clouds
                    frameCount++
                    val alpha = 1.0f / min(frameCount.toFloat(), 30.0f)
                    val invAlpha = 1.0f - alpha

                    for (i in pixelsAcc.indices) {
                        val c1 = pixelsAcc[i]
                        val c2 = pixelsNew[i]

                        val r = (Color.red(c1) * invAlpha + Color.red(c2) * alpha).toInt()
                        val g = (Color.green(c1) * invAlpha + Color.green(c2) * alpha).toInt()
                        val b = (Color.blue(c1) * invAlpha + Color.blue(c2) * alpha).toInt()

                        pixelsAcc[i] = Color.rgb(r, g, b)
                    }
                }
            }

            currentAcc.setPixels(pixelsAcc, 0, width, 0, 0, width, height)
            return@withContext currentAcc
        }
    }

    fun getResult(): Bitmap? {
        synchronized(lock) {
            return accumulatedBitmap?.copy(Bitmap.Config.ARGB_8888, false)
        }
    }
}
