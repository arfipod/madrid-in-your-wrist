package com.arfipod.madridinyourwrist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CounterStateTest {
    @Test
    fun defaultCounterStartsAtZero() {
        val counter = CounterState()

        assertEquals(0, counter.value)
        assertEquals("Counter: 0", counter.label())
    }

    @Test
    fun incrementReturnsAndStoresNextValue() {
        val counter = CounterState()

        assertEquals(1, counter.increment())
        assertEquals(2, counter.increment())
        assertEquals("Counter: 2", counter.label())
    }

    @Test
    fun negativeValuesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CounterState(-1)
        }

        assertThrows(IllegalArgumentException::class.java) {
            CounterState.labelFor(-1)
        }
    }
}
