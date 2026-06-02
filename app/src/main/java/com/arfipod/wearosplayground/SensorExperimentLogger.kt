package com.arfipod.wearosplayground

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorExperimentLogger(
    context: Context,
    private val log: (String) -> Unit,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var lastLoggedAtNanos = Long.MIN_VALUE

    var isRunning: Boolean = false
        private set

    fun start(): Boolean {
        if (isRunning) {
            return true
        }

        val sensor = accelerometer
        if (sensor == null) {
            log("Sensor experiment unavailable: accelerometer not found")
            return false
        }

        val registered = sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL,
        )

        isRunning = registered
        if (registered) {
            lastLoggedAtNanos = Long.MIN_VALUE
            log("Sensor experiment started: ${sensor.name}")
        } else {
            log("Sensor experiment failed to start: ${sensor.name}")
        }

        return registered
    }

    fun stop() {
        if (!isRunning) {
            return
        }

        sensorManager.unregisterListener(this)
        isRunning = false
        log("Sensor experiment stopped")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER || event.values.size < 3) {
            return
        }

        if (lastLoggedAtNanos != Long.MIN_VALUE &&
            event.timestamp - lastLoggedAtNanos < LOG_INTERVAL_NANOS
        ) {
            return
        }

        lastLoggedAtNanos = event.timestamp
        log(
            SensorSampleFormatter.accelerometerSample(
                x = event.values[0],
                y = event.values[1],
                z = event.values[2],
                timestampNanos = event.timestamp,
            )
        )
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        log("Sensor accuracy changed: ${sensor.name} accuracy=$accuracy")
    }

    companion object {
        private const val LOG_INTERVAL_NANOS = 1_000_000_000L
    }
}
