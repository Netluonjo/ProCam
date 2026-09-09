package com.sondeptrai.procam.core.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.RggbChannelVector
import android.media.ImageReader
import android.media.MediaActionSound
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val mediaSound = MediaActionSound()

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
            isFrontCamera = false,
            hardwareInfo = hardwareInfo,
            currentIso = hardwareInfo.isoRange.lower,
            currentExposureTimeNanos = 10_000_000L // 1/100s default
        )

        setupImageReaders()
        openCameraDevice()
    }

    @SuppressLint("MissingPermission")
    private fun openCameraDevice() {
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
            Log.e(TAG, "Failed to open camera device: $currentCameraId", e)
        }
    }

    private fun closeCurrentCameraSession() {
        try {
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            jpegImageReader?.close()
            jpegImageReader = null
            rawImageReader?.close()
            rawImageReader = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing camera session", e)
        }
    }

    fun switchCamera() {
        val surface = previewSurface ?: return
        val targetIsFront = !_uiState.value.isFrontCamera
        val nextCameraId = if (targetIsFront) {
            CameraHardwareCapabilities.getFrontCameraId(cameraManager)
        } else {
            CameraHardwareCapabilities.getBackCameraId(cameraManager)
        }

        if (nextCameraId == null) {
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Thiết bị không hỗ trợ camera này", Toast.LENGTH_SHORT).show()
            }
            return
        }

        closeCurrentCameraSession()

        currentCameraId = nextCameraId
        cameraCharacteristics = cameraManager.getCameraCharacteristics(currentCameraId)
        val hardwareInfo = CameraHardwareCapabilities.queryCapabilities(context, currentCameraId)

        _uiState.value = _uiState.value.copy(
            isFrontCamera = targetIsFront,
            hardwareInfo = hardwareInfo,
            currentIso = hardwareInfo.isoRange.lower,
            currentExposureTimeNanos = 10_000_000L
        )

        setupImageReaders()
        openCameraDevice()
    }

    private fun setupImageReaders() {
        val chars = cameraCharacteristics ?: return
        val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return

        // 1. Find best supported JPEG size from hardware sensor
        val jpegSizes = map.getOutputSizes(ImageFormat.JPEG) ?: emptyArray()
        val bestJpegSize = jpegSizes.maxByOrNull { it.width * it.height } ?: Size(1920, 1080)
        Log.d(TAG, "Selected JPEG size: ${bestJpegSize.width}x${bestJpegSize.height}")

        jpegImageReader = ImageReader.newInstance(bestJpegSize.width, bestJpegSize.height, ImageFormat.JPEG, 2).apply {
            setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                val buffer: ByteBuffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                image.close()

                scope.launch {
                    val uri = rawCaptureManager.saveJpegImage(bytes)
                    withContext(Dispatchers.Main) {
                        if (uri != null) {
                            _uiState.value = _uiState.value.copy(lastCapturedUri = uri)
                            Toast.makeText(context, "Đã lưu ảnh vào Thư viện!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Không thể lưu ảnh", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }, backgroundHandler)
        }

        // 2. RAW_SENSOR Reader if supported by hardware
        val rawSizes = map.getOutputSizes(ImageFormat.RAW_SENSOR)
        if (_uiState.value.hardwareInfo.isRawSupported && !rawSizes.isNullOrEmpty()) {
            val bestRawSize = rawSizes.maxByOrNull { it.width * it.height } ?: rawSizes[0]
            Log.d(TAG, "Selected RAW size: ${bestRawSize.width}x${bestRawSize.height}")
            rawImageReader = ImageReader.newInstance(bestRawSize.width, bestRawSize.height, ImageFormat.RAW_SENSOR, 2)
        } else {
            rawImageReader = null
        }
    }

    private fun createCaptureSession() {
        val device = cameraDevice ?: return
        val surface = previewSurface ?: return

        try {
            val targets = mutableListOf(surface)
            jpegImageReader?.let { targets.add(it.surface) }
            rawImageReader?.let { targets.add(it.surface) }

            device.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (cameraDevice == null) return
                    captureSession = session
                    Log.d(TAG, "Camera capture session configured successfully!")

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
                    // Real-time metadata feedback from hardware sensor with change check
                    val actualIso = result.get(CaptureResult.SENSOR_SENSITIVITY)
                    val actualExposure = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)
                    val actualFocus = result.get(CaptureResult.LENS_FOCUS_DISTANCE)

                    val cur = _uiState.value
                    var updated = cur
                    if (cur.isAutoIso && actualIso != null && cur.currentIso != actualIso) {
                        updated = updated.copy(currentIso = actualIso)
                    }
                    if (cur.isAutoShutter && actualExposure != null && cur.currentExposureTimeNanos != actualExposure) {
                        updated = updated.copy(currentExposureTimeNanos = actualExposure)
                    }
                    if (cur.isAutoFocus && actualFocus != null && kotlin.math.abs(cur.currentFocusDistance - actualFocus) > 0.05f) {
                        updated = updated.copy(currentFocusDistance = actualFocus)
                    }
                    if (updated !== cur) {
                        _uiState.value = updated
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

    fun toggleFlash() {
        val next = (_uiState.value.flashMode + 1) % 4
        _uiState.value = _uiState.value.copy(flashMode = next)
        previewRequestBuilder?.let {
            applyParametersToBuilder(it)
            sendRepeatingRequest()
        }
    }

    fun toggleTiff() {
        _uiState.value = _uiState.value.copy(isTiffEnabled = !_uiState.value.isTiffEnabled)
    }

    fun toggleSbrt() {
        _uiState.value = _uiState.value.copy(isSbrtEnabled = !_uiState.value.isSbrtEnabled)
    }

    fun toggleAeb() {
        _uiState.value = _uiState.value.copy(isAebEnabled = !_uiState.value.isAebEnabled)
    }

    fun toggleSetMenu() {
        _uiState.value = _uiState.value.copy(isSetMenuOpen = !_uiState.value.isSetMenuOpen)
    }

    fun toggleModeDrawer() {
        _uiState.value = _uiState.value.copy(isModeDrawerExpanded = !_uiState.value.isModeDrawerExpanded)
    }

    fun cycleTimer() {
        val next = when (_uiState.value.timerSeconds) {
            0 -> 3
            3 -> 10
            else -> 0
        }
        _uiState.value = _uiState.value.copy(timerSeconds = next)
    }

    fun toggleAeAfLock() {
        _uiState.value = _uiState.value.copy(isAeAfLocked = !_uiState.value.isAeAfLocked)
    }

    fun toggleWbLock() {
        _uiState.value = _uiState.value.copy(isWbLocked = !_uiState.value.isWbLocked)
    }

    fun toggleHistogram() {
        _uiState.value = _uiState.value.copy(isHistogramEnabled = !_uiState.value.isHistogramEnabled)
    }

    fun toggleTiltMeter() {
        _uiState.value = _uiState.value.copy(isTiltMeterEnabled = !_uiState.value.isTiltMeterEnabled)
    }

    fun cycleAspectRatio() {
        val next = when (_uiState.value.aspectRatio) {
            AspectRatio.RATIO_4_3 -> AspectRatio.RATIO_16_9
            AspectRatio.RATIO_16_9 -> AspectRatio.RATIO_1_1
            AspectRatio.RATIO_1_1 -> AspectRatio.RATIO_4_3
        }
        _uiState.value = _uiState.value.copy(aspectRatio = next)
    }

    fun setGridType(gridType: GridType) {
        _uiState.value = _uiState.value.copy(gridType = gridType)
    }

    fun captureStillPhoto() {
        val device = cameraDevice ?: run {
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Camera chưa sẵn sàng", Toast.LENGTH_SHORT).show()
            }
            return
        }
        val session = captureSession ?: run {
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Camera session đang kết nối...", Toast.LENGTH_SHORT).show()
            }
            return
        }

        try {
            // 1. Play real physical shutter click sound
            try {
                mediaSound.play(MediaActionSound.SHUTTER_CLICK)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to play shutter sound", e)
            }

            // 2. Trigger white screen flash visual feedback
            scope.launch {
                _uiState.value = _uiState.value.copy(isShutterFlashing = true)
                delay(80)
                _uiState.value = _uiState.value.copy(isShutterFlashing = false)
            }

            val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            applyParametersToBuilder(captureBuilder)

            // 3. Set JPEG Orientation based on sensor orientation and front/back lens
            val sensorOrientation = cameraCharacteristics?.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90
            val isFront = _uiState.value.isFrontCamera
            val jpegOrientation = if (isFront) {
                (sensorOrientation + 270) % 360
            } else {
                sensorOrientation
            }
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation)

            if (_uiState.value.isRawEnabled && rawImageReader != null) {
                captureBuilder.addTarget(rawImageReader!!.surface)
                rawImageReader!!.setOnImageAvailableListener({ reader ->
                    val rawImage = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                    // Wait for result callback to save DNG
                }, backgroundHandler)
            } else if (jpegImageReader != null) {
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

                override fun onCaptureFailed(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    failure: CaptureFailure
                ) {
                    super.onCaptureFailed(session, request, failure)
                    Log.e(TAG, "Capture failed: ${failure.reason}")
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
