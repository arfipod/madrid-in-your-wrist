package com.arfipod.madridinyourwrist

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

private const val LOG_TAG = "WearLoop"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(LOG_TAG, "MainActivity created. Build timestamp=${BuildConfig.BUILD_TIMESTAMP}")

        setContent {
            MadridInYourWristApp(
                onEvent = { message -> Log.i(LOG_TAG, message) },
                onHaptic = ::vibrateShort,
            )
        }
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
