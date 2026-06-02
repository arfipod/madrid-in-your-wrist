package com.arfipod.wearosplayground.examples

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay

data class FlappyBirdSnapshot(
    val birdY: Float,
    val velocity: Float,
    val pipeX: Float,
    val gapY: Float,
    val score: Int,
    val running: Boolean,
    val crashed: Boolean,
)

object FlappyBirdLogic {
    const val BIRD_X = 0.25f
    const val BIRD_RADIUS = 0.055f
    const val PIPE_WIDTH = 0.18f
    const val GAP_HEIGHT = 0.34f

    private const val GRAVITY = 1.24f
    private const val FLAP_VELOCITY = -0.52f
    private const val PIPE_SPEED = 0.32f

    fun initial(): FlappyBirdSnapshot = FlappyBirdSnapshot(
        birdY = 0.45f,
        velocity = 0f,
        pipeX = 1.05f,
        gapY = 0.48f,
        score = 0,
        running = false,
        crashed = false,
    )

    fun flap(snapshot: FlappyBirdSnapshot): FlappyBirdSnapshot {
        val base = if (snapshot.crashed) initial() else snapshot
        return base.copy(velocity = FLAP_VELOCITY, running = true, crashed = false)
    }

    fun tick(snapshot: FlappyBirdSnapshot, deltaSeconds: Float): FlappyBirdSnapshot {
        require(deltaSeconds >= 0f) { "Delta must be non-negative." }
        if (!snapshot.running || snapshot.crashed) return snapshot

        val velocity = (snapshot.velocity + GRAVITY * deltaSeconds).coerceIn(-1.2f, 1.2f)
        val birdY = snapshot.birdY + velocity * deltaSeconds
        var pipeX = snapshot.pipeX - PIPE_SPEED * deltaSeconds
        var gapY = snapshot.gapY
        var score = snapshot.score

        if (pipeX + PIPE_WIDTH < 0f) {
            score += 1
            pipeX = 1.05f
            gapY = gapForScore(score)
        }

        val candidate = snapshot.copy(
            birdY = birdY,
            velocity = velocity,
            pipeX = pipeX,
            gapY = gapY,
            score = score,
        )

        return if (hasCollision(candidate)) {
            candidate.copy(running = false, crashed = true)
        } else {
            candidate
        }
    }

    fun hasCollision(snapshot: FlappyBirdSnapshot): Boolean {
        if (snapshot.birdY - BIRD_RADIUS <= 0f || snapshot.birdY + BIRD_RADIUS >= 1f) {
            return true
        }

        val overlapsPipe = BIRD_X + BIRD_RADIUS > snapshot.pipeX &&
            BIRD_X - BIRD_RADIUS < snapshot.pipeX + PIPE_WIDTH
        if (!overlapsPipe) return false

        val topGap = snapshot.gapY - GAP_HEIGHT / 2f
        val bottomGap = snapshot.gapY + GAP_HEIGHT / 2f
        return snapshot.birdY - BIRD_RADIUS < topGap ||
            snapshot.birdY + BIRD_RADIUS > bottomGap
    }

    fun gapForScore(score: Int): Float {
        require(score >= 0) { "Score must be non-negative." }
        return 0.32f + ((score * 37) % 37) / 100f
    }
}

@Composable
fun FlappyBirdExample(
    onBack: () -> Unit,
    onEvent: (String) -> Unit,
) {
    var snapshot by remember { mutableStateOf(FlappyBirdLogic.initial()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            snapshot = FlappyBirdLogic.tick(snapshot, deltaSeconds = 0.05f)
        }
    }

    ExampleChrome(title = ExampleKind.FLAPPY.title, onBack = onBack) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable {
                    val wasCrashed = snapshot.crashed
                    snapshot = FlappyBirdLogic.flap(snapshot)
                    onEvent(
                        if (wasCrashed) {
                            "Flappy Bird restarted"
                        } else {
                            "Flappy Bird flap score=${snapshot.score}"
                        }
                    )
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sky = Color(0xFF06303D)
                val pipe = Color(0xFF4DD178)
                val bird = Color(0xFFFFD166)
                val ground = Color(0xFF303030)

                drawRect(color = sky, size = size)
                drawRect(
                    color = ground,
                    topLeft = Offset(0f, size.height * 0.92f),
                    size = Size(size.width, size.height * 0.08f),
                )

                val pipeWidthPx = size.width * FlappyBirdLogic.PIPE_WIDTH
                val pipeXPx = size.width * snapshot.pipeX
                val gapCenterPx = size.height * snapshot.gapY
                val gapHeightPx = size.height * FlappyBirdLogic.GAP_HEIGHT
                val topHeight = (gapCenterPx - gapHeightPx / 2f).coerceAtLeast(0f)
                val bottomY = (gapCenterPx + gapHeightPx / 2f).coerceAtMost(size.height)

                drawRect(
                    color = pipe,
                    topLeft = Offset(pipeXPx, 0f),
                    size = Size(pipeWidthPx, topHeight),
                )
                drawRect(
                    color = pipe,
                    topLeft = Offset(pipeXPx, bottomY),
                    size = Size(pipeWidthPx, size.height - bottomY),
                )

                drawCircle(
                    color = bird,
                    radius = size.minDimension * FlappyBirdLogic.BIRD_RADIUS,
                    center = Offset(
                        x = size.width * FlappyBirdLogic.BIRD_X,
                        y = size.height * snapshot.birdY,
                    ),
                )
            }
            Text(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                text = "Score ${snapshot.score}",
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
            if (snapshot.crashed) {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    text = "CRASH",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
