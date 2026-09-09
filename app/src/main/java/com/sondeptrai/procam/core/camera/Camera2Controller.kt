package com.sondeptrai.procam.core.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.RggbChannelVector
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

class Camera2Controller(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "Camera2Controller"
    }

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val rawCaptureManager = RawCaptureManager(context)

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null

    private var jpegImageReader: ImageReader? = null
    private var rawImageReader: ImageReader? = null
    private var previewSurface: Surface? = null

    private var currentCameraId: String = ""
    private var cameraCharacteristics: CameraCharacteristics? = null

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("Camera2Background").apply {
                start()
                backgroundHandler = Handler(looper)
            }
        }
    }

    fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera(surface: Surface) {
        startBackgroundThread()
        this.previewSurface = surface

        currentCameraId = CameraHardwareCapabilities.getBackCameraId(cameraManager) ?: return
        cameraCharacteristics = cameraManager.getCameraCharacteristics(currentCameraId)

        val hardwareInfo = CameraHardwareCapabilities.queryCapabilities(context, currentCameraId)
        _uiState.value = _uiState.value.copy(
            hardwareInfo = hardwareInfo,
            currentIso = hardwareInfo.isoRange.lower,
            currentExposureTimeNanos = 10_000_000L // 1/100s default
        )

        setupImageReaders()

        try {
            cameraManager.openCamera(currentCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d(TAG, "Camera opened: ${camera.id}")
                    cameraDevice = camera
                    createCaptureSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.w(TAG, "Camera disconnected")
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera open error code: $error")
                    camera.close()
                    cameraDevice = null
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera", e)
        }
    }

    private fun setupImageReaders() {
        // High-res JPEG Reader
        jpegImageReader = ImageReader.newInstance(4000, 3000, ImageFormat.JPEG, 2).apply {
            setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                val buffer: ByteBuffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                image.close()

                scope.launch {
                    rawCaptureManager.saveJpegImage(bytes)
                }
            }, backgroundHandler)
        }

        // RAW_SENSOR Reader if supported
        if (_uiState.value.hardwareInfo.isRawSupported) {
            rawImageReader = ImageReader.newInstance(4000, 3000, ImageFormat.RAW_SENSOR, 2)
        }
    }

    private fun createCaptureSession() {
        val device = cameraDevice ?: return
        val surface = previewSurface ?: return

        try {
            val targets = mutableListOf(surface, jpegImageReader!!.surface)
            rawImageReader?.let { targets.add(it.surface) }

            device.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (cameraDevice == null) return
                    captureSession = session

                    previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                        addTarget(surface)
                    }

                    applyParametersToBuilder(previewRequestBuilder!!)
                    sendRepeatingRequest()

                    _uiState.value = _uiState.value.copy(isInitialized = true)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Camera capture session configuration failed")
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create capture session", e)
        }
    }

    private fun sendRepeatingRequest() {
        val session = captureSession ?: return
        val builder = previewRequestBuilder ?: return

        try {
            session.setRepeatingRequest(builder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    // Real-time metadata feedback from hardware sensor
                    val actualIso = result.get(CaptureResult.SENSOR_SENSITIVITY)
                    val actualExposure = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)
                    val actualFocus = result.get(CaptureResult.LENS_FOCUS_DISTANCE)

                    if (_uiState.value.isAutoIso && actualIso != null) {
                        _uiState.value = _uiState.value.copy(currentIso = actualIso)
                    }
                    if (_uiState.value.isAutoShutter && actualExposure != null) {
                        _uiState.value = _uiState.value.copy(currentExposureTimeNanos = actualExposure)
                    }
                    if (_uiState.value.isAutoFocus && actualFocus != null) {
                        _uiState.value = _uiState.value.copy(currentFocusDistance = actualFocus)
                    }
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting repeating request", e)
        }
    }

    private fun applyParametersToBuilder(builder: CaptureRequest.Builder) {
        val state = _uiState.value
        val info = state.hardwareInfo

        // 1. ISO and Shutter Speed Control
        if (state.isAutoIso && state.isAutoShutter) {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, state.currentEvIndex)
        } else {
            // Manual Sensor Exposure
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)

            val clampedIso = min(max(state.currentIso, info.isoRange.lower), info.isoRange.upper)
            builder.set(CaptureRequest.SENSOR_SENSITIVITY, clampedIso)

            val clampedExposure = min(
                max(state.currentExposureTimeNanos, info.exposureTimeRange.lower),
                info.exposureTimeRange.upper
            )
            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, clampedExposure)
        }

        // 2. Focus Distance Control (Manual Focus vs Auto Focus)
        if (state.isAutoFocus) {
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        } else {
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            val clampedDist = min(max(state.currentFocusDistance, 0f), info.minFocusDistance)
            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, clampedDist)
        }

        // 3. White Balance (Kelvin & Color Correction Gains)
        if (state.isAutoWb) {
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
        } else {
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
            val gains = kelvinToRggbGains(state.currentKelvin)
            builder.set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)
        }

        // 4. Flash Mode
        when (state.flashMode) {
            1 -> builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
            2 -> builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE)
            3 -> builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
            else -> builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
        }
    }

    /**
     * Converts a Color Temperature in Kelvin (2000K - 10000K) to RGGB White Balance Gains
     */
    private fun kelvinToRggbGains(kelvin: Int): RggbChannelVector {
        val temp = kelvin / 100.0
        val r: Double
        val b: Double
        val g = 1.0

        if (temp <= 66) {
            r = 2.0
            b = if (temp <= 19) 0.0 else (138.5177312231 * Math.log(temp - 10) - 305.0447927307) / 255.0
        } else {
            r = (329.698727446 * Math.pow(temp - 60, -0.1332047592)) / 255.0
            b = 2.0
        }

        val rGain = min(max(r.toFloat(), 1.0f), 3.5f)
        val bGain = min(max(b.toFloat(), 1.0f), 3.5f)
        return RggbChannelVector(rGain, g.toFloat(), g.toFloat(), bGain)
    }

    // Public setter methods triggered by UI
    fun setIso(iso: Int, isAuto: Boolean = false) {
        _uiState.value = _uiState.value.copy(currentIso = iso, isAutoIso = isAuto)
        previewRequestBuilder?.let {
            applyParametersToBuilder(it)
            sendRepeatingRequest()
        }
    }

    fun setShutterSpeed(nanos: Long, isAuto: Boolean = false) {
        _uiState.value = _uiState.value.copy(currentExposureTimeNanos = nanos, isAutoShutter = isAuto)
        previewRequestBuilder?.let {
            applyParametersToBuilder(it)
            sendRepeatingRequest()
        }
    }

    fun setManualFocus(distance: Float, isAuto: Boolean = false) {
        _uiState.value = _uiState.value.copy(currentFocusDistance = distance, isAutoFocus = isAuto)
        previewRequestBuilder?.let {
            applyParametersToBuilder(it)
            sendRepeatingRequest()
        }
    }

    fun setKelvin(kelvin: Int, isAuto: Boolean = false) {
        _uiState.value = _uiState.value.copy(currentKelvin = kelvin, isAutoWb = isAuto)
        previewRequestBuilder?.let {
            applyParametersToBuilder(it)
            sendRepeatingRequest()
        }
    }

    fun setEv(evIndex: Int) {
        _uiState.value = _uiState.value.copy(currentEvIndex = evIndex)
        previewRequestBuilder?.let {
            applyParametersToBuilder(it)
            sendRepeatingRequest()
        }
    }

    fun setShootingMode(mode: ShootingMode) {
        _uiState.value = _uiState.value.copy(selectedMode = mode)
    }

    fun setActiveManualParam(param: ManualParameter) {
        _uiState.value = _uiState.value.copy(activeManualParam = param)
    }

    fun toggleFocusPeaking() {
        _uiState.value = _uiState.value.copy(isFocusPeakingEnabled = !_uiState.value.isFocusPeakingEnabled)
    }

    fun toggleZebra() {
        _uiState.value = _uiState.value.copy(isZebraEnabled = !_uiState.value.isZebraEnabled)
    }

    fun toggleRaw() {
        if (_uiState.value.hardwareInfo.isRawSupported) {
            _uiState.value = _uiState.value.copy(isRawEnabled = !_uiState.value.isRawEnabled)
        }
    }

    fun setGridType(gridType: GridType) {
        _uiState.value = _uiState.value.copy(gridType = gridType)
    }

    fun captureStillPhoto() {
        val device = cameraDevice ?: return
        val session = captureSession ?: return

        try {
            val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            applyParametersToBuilder(captureBuilder)

            if (_uiState.value.isRawEnabled && rawImageReader != null) {
                captureBuilder.addTarget(rawImageReader!!.surface)
                rawImageReader!!.setOnImageAvailableListener({ reader ->
                    val rawImage = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                    // Wait for result callback to save DNG
                }, backgroundHandler)
            } else {
                captureBuilder.addTarget(jpegImageReader!!.surface)
            }

            session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    Log.d(TAG, "Photo captured successfully")
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture still photo", e)
        }
    }

    fun closeCamera() {
        try {
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            jpegImageReader?.close()
            jpegImageReader = null
            rawImageReader?.close()
            rawImageReader = null
            stopBackgroundThread()
            _uiState.value = _uiState.value.copy(isInitialized = false)
        } catch (e: Exception) {
            Log.e(TAG, "Error closing camera", e)
        }
    }
}
