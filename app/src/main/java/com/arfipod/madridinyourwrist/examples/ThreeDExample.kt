package com.arfipod.madridinyourwrist.examples

import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

data class Point3D(val x: Float, val y: Float, val z: Float)

data class ProjectedPoint(val x: Float, val y: Float, val depth: Float)

object CubeProjection {
    val vertices = listOf(
        Point3D(-1f, -1f, -1f),
        Point3D(1f, -1f, -1f),
        Point3D(1f, 1f, -1f),
        Point3D(-1f, 1f, -1f),
        Point3D(-1f, -1f, 1f),
        Point3D(1f, -1f, 1f),
        Point3D(1f, 1f, 1f),
        Point3D(-1f, 1f, 1f),
    )

    val edges = listOf(
        0 to 1,
        1 to 2,
        2 to 3,
        3 to 0,
        4 to 5,
        5 to 6,
        6 to 7,
        7 to 4,
        0 to 4,
        1 to 5,
        2 to 6,
        3 to 7,
    )

    fun project(angleRadians: Float): List<ProjectedPoint> {
        val cosY = cos(angleRadians)
        val sinY = sin(angleRadians)
        val cosX = cos(angleRadians * 0.7f)
        val sinX = sin(angleRadians * 0.7f)

        return vertices.map { point ->
            val rotatedX = point.x * cosY - point.z * sinY
            val rotatedZ = point.x * sinY + point.z * cosY
            val rotatedY = point.y * cosX - rotatedZ * sinX
            val depth = point.y * sinX + rotatedZ * cosX + 4f
            val perspective = 0.95f / depth

            ProjectedPoint(
                x = 0.5f + rotatedX * perspective,
                y = 0.5f + rotatedY * perspective,
                depth = depth,
            )
        }
    }
}

@Composable
fun ThreeDExample(
    onBack: () -> Unit,
    onEvent: (String) -> Unit,
) {
    var angle by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        onEvent("3D cube example started")
        while (true) {
            delay(66)
            angle += 0.08f
        }
    }

    ExampleChrome(title = ExampleKind.CUBE_3D.title, onBack = onBack) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .fillMaxSize(),
        ) {
            drawRect(color = Color(0xFF101820), size = size)

            val projected = CubeProjection.project(angle)
            CubeProjection.edges.forEachIndexed { index, edge ->
                val start = projected[edge.first]
                val end = projected[edge.second]
                val tint = if (index % 2 == 0) {
                    Color(0xFF75E6DA)
                } else {
                    Color(0xFFFFC857)
                }
                drawLine(
                    color = tint,
                    start = Offset(start.x * size.width, start.y * size.height),
                    end = Offset(end.x * size.width, end.y * size.height),
                    strokeWidth = 3.dp.toPx(),
                )
            }

            projected.forEach { point ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.55f),
                    radius = (7f / point.depth).coerceAtLeast(1.5f),
                    center = Offset(point.x * size.width, point.y * size.height),
                )
            }
        }
    }
}
