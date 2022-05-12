package org.dmg.dreamhubfront.formula

import java.math.BigDecimal
import java.math.RoundingMode

open class Decimal(val value: BigDecimal, val type: String) : Comparable<Decimal> {
  open operator fun unaryMinus(): Decimal = Decimal(-value, type)
  open operator fun plus(right: Decimal) = Decimal(value + right.value, combineTypes(right))
  open operator fun minus(right: Decimal) = Decimal(value - right.value, combineTypes(right))
  open operator fun times(right: Decimal) = when (right) {
    is NoneDecimal -> Decimal(value, combineTypes(right))
    else -> Decimal(value * right.value, combineTypes(right))
  }
  open operator fun div(right: Decimal) = Decimal(BigDecimal("1.0") * value / right.value, combineTypes(right))

  fun combineTypes(right: Decimal) = type.takeIf { it.isNotBlank() } ?: right.type

  override operator fun compareTo(other: Decimal) = when (type) {
    other.type -> value.compareTo(other.value)
    else -> throw IllegalStateException("Cannot compare $type and ${other.type}")
  }

  fun setScale(newScale: Int, roundingMode: RoundingMode): Decimal = Decimal(value.setScale(newScale, roundingMode), type)

  override fun toString(): String = "$value $type"

  companion object {
    val NONE = NoneDecimal("")
    val ZERO = Decimal(BigDecimal.ZERO, "")
    val ONE = Decimal(BigDecimal.ONE, "")
  }
}

class NoneDecimal(type: String) : Decimal(BigDecimal.ZERO, type) {
  override fun unaryMinus() = this

  override operator fun times(right: Decimal) = when (right) {
    is NoneDecimal -> NoneDecimal(combineTypes(right))
    else -> Decimal(right.value, combineTypes(right))
  }

  override fun div(right: Decimal) = Decimal(BigDecimal("1.0")/ right.value, combineTypes(right))
}

fun BigDecimal.toDecimal(): Decimal = Decimal(this, "")

fun Int.toDecimal(): Decimal = this.toBigDecimal().toDecimal()

fun Iterable<Decimal>.sum() = fold<Decimal, Decimal>(Decimal.NONE, { a, b -> a + b })