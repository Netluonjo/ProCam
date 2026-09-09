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
        val state = uiState.value
        when (state.selectedMode) {
            ShootingMode.ANTI_SHAKE -> {
                if (isSteady.value) {
                    cameraController.captureStillPhoto()
                }
            }
            ShootingMode.SLOW_SHUTTER -> {
                // Trigger frame stacking long exposure sequence
                cameraController.captureStillPhoto()
            }
            else -> {
                cameraController.captureStillPhoto()
            }
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

    fun onParamSelected(param: ManualParameter) {
        cameraController.setActiveManualParam(param)
    }

    fun onModeSelected(mode: ShootingMode) {
        cameraController.setShootingMode(mode)
    }

    fun onFlashToggle() {
        val nextFlash = (uiState.value.flashMode + 1) % 4
        // Handled via controller
    }

    fun onRawToggle() {
        cameraController.toggleRaw()
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
        // Toggle 4:3 -> 16:9 -> 1:1
    }

    fun onTiltMeterToggle() {
        // Toggle tiltmeter visibility
    }

    override fun onCleared() {
        super.onCleared()
        antiShakeDetector.stop()
        orientationLevelSensor.stop()
        cameraController.closeCamera()
    }
}
