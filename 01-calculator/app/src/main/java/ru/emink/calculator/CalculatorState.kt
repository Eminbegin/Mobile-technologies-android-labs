package ru.emink.calculator

import java.math.BigDecimal
import java.math.MathContext

private const val MAX_INPUT_LENGTH = 16

enum class Operation {
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
}

data class CalculatorState(
    val display: String = "0",
    val accumulator: Double? = null,
    val pendingOperation: Operation? = null,
    val startNewEntry: Boolean = true,
) {
    fun inputDigit(digit: Int): CalculatorState {
        require(digit in 0..9)

        val digitText = digit.toString()
        if (startNewEntry) {
            return copy(display = digitText, startNewEntry = false)
        }
        if (display.length >= MAX_INPUT_LENGTH) return this

        val nextDisplay = when (display) {
            "0" -> digitText
            "-0" -> "-$digitText"
            else -> display + digitText
        }
        return copy(display = nextDisplay)
    }

    fun inputDecimal(): CalculatorState {
        if (startNewEntry) return copy(display = "0.", startNewEntry = false)
        if (display == "-") return copy(display = "-0.")
        if ('.' in display || display.length >= MAX_INPUT_LENGTH) return this
        return copy(display = "$display.")
    }

    fun selectOperation(operation: Operation): CalculatorState {
        if (startNewEntry && pendingOperation != null) {
            if (operation == Operation.SUBTRACT) {
                return copy(display = "-", startNewEntry = false)
            }
            return copy(pendingOperation = operation)
        }

        if (display == "-") {
            return copy(display = "0", pendingOperation = operation, startNewEntry = true)
        }

        val currentValue = display.toDouble()
        val nextAccumulator = if (accumulator != null && pendingOperation != null) {
            calculate(accumulator, currentValue, pendingOperation)
        } else {
            currentValue
        }

        return copy(
            display = format(nextAccumulator),
            accumulator = nextAccumulator,
            pendingOperation = operation,
            startNewEntry = true,
        )
    }

    fun calculateResult(): CalculatorState {
        val operation = pendingOperation ?: return copy(startNewEntry = true)
        val left = accumulator ?: return copy(startNewEntry = true)
        val right = if (startNewEntry) left else display.toDouble()
        val result = calculate(left, right, operation)

        return copy(
            display = format(result),
            accumulator = null,
            pendingOperation = null,
            startNewEntry = true,
        )
    }

    fun clearEntry(): CalculatorState {
        return copy(display = "0", startNewEntry = true)
    }

    fun clear(): CalculatorState = CalculatorState()

    private fun calculate(left: Double, right: Double, operation: Operation): Double {
        return when (operation) {
            Operation.ADD -> left + right
            Operation.SUBTRACT -> left - right
            Operation.MULTIPLY -> left * right
            Operation.DIVIDE -> left / right
        }
    }

    private fun format(value: Double): String {
        if (!value.isFinite()) return value.toString()
        return BigDecimal.valueOf(value)
            .round(MathContext.DECIMAL64)
            .stripTrailingZeros()
            .toPlainString()
    }
}
