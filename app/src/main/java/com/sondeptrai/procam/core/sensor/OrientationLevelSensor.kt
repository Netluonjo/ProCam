package com.sondeptrai.procam.core.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class LevelOrientation(
    val rollAngle: Float = 0f,   // In degrees (-180 to 180)
    val pitchAngle: Float = 0f,  // In degrees (-90 to 90)
    val isLevel: Boolean = false // True if roll is within ±0.75 degree
)

class OrientationLevelSensor(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _levelState = MutableStateFlow(LevelOrientation())
    val levelState: StateFlow<LevelOrientation> = _levelState.asStateFlow()

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // Convert radians to degrees
        val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

        val isLevel = abs(roll) < 0.75f

        _levelState.value = LevelOrientation(
            rollAngle = roll,
            pitchAngle = pitch,
            isLevel = isLevel
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
