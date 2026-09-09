package com.sondeptrai.procam.core.computational

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

class AntiShakeDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val _isSteady = MutableStateFlow(true)
    val isSteady: StateFlow<Boolean> = _isSteady.asStateFlow()

    private var steadyFramesCount = 0
    private val requiredSteadyFrames = 15 // ~300ms at 50Hz sensor rate

    private val accelThreshold = 0.25f // m/s^2
    private val gyroThreshold = 0.15f // rad/s

    private var currentAccelEnergy = 0.0f
    private var currentGyroEnergy = 0.0f

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                currentAccelEnergy = sqrt(x * x + y * y + z * z)
            }
            Sensor.TYPE_GYROSCOPE -> {
                val wx = event.values[0]
                val wy = event.values[1]
                val wz = event.values[2]
                currentGyroEnergy = sqrt(wx * wx + wy * wy + wz * wz)
            }
        }

        val isNowCalm = currentAccelEnergy < accelThreshold && currentGyroEnergy < gyroThreshold
        if (isNowCalm) {
            steadyFramesCount++
            if (steadyFramesCount >= requiredSteadyFrames) {
                _isSteady.value = true
            }
        } else {
            steadyFramesCount = 0
            _isSteady.value = false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
