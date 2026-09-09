package com.sondeptrai.procam.ui.viewmodel

import android.app.Application
import android.view.Surface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sondeptrai.procam.core.camera.*
import com.sondeptrai.procam.core.computational.AntiShakeDetector
import com.sondeptrai.procam.core.gl.HistogramData
import com.sondeptrai.procam.core.sensor.LevelOrientation
import com.sondeptrai.procam.core.sensor.OrientationLevelSensor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val cameraController = Camera2Controller(application, viewModelScope)
    private val antiShakeDetector = AntiShakeDetector(application)
    private val orientationLevelSensor = OrientationLevelSensor(application)

    val uiState: StateFlow<CameraUiState> = cameraController.uiState
    val levelState: StateFlow<LevelOrientation> = orientationLevelSensor.levelState
    val isSteady: StateFlow<Boolean> = antiShakeDetector.isSteady

    private val _histogramData = MutableStateFlow(HistogramData())
    val histogramData: StateFlow<HistogramData> = _histogramData.asStateFlow()

    init {
        antiShakeDetector.start()
        orientationLevelSensor.start()
    }

    fun onSurfaceCreated(surface: Surface) {
        cameraController.openCamera(surface)
    }

    fun onShutterPressed() {
        cameraController.captureStillPhoto()
    }

    fun onSwitchCamera() {
        cameraController.switchCamera()
    }

    fun onOpenGallery(context: android.content.Context) {
        val uri = uiState.value.lastCapturedUri
        val intent = if (uri != null) {
            android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/*")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Không thể mở Thư viện", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun onIsoChanged(iso: Int, isAuto: Boolean) {
        cameraController.setIso(iso, isAuto)
    }

    fun onShutterChanged(nanos: Long, isAuto: Boolean) {
        cameraController.setShutterSpeed(nanos, isAuto)
    }

    fun onFocusChanged(distance: Float, isAuto: Boolean) {
        cameraController.setManualFocus(distance, isAuto)
    }

    fun onKelvinChanged(kelvin: Int, isAuto: Boolean) {
        cameraController.setKelvin(kelvin, isAuto)
    }

    fun onEvChanged(evIndex: Int) {
        cameraController.setEv(evIndex)
    }

    fun onEvChanged(evValue: Float) {
        cameraController.setEvValue(evValue)
    }

    fun onZoomChanged(zoom: Float) {
        cameraController.setZoomRatio(zoom)
    }

    fun onParamSelected(param: ManualParameter) {
        cameraController.setActiveManualParam(param)
    }

    fun onModeSelected(mode: ShootingMode) {
        cameraController.setShootingMode(mode)
    }

    fun onFlashToggle() {
        cameraController.toggleFlash()
    }

    fun onRawToggle() {
        cameraController.toggleRaw()
    }

    fun onTiffToggle() {
        cameraController.toggleTiff()
    }

    fun onSbrtToggle() {
        cameraController.toggleSbrt()
    }

    fun onAebToggle() {
        cameraController.toggleAeb()
    }

    fun onSetMenuToggle() {
        cameraController.toggleSetMenu()
    }

    fun onModeDrawerToggle() {
        cameraController.toggleModeDrawer()
    }

    fun onTimerCycle() {
        cameraController.cycleTimer()
    }

    fun onAeAfLockToggle() {
        cameraController.toggleAeAfLock()
    }

    fun onWbLockToggle() {
        cameraController.toggleWbLock()
    }

    fun onPeakingToggle() {
        cameraController.toggleFocusPeaking()
    }

    fun onZebraToggle() {
        cameraController.toggleZebra()
    }

    fun onGridToggle() {
        val current = uiState.value.gridType
        val next = when (current) {
            GridType.NONE -> GridType.RULE_OF_THIRDS
            GridType.RULE_OF_THIRDS -> GridType.GOLDEN_RATIO
            GridType.GOLDEN_RATIO -> GridType.CROSSHAIR
            GridType.CROSSHAIR -> GridType.NONE
        }
        cameraController.setGridType(next)
    }

    fun onAspectRatioToggle() {
        cameraController.cycleAspectRatio()
    }

    fun onTiltMeterToggle() {
        cameraController.toggleTiltMeter()
    }

    fun onHistogramToggle() {
        cameraController.toggleHistogram()
    }

    override fun onCleared() {
        super.onCleared()
        antiShakeDetector.stop()
        orientationLevelSensor.stop()
        cameraController.closeCamera()
    }
}
