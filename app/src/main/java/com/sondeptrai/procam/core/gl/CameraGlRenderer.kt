package com.sondeptrai.procam.core.gl

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import android.view.Surface
import com.sondeptrai.procam.core.camera.PeakingColor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraGlRenderer(
    private val onSurfaceCreatedCallback: (Surface) -> Unit
) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private var textureId: Int = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var previewSurface: Surface? = null

    private var programId: Int = 0
    private var aPositionLoc: Int = 0
    private var aTextureCoordLoc: Int = 0
    private var uMVPMatrixLoc: Int = 0
    private var uTexMatrixLoc: Int = 0
    private var uTexelSizeLoc: Int = 0
    private var uEnablePeakingLoc: Int = 0
    private var uPeakingColorLoc: Int = 0
    private var uPeakingThresholdLoc: Int = 0
    private var uEnableZebraLoc: Int = 0
    private var uZebraThresholdLoc: Int = 0
    private var uTimeLoc: Int = 0

    private val mvpMatrix = FloatArray(16)
    private val texMatrix = FloatArray(16)

    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer

    @Volatile
    private var updateSurface = false

    // Dynamic Shader Control States
    var isFocusPeakingEnabled = false
    var peakingColor = PeakingColor.GREEN
    var peakingThreshold = 0.28f

    var isZebraEnabled = false
    var zebraThreshold = 0.95f
    var isFrontCamera = false

    private var surfaceWidth = 1920
    private var surfaceHeight = 1080
    private val startTime = SystemClock.uptimeMillis()

    init {
        // Full screen quad coordinates
        val vertexCoords = floatArrayOf(
            -1.0f, -1.0f, 0.0f,
             1.0f, -1.0f, 0.0f,
            -1.0f,  1.0f, 0.0f,
             1.0f,  1.0f, 0.0f
        )
        vertexBuffer = ByteBuffer.allocateDirect(vertexCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertexCoords)
                position(0)
            }

        // Texture coordinates (UV)
        val texCoords = floatArrayOf(
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
        )
        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(texCoords)
                position(0)
            }

        Matrix.setIdentityM(mvpMatrix, 0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        programId = GlUtils.createProgram(GlShaders.VERTEX_SHADER, GlShaders.FRAGMENT_SHADER)

        aPositionLoc = GLES20.glGetAttribLocation(programId, "aPosition")
        aTextureCoordLoc = GLES20.glGetAttribLocation(programId, "aTextureCoord")
        uMVPMatrixLoc = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
        uTexMatrixLoc = GLES20.glGetUniformLocation(programId, "uTexMatrix")
        uTexelSizeLoc = GLES20.glGetUniformLocation(programId, "uTexelSize")
        uEnablePeakingLoc = GLES20.glGetUniformLocation(programId, "uEnablePeaking")
        uPeakingColorLoc = GLES20.glGetUniformLocation(programId, "uPeakingColor")
        uPeakingThresholdLoc = GLES20.glGetUniformLocation(programId, "uPeakingThreshold")
        uEnableZebraLoc = GLES20.glGetUniformLocation(programId, "uEnableZebra")
        uZebraThresholdLoc = GLES20.glGetUniformLocation(programId, "uZebraThreshold")
        uTimeLoc = GLES20.glGetUniformLocation(programId, "uTime")

        // Create OES external texture
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        surfaceTexture = SurfaceTexture(textureId).apply {
            setDefaultBufferSize(1920, 1080)
            setOnFrameAvailableListener(this@CameraGlRenderer)
        }
        previewSurface = Surface(surfaceTexture)
        onSurfaceCreatedCallback(previewSurface!!)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        surfaceWidth = width
        surfaceHeight = height
    }

    override fun onDrawFrame(gl: GL10?) {
        synchronized(this) {
            if (updateSurface) {
                surfaceTexture?.updateTexImage()
                surfaceTexture?.getTransformMatrix(texMatrix)
                updateSurface = false
            }
        }

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        Matrix.setIdentityM(mvpMatrix, 0)
        if (isFrontCamera) {
            Matrix.scaleM(mvpMatrix, 0, -1f, 1f, 1f)
        }

        // Maintain undistorted 16:9 aspect ratio geometry
        val textureAspect = 9.0f / 16.0f
        val viewAspect = if (surfaceHeight > 0) surfaceWidth.toFloat() / surfaceHeight.toFloat() else textureAspect

        if (viewAspect > textureAspect + 0.005f) {
            val scaleY = viewAspect / textureAspect
            Matrix.scaleM(mvpMatrix, 0, 1.0f, scaleY, 1.0f)
        } else if (viewAspect < textureAspect - 0.005f) {
            val scaleX = textureAspect / viewAspect
            Matrix.scaleM(mvpMatrix, 0, scaleX, 1.0f, 1.0f)
        }

        GLES20.glUseProgram(programId)

        // Bind attributes
        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)

        texCoordBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aTextureCoordLoc)
        GLES20.glVertexAttribPointer(aTextureCoordLoc, 2, GLES20.GL_FLOAT, false, 8, texCoordBuffer)

        // Pass matrices
        GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, texMatrix, 0)

        // Pass texel size for Sobel convolution
        GLES20.glUniform2f(uTexelSizeLoc, 1.0f / surfaceWidth, 1.0f / surfaceHeight)

        // Pass Focus Peaking uniforms
        GLES20.glUniform1i(uEnablePeakingLoc, if (isFocusPeakingEnabled) 1 else 0)
        val r = android.graphics.Color.red(peakingColor.hexColor) / 255.0f
        val g = android.graphics.Color.green(peakingColor.hexColor) / 255.0f
        val b = android.graphics.Color.blue(peakingColor.hexColor) / 255.0f
        GLES20.glUniform3f(uPeakingColorLoc, r, g, b)
        GLES20.glUniform1f(uPeakingThresholdLoc, peakingThreshold)

        // Pass Zebra Stripes uniforms
        GLES20.glUniform1i(uEnableZebraLoc, if (isZebraEnabled) 1 else 0)
        GLES20.glUniform1f(uZebraThresholdLoc, zebraThreshold)

        val timeSec = (SystemClock.uptimeMillis() - startTime) / 1000.0f
        GLES20.glUniform1f(uTimeLoc, timeSec)

        // Active texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        // Draw quad
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPositionLoc)
        GLES20.glDisableVertexAttribArray(aTextureCoordLoc)
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        synchronized(this) {
            updateSurface = true
        }
    }

    fun release() {
        previewSurface?.release()
        previewSurface = null
        surfaceTexture?.release()
        surfaceTexture = null
    }
}
