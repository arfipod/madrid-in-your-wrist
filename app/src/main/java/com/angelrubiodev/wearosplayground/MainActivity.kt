package com.angelrubiodev.wearosplayground

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

private const val LOG_TAG = "WearLoop"

class MainActivity : Activity() {
    private var counter = 0
    private lateinit var counterText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(LOG_TAG, "MainActivity created. Build timestamp=${BuildConfig.BUILD_TIMESTAMP}")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            setPadding(24, 24, 24, 24)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val title = TextView(this).apply {
            text = "Hello Pixel Watch 3"
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
        }

        val build = TextView(this).apply {
            text = "Build: ${BuildConfig.BUILD_TIMESTAMP}"
            setTextColor(Color.LTGRAY)
            textSize = 10f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 16)
        }

        counterText = TextView(this).apply {
            text = "Counter: 0"
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 8)
        }

        val button = Button(this).apply {
            text = "Tap"
            setOnClickListener {
                counter += 1
                counterText.text = "Counter: $counter"
                Log.i(LOG_TAG, "Counter incremented to $counter")
                vibrateShort()
            }
        }

        root.addView(title)
        root.addView(build)
        root.addView(counterText)
        root.addView(button)
        setContentView(root)
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
