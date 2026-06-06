package com.arfipod.madridinyourwrist.examples

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FlappyBirdLogicTest {
    @Test
    fun flapStartsTheGameAndAddsLift() {
        val snapshot = FlappyBirdLogic.flap(FlappyBirdLogic.initial())

        assertTrue(snapshot.running)
        assertFalse(snapshot.crashed)
        assertTrue(snapshot.velocity < 0f)
    }

    @Test
    fun tickAdvancesPipeWhileRunning() {
        val snapshot = FlappyBirdLogic.flap(FlappyBirdLogic.initial())
        val next = FlappyBirdLogic.tick(snapshot, deltaSeconds = 0.1f)

        assertTrue(next.pipeX < snapshot.pipeX)
    }

    @Test
    fun pipeResetIncrementsScore() {
        val snapshot = FlappyBirdLogic.initial().copy(
            running = true,
            pipeX = -FlappyBirdLogic.PIPE_WIDTH - 0.01f,
        )

        val next = FlappyBirdLogic.tick(snapshot, deltaSeconds = 0.1f)

        assertEquals(1, next.score)
        assertTrue(next.pipeX > 1f)
    }

    @Test
    fun collisionDetectsPipeOutsideGap() {
        val snapshot = FlappyBirdLogic.initial().copy(
            pipeX = FlappyBirdLogic.BIRD_X,
            gapY = 0.75f,
            birdY = 0.3f,
        )

        assertTrue(FlappyBirdLogic.hasCollision(snapshot))
    }

    @Test
    fun negativeDeltaIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            FlappyBirdLogic.tick(FlappyBirdLogic.initial(), deltaSeconds = -0.1f)
        }
    }
}
