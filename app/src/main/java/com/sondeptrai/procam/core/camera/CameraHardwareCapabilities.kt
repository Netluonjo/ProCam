package com.sondeptrai.procam.core.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.util.Log
import android.util.Range
import android.util.Rational

object CameraHardwareCapabilities {
    private const val TAG = "CameraCaps"

    fun getBackCameraId(cameraManager: CameraManager): String? {
        return cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: cameraManager.cameraIdList.firstOrNull()
    }

    fun getFrontCameraId(cameraManager: CameraManager): String? {
        return cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        }
    }

    fun queryCapabilities(context: Context, cameraId: String): CameraHardwareInfo {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val chars = manager.getCameraCharacteristics(cameraId)

        val hardwareLevel = chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            ?: CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY

        val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
        val isManualSensor = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
        val isRawSupported = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)

        val isoRange = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
            ?: Range(100, 3200)

        val exposureTimeRange = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
            ?: Range(100_000L, 1_000_000_000L) // 0.1ms to 1s fallback

        val minFocusDist = chars.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 10f

        val aeCompensationRange = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
            ?: Range(-12, 12)

        val aeStep = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
            ?: Rational(1, 3)

        val sensorOrientation = chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90

        Log.d(TAG, "Device Hardware Level: $hardwareLevel (FULL=1, LEVEL_3=3, LIMITED=0, LEGACY=2)")
        Log.d(TAG, "Manual Sensor Support: $isManualSensor, RAW Support: $isRawSupported")
        Log.d(TAG, "ISO Range: ${isoRange.lower} - ${isoRange.upper}")
        Log.d(TAG, "Shutter Range (ns): ${exposureTimeRange.lower} - ${exposureTimeRange.upper}")
        Log.d(TAG, "Min Focus Distance: $minFocusDist diopters")

        return CameraHardwareInfo(
            hardwareLevel = hardwareLevel,
            isManualSensorSupported = isManualSensor,
            isRawSupported = isRawSupported,
            isoRange = isoRange,
            exposureTimeRange = exposureTimeRange,
            minFocusDistance = minFocusDist,
            aeCompensationRange = aeCompensationRange,
            aeCompensationStep = aeStep,
            sensorOrientation = sensorOrientation
        )
    }
}
