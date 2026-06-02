package com.arfipod.wearosplayground.examples

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CubeProjectionTest {
    @Test
    fun cubeHasExpectedTopology() {
        assertEquals(8, CubeProjection.vertices.size)
        assertEquals(12, CubeProjection.edges.size)
    }

    @Test
    fun projectedPointsStayInViewportForDefaultAngles() {
        val points = CubeProjection.project(angleRadians = 0.4f)

        assertEquals(8, points.size)
        points.forEach { point ->
            assertTrue(point.x in 0f..1f)
            assertTrue(point.y in 0f..1f)
            assertTrue(point.depth > 0f)
        }
    }
}
