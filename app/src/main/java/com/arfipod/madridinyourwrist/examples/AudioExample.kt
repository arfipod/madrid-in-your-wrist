package com.arfipod.madridinyourwrist.examples

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay

data class AudioNote(
    val name: String,
    val durationMs: Int,
    val pauseMs: Int,
)

object AudioExampleMelody {
    val notes = listOf(
        AudioNote(name = "beep", durationMs = 120, pauseMs = 30),
        AudioNote(name = "beep2", durationMs = 120, pauseMs = 30),
        AudioNote(name = "ack", durationMs = 180, pauseMs = 40),
    )

    fun totalDurationMs(): Int = notes.sumOf { it.durationMs + it.pauseMs }

    fun activeBars(progress: Float, barCount: Int = 12): Int {
        require(barCount > 0) { "Bar count must be positive." }
        return (progress.coerceIn(0f, 1f) * barCount).toInt().coerceIn(0, barCount)
    }
}

@Composable
fun AudioExample(
    onBack: () -> Unit,
    onEvent: (String) -> Unit,
) {
    var playRequest by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }
    val toneGenerator = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }
            .onFailure { error = it.message ?: "Audio init failed" }
            .getOrNull()
    }

    DisposableEffect(toneGenerator) {
        onDispose {
            toneGenerator?.release()
        }
    }

    LaunchedEffect(playRequest) {
        if (playRequest == 0 || toneGenerator == null) return@LaunchedEffect
        playing = true
        progress = 0f
        error = null
        onEvent("Audio example playback started")

        val total = AudioExampleMelody.totalDurationMs().coerceAtLeast(1)
        var elapsed = 0
        AudioExampleMelody.notes.forEach { note ->
            toneGenerator.startTone(note.toToneType(), note.durationMs)
            delay((note.durationMs + note.pauseMs).toLong())
            elapsed += note.durationMs + note.pauseMs
            progress = elapsed.toFloat() / total
        }

        playing = false
        progress = 0f
        onEvent("Audio example playback finished")
    }

    ExampleChrome(title = ExampleKind.AUDIO.title, onBack = onBack) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .fillMaxSize(),
        ) {
            drawRect(color = Color(0xFF151515), size = size)
            val bars = 12
            val activeBars = AudioExampleMelody.activeBars(progress, bars)
            val gap = size.width / (bars * 3f)
            val barWidth = (size.width - gap * (bars - 1)) / bars
            for (index in 0 until bars) {
                val heightFactor = 0.2f + ((index % 5) + 1) * 0.12f
                val barHeight = size.height * heightFactor
                val x = index * (barWidth + gap)
                val y = (size.height - barHeight) / 2f
                drawLine(
                    color = if (index < activeBars) Color(0xFF7AE582) else Color(0xFF4A4A4A),
                    start = Offset(x + barWidth / 2f, y),
                    end = Offset(x + barWidth / 2f, y + barHeight),
                    strokeWidth = barWidth,
                )
            }
        }
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = error ?: if (playing) "PLAYING" else "READY",
            color = if (error == null) Color.White else Color(0xFFFF8A80),
            textAlign = TextAlign.Center,
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !playing && toneGenerator != null,
            onClick = { playRequest += 1 },
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = if (playing) "PLAYING" else "PLAY",
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun AudioNote.toToneType(): Int = when (name) {
    "beep" -> ToneGenerator.TONE_PROP_BEEP
    "beep2" -> ToneGenerator.TONE_PROP_BEEP2
    else -> ToneGenerator.TONE_PROP_ACK
}
