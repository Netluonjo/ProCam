package com.sondeptrai.procam.core.gl

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.Surface
import com.sondeptrai.procam.core.camera.PeakingColor

class CameraGlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    onSurfaceCreated: (Surface) -> Unit = {}
) : GLSurfaceView(context, attrs) {

    val renderer: CameraGlRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = CameraGlRenderer(onSurfaceCreated)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun updatePeaking(enabled: Boolean, color: PeakingColor, threshold: Float = 0.28f) {
        queueEvent {
            renderer.isFocusPeakingEnabled = enabled
            renderer.peakingColor = color
            renderer.peakingThreshold = threshold
        }
    }

    fun updateZebra(enabled: Boolean, threshold: Float = 0.95f) {
        queueEvent {
            renderer.isZebraEnabled = enabled
            renderer.zebraThreshold = threshold
        }
    }

    fun updateFrontCamera(isFront: Boolean) {
        queueEvent {
            renderer.isFrontCamera = isFront
        }
    }

    fun updateWhiteBalance(kelvin: Int, isAuto: Boolean) {
        queueEvent {
            if (isAuto) {
                renderer.updateWbTint(1.0f, 1.0f, 1.0f)
            } else {
                val norm = (kelvin - 5500f) / 4500f
                val r: Float
                val g: Float
                val b: Float
                if (norm >= 0f) {
                    // Warmer temperature (6000K - 10000K): enhance warm amber/red tones
                    r = (1.0f + norm * 0.40f).coerceIn(0.6f, 1.5f)
                    g = (1.0f + norm * 0.05f).coerceIn(0.8f, 1.2f)
                    b = (1.0f - norm * 0.35f).coerceIn(0.5f, 1.5f)
                } else {
                    // Cooler temperature (2000K - 5000K): enhance cool daylight/blue tones
                    val factor = -norm
                    r = (1.0f - factor * 0.35f).coerceIn(0.5f, 1.5f)
                    g = (1.0f - factor * 0.05f).coerceIn(0.8f, 1.2f)
                    b = (1.0f + factor * 0.40f).coerceIn(0.6f, 1.5f)
                }
                renderer.updateWbTint(r, g, b)
            }
        }
    }

    fun release() {
        queueEvent {
            renderer.release()
        }
    }
}
