package com.arfipod.wearosplayground

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

private const val LOG_TAG = "WearLoop"

class MainActivity : ComponentActivity() {
    private lateinit var sensorExperimentLogger: SensorExperimentLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorExperimentLogger = SensorExperimentLogger(this) { message ->
            Log.i(LOG_TAG, message)
        }

        Log.i(LOG_TAG, "MainActivity created. Build timestamp=${BuildConfig.BUILD_TIMESTAMP}")

        setContent {
            WearLoopApp(
                buildTimestamp = BuildConfig.BUILD_TIMESTAMP,
                onCounterIncremented = { value ->
                    Log.i(LOG_TAG, "Counter incremented to $value")
                    vibrateShort()
                },
                onSensorLoggingChanged = { enabled ->
                    if (enabled) {
                        sensorExperimentLogger.start()
                    } else {
                        sensorExperimentLogger.stop()
                        false
                    }
                },
            )
        }
    }

    override fun onDestroy() {
        sensorExperimentLogger.stop()
        super.onDestroy()
    }

    private fun vibrateShort() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(VibratorManager::class.java)
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        if (!vibrator.hasVibrator()) {
            Log.w(LOG_TAG, "No vibrator available")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(40)
        }
    }
}

@Composable
fun WearLoopApp(
    buildTimestamp: String,
    onCounterIncremented: (Int) -> Unit,
    onSensorLoggingChanged: (Boolean) -> Boolean,
) {
    MaterialTheme {
        var counter by remember { mutableIntStateOf(0) }
        var sensorLogging by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Hello Pixel Watch 3",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Build: $buildTimestamp",
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                    ),
                )
                Text(
                    text = CounterState.labelFor(counter),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        counter += 1
                        onCounterIncremented(counter)
                    },
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "TAP",
                        textAlign = TextAlign.Center,
                    )
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        sensorLogging = onSensorLoggingChanged(!sensorLogging)
                    },
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = if (sensorLogging) "SENSORS ON" else "SENSORS OFF",
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
