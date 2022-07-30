package org.dmg.dreamhubfront.formula

interface FNode {
  fun calculate(): Decimal = throw UnsupportedOperationException()

  fun close(): FNode = this

  operator fun unaryMinus(): FNode = FNegate(this)
  operator fun plus(right: FNode): FNode = FSum(listOf(this, right))
  operator fun minus(right: FNode): FNode = this + (-right)
  operator fun times(right: FNode): FNode = FTimes(listOf(this, right))
  operator fun div(right: FNode): FNode = FDiv(this, right)
  fun orZero(): FNode = FOrZero(this)

  infix fun and(right: FNode): FNode = FNodeList(listOf(this, right))
  fun min(): FNode = this
  fun max(): FNode = this
  fun sum(): FNode = this
  fun count(): FNode = object : FNode {
    override fun calculate(): Decimal = Decimal.ONE
  }

  fun prod(): FNode = this
  fun sumto(): FNode = this
}

class FCalculator(private val calculator: () -> Decimal) : FNode {
  override fun calculate(): Decimal = calculator()
}

class FConst(private val value: Decimal) : FNode {
  override fun calculate() = value
}

class FVar(private val value: () -> List<Decimal>) : FAbstractNodeList {
  override fun calculate() = value().firstOrNull() ?: NanDecimal

  override fun values(): List<FNode> = value().map { FConst(it) }
}

class FNegate(private val value: FNode) : FNode {
  override fun calculate() = -value.calculate()
}

open class FClosedSum(private val values: List<FNode>) : FNode {
  override fun calculate(): Decimal = values.fold(Decimal.NONE as Decimal) { acc, r -> acc + r.calculate() }
}

class FSum(private val values: List<FNode>) : FClosedSum(values) {
  override fun calculate(): Decimal = values.fold(Decimal.NONE as Decimal) { acc, r -> acc + r.calculate() }

  override fun close(): FNode = FClosedSum(values)

  override fun orZero(): FNode = FSum(values.dropLast(1) + values.last().orZero())

  override fun plus(right: FNode) = FSum(values + right)

  override fun times(right: FNode) = FSum(values.dropLast(1) + (values.last() * right))

  override fun div(right: FNode) = FSum(values.dropLast(1) + (values.last() / right))
}

class FTimes(private val values: List<FNode>) : FNode {
  override fun calculate(): Decimal = values.fold(Decimal.NONE as Decimal) { acc, r -> acc * r.calculate() }

  override fun orZero(): FNode = FTimes(values.dropLast(1) + values.last().orZero())

  override fun times(right: FNode): FNode = FTimes(values + right)
}

class FDiv(private val left: FNode, private val right: FNode) : FNode {
  override fun orZero(): FNode = FDiv(left, right.orZero())

  override fun calculate(): Decimal = left.calculate() / right.calculate()
}

class FNone(val type: String) : FNode {
  override fun calculate() = NoneDecimal(type)

  override fun unaryMinus(): FNode = this

  override fun plus(right: FNode): FNode = right + this

  override fun minus(right: FNode): FNode = -right + this

  override fun times(right: FNode): FNode = when (type) {
    "" -> right
    else -> super.times(right)
  }

  override fun div(right: FNode): FNode = throw UnsupportedOperationException()
}

interface FAbstractNodeList : FNode {
  fun values(): List<FNode>

  override fun min(): FNode = FCalculator { values().map { it.calculate() }.min() }

  override fun max(): FNode = FCalculator { values().map { it.calculate() }.max() }

  override fun sum(): FNode = FCalculator { values().map { it.calculate() }.sum() }

  override fun count(): FNode = FCalculator { values().size.toDecimal() }

  override fun prod(): FNode = FCalculator { values().map { it.calculate() }.prod() }

  override fun sumto(): FNode = FCalculator { values().map { it.calculate() }.sorted().sumto() }
}

class FNodeList(val values: List<FNode>) : FAbstractNodeList {
  override fun values(): List<FNode> = values

  override fun and(right: FNode): FNode = FNodeList(values + right)
}

class FOrZero(private val inner: FNode) : FNode {
  override fun calculate(): Decimal = when (val value = inner.calculate()) {
    is NanDecimal -> Decimal.ZERO
    else -> value
  }
}

object Formula {
  private val PATTERN = "[+-]?([0-9]*[.])?[0-9]+|\\(|\\)|,|\\?|\\+|-|\\*|/|&?([A-Za-z_])+|&?([ЁёА-я_])+|&?\"([A-Za-z _])+\"|&?\"([ЁёА-я _])+\"".toRegex()

  operator fun invoke(value: String, context: (String) -> List<Decimal>): FNode {
    if (value.isBlank()) {
      return FNone("")
    }

    val parts = PATTERN.findAll(value.uppercase()).toList()
    if (parts.isEmpty()) {
      return FNone("")
    }
    var i = 0

    val times: (FNode, FNode) -> FNode = { a, b -> a * b }

    fun parse(shift: Int = 0): FNode {
      i = i + shift
      var result: FNode = FNone("")
      var action = times

      while (true) {
        val part = parts[i].value
        i++

        when (part) {
          ")" -> return result.close()

          "+" -> action = { a, b -> a + b }
          "-" -> action = { a, b -> a - b }
          "*" -> action = times
          "/" -> action = { a, b -> a / b }

          "," -> action = { a, b -> a and b }
          "?" -> result = result.orZero()

          "MIN" -> result = action(result, parse(1).min())
          "MAX" -> result = action(result, parse(1).max())
          "SUM" -> result = action(result, parse(1).sum())
          "COUNT" -> result = action(result, parse(1).count())
          "PROD" -> result = action(result, parse(1).prod())
          "SUMTO" -> result = action(result, parse(1).sumto())
          "(" -> result = action(result, parse())
          else -> result = when {
            part.startsWith("&") -> FVar { context(part) }
            else -> action(result, FVar { context(part) })
          }
        }

        when (part) {
          "+", "-", "*", "/", ",", "&" -> {}
          else -> action = times
        }

        if (i == parts.size) {
          return result.close()
        }
      }
    }

    return parse()
  }

  fun String?.toFormula(context: (String) -> List<Decimal>) = this
    ?.let { Formula(it, context) }
    ?: FNone("")
}

/*
fun test() {
  val x = { BigDecimal("2.0").toDecimal() }
  val a = { Decimal(BigDecimal("3.0"), "АЭ") }
  val b = { Decimal(BigDecimal("4.0"), "АЭ") }

  listOf(
    "1",
    "1 + x",
    "x * x",
    "1 + X / 2",
    "1 АЭ",
    "sum(&АЭ)",
    "min(&АЭ) max(&АЭ)",
    "3 count(&АЭ) ПЭ"
  )
    .forEach {
      val formula = it.toFormula(Context(mapOf("x" to x, "a" to a, "b" to b)))
      val decimal = formula.calculate()
      println(it + " = " + decimal)
    }
}*/