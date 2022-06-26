package org.dmg.dreamhubfront.formula

import org.dmg.dreamhubfront.formula.Decimal.Companion.NONE
import org.dmg.dreamhubfront.formula.Decimal.Companion.ONE
import org.dmg.dreamhubfront.formula.Decimal.Companion.TWO
import java.math.BigDecimal
import java.math.RoundingMode

open class Decimal(val value: BigDecimal, val type: String) : Comparable<Decimal> {
  open operator fun unaryMinus(): Decimal = Decimal(-value, type)

  open operator fun plus(right: Decimal) = when(right) {
    is NanDecimal -> NanDecimal
    is NoneDecimal -> Decimal(value, combineTypes(right))
    else -> Decimal(value + right.value, combineTypes(right))
  }

  open operator fun minus(right: Decimal) = this + (-right)

  open operator fun times(right: Decimal) = when (right) {
    is NanDecimal -> NanDecimal
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

  override fun toString(): String = when {
    type.isBlank() -> "$value"
    else -> "$value $type"
  }

  companion object {
    val NONE = NoneDecimal("")
    val ZERO = Decimal(BigDecimal.ZERO, "")
    val ONE = Decimal(BigDecimal.ONE, "")
    val TWO = Decimal("2.0".toBigDecimal(), "")
  }
}

class NoneDecimal(type: String) : Decimal(BigDecimal.ZERO, type) {
  override fun unaryMinus() = this

  override operator fun times(right: Decimal) = when (right) {
    is NoneDecimal -> NoneDecimal(combineTypes(right))
    else -> Decimal(right.value, combineTypes(right))
  }

  override fun div(right: Decimal) = Decimal(BigDecimal("1.0")/ right.value, combineTypes(right))

  override fun toString(): String = type
}

object NanDecimal: Decimal(BigDecimal.ZERO, "") {
  override fun unaryMinus() = NanDecimal

  override fun plus(right: Decimal) = NanDecimal

  override fun minus(right: Decimal) = NanDecimal

  override fun times(right: Decimal) = NanDecimal

  override fun div(right: Decimal) = NanDecimal

  override fun compareTo(other: Decimal) = -1

  override fun toString() = "NaN"
}

fun BigDecimal.toDecimal(): Decimal = Decimal(this, "")

fun Int.toDecimal(): Decimal = this.toBigDecimal().toDecimal()

fun Iterable<Decimal>.sum() = fold<Decimal, Decimal>(NONE, { a, b -> a + b })

fun Iterable<Decimal>.sumto() = fold<Decimal, Decimal>(NONE, { a, b -> a / TWO + b })

fun Iterable<Decimal>.prod() = fold(ONE, { a, b -> a * b })

fun String.toDecimalOrNull(): Decimal? =
  takeIf { it.isNotBlank() }
    ?.split(" ")
    ?.takeIf { it.size <= 2 }
    ?.let { parts ->
      val type = if (parts.size == 1) "" else parts[1]
      when (parts[0]) {
        "false" -> Decimal(BigDecimal.ZERO, type)
        "true" -> Decimal(BigDecimal.ONE, type)
        else -> parts[0].toBigDecimalOrNull()?.let { Decimal(it, type) }
      }
    }