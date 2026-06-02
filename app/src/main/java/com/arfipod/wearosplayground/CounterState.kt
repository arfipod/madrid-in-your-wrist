package com.arfipod.wearosplayground

class CounterState(initialValue: Int = 0) {
    var value: Int = initialValue
        private set

    init {
        require(initialValue >= 0) { "Initial counter value must be non-negative." }
    }

    fun increment(): Int {
        value += 1
        return value
    }

    fun label(): String = labelFor(value)

    companion object {
        fun labelFor(value: Int): String {
            require(value >= 0) { "Counter value must be non-negative." }
            return "Counter: $value"
        }
    }
}
