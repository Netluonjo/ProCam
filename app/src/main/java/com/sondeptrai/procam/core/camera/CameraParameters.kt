package com.sondeptrai.procam.core.camera

import android.hardware.camera2.CameraCharacteristics
import android.net.Uri
import android.util.Range
import android.util.Rational

enum class ShootingMode(val title: String) {
    PHOTO("PHOTO"),
    MANUAL("MANUAL"),
    SLOW_SHUTTER("SLOW SHUTTER"),
    BURST("BURST"),
    ANTI_SHAKE("ANTI-SHAKE"),
    VIDEO("VIDEO"),
    PORTRAIT("PORTRAIT")
}

enum class SlowShutterMode(val title: String) {
    LIGHT_TRAILS("Light Trails"),
    MOTION_BLUR("Motion Blur")
}

enum class ManualParameter(val label: String) {
    NONE("AUTO"),
    AE("AE"),
    SHUTTER("SEC"),
    ISO("ISO"),
    FOCUS("AF"),
    WB("AWB"),
    LOCK("E/F");

    companion object {
        val EV get() = AE
    }
}

enum class PeakingColor(val hexColor: Int, val displayName: String) {
    GREEN(0xFF00E676.toInt(), "Green"),
    RED(0xFFFF1744.toInt(), "Red"),
    CYAN(0xFF00E5FF.toInt(), "Cyan")
}

enum class GridType(val title: String) {
    NONE("Off"),
    RULE_OF_THIRDS("3x3"),
    GOLDEN_RATIO("Golden"),
    CROSSHAIR("Target")
}

enum class AspectRatio(val ratio: Float, val label: String) {
    RATIO_4_3(4f / 3f, "4:3"),
    RATIO_16_9(16f / 9f, "16:9"),
    RATIO_1_1(1f, "1:1")
}

data class CameraHardwareInfo(
    val hardwareLevel: Int = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
    val isManualSensorSupported: Boolean = false,
    val isRawSupported: Boolean = false,
    val isoRange: Range<Int> = Range(50, 3200),
    val exposureTimeRange: Range<Long> = Range(125000L, 1000000000L), // 1/8000s to 1s in ns
    val minFocusDistance: Float = 10f, // diopters (1/d in meters, e.g. 10 means 10cm min focus)
    val aeCompensationRange: Range<Int> = Range(-12, 12),
    val aeCompensationStep: Rational = Rational(1, 3),
    val sensorOrientation: Int = 90,
    val maxDigitalZoom: Float = 10f,
    val zoomRatioRange: Range<Float> = Range(1f, 10f)
) {
    val isFullOrLevel3: Boolean
        get() = hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ||
                hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3

    fun formatShutterSpeed(nanos: Long): String {
        val seconds = nanos / 1_000_000_000.0
        return when {
            seconds >= 1.0 -> String.format("%.1f\"", seconds)
            seconds > 0.0 -> {
                val denom = Math.round(1.0 / seconds).toInt()
                "1/$denom"
            }
            else -> "Auto"
        }
    }

    fun formatEv(evValue: Float): String {
        return when {
            evValue > 0f -> String.format("+%.1f", evValue)
            evValue < 0f -> String.format("%.1f", evValue)
            else -> "+0.0"
        }
    }

    fun formatEv(stepIndex: Int): String {
        val evValue = stepIndex * aeCompensationStep.toFloat()
        return if (evValue > 0) String.format("+%.1f", evValue) else String.format("%.1f", evValue)
    }
}

data class CameraUiState(
    val isInitialized: Boolean = false,
    val selectedMode: ShootingMode = ShootingMode.MANUAL,
    val activeManualParam: ManualParameter = ManualParameter.NONE,
    
    // Exposure controls
    val isAutoIso: Boolean = true,
    val currentIso: Int = 100,
    
    val isAutoShutter: Boolean = true,
    val currentExposureTimeNanos: Long = 10_000_000L, // 1/100s
    
    val isAutoFocus: Boolean = true,
    val currentFocusDistance: Float = 0f, // 0.0 = infinity, max = macro
    
    val isAutoWb: Boolean = true,
    val currentKelvin: Int = 5500, // 2000K - 10000K
    
    val currentEvValue: Float = 0.0f,
    val currentEvIndex: Int = 0,
    val currentZoomRatio: Float = 1.0f,
    
    // Capture format
    val isRawEnabled: Boolean = false,
    val flashMode: Int = 0, // 0: Off, 1: Auto, 2: On, 3: Torch
    
    // Assistance overlays
    val isFocusPeakingEnabled: Boolean = false,
    val peakingColor: PeakingColor = PeakingColor.GREEN,
    val isZebraEnabled: Boolean = false,
    val zebraThreshold: Float = 0.95f,
    val gridType: GridType = GridType.RULE_OF_THIRDS,
    val isHistogramEnabled: Boolean = true,
    val isTiltMeterEnabled: Boolean = true,
    val aspectRatio: AspectRatio = AspectRatio.RATIO_4_3,
    
    // Slow Shutter & Anti-Shake
    val slowShutterMode: SlowShutterMode = SlowShutterMode.LIGHT_TRAILS,
    val slowShutterDurationSec: Int = 4,
    val isCapturingLongExposure: Boolean = false,
    val isSteadyForCapture: Boolean = true,
    
    // Video
    val isRecordingVideo: Boolean = false,
    val videoRecordingSeconds: Int = 0,
    val audioDecibelLevel: Float = 0f,
    
    // Front Camera & Feedback
    val isFrontCamera: Boolean = false,
    val isShutterFlashing: Boolean = false,
    val lastCapturedUri: Uri? = null,
    
    // ProCam Authentic Controls
    val isAeAfLocked: Boolean = false,
    val isWbLocked: Boolean = false,
    val timerSeconds: Int = 0, // 0: Off, 3: 3s, 10: 10s
    val isModeDrawerExpanded: Boolean = false,
    val isSetMenuOpen: Boolean = false,
    val isTiffEnabled: Boolean = false,
    val isSbrtEnabled: Boolean = false,
    val isAebEnabled: Boolean = false,
    val batteryPercent: Int = 85,
    val freeStorageGb: String = "127.8",
    val sensorMegapixels: String = "12MP",

    // Hardware info
    val hardwareInfo: CameraHardwareInfo = CameraHardwareInfo()
)
