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

    fun release() {
        queueEvent {
            renderer.release()
        }
    }
}
