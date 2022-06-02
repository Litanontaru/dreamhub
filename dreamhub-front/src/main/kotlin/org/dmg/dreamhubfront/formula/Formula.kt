package org.dmg.dreamhubfront.formula

interface FNode {
  fun calculate(): Decimal = throw UnsupportedOperationException()

  operator fun unaryMinus(): FNode = FNegate(this)
  operator fun plus(right: FNode): FNode = FSum(listOf(this, right))
  operator fun minus(right: FNode): FNode = this + (-right)
  operator fun times(right: FNode): FNode = FTimes(listOf(this, right))
  operator fun div(right: FNode): FNode = FDiv(this, right)

  infix fun and(right: FNode): FNode = FNodeList(listOf(this, right))
  fun min(): FNode = this
  fun max(): FNode = this
  fun sum(): FNode = this
  fun count(): FNode = object : FNode {
    override fun calculate(): Decimal = Decimal.ONE
  }
}

class FConst(private val value: Decimal) : FNode {
  override fun calculate() = value
}

class FVar(private val value: () -> List<Decimal>) : FAbstractNodeList {
  override fun calculate() = value().first()

  override fun values(): List<FNode> = value().map { FConst(it) }
}

class FNegate(private val value: FNode) : FNode {
  override fun calculate() = -value.calculate()
}

class FSum(private val values: List<FNode>) : FNode {
  override fun calculate(): Decimal = values.fold(Decimal.NONE as Decimal) { acc, r -> acc + r.calculate() }

  override fun plus(right: FNode) = FSum(values + right)

  override fun times(right: FNode) = FSum(values.dropLast(1) + (values.last() * right))

  override fun div(right: FNode) = FSum(values.dropLast(1) + (values.last() / right))
}

class FTimes(private val values: List<FNode>) : FNode {
  override fun calculate(): Decimal = values.fold(Decimal.NONE as Decimal) { acc, r -> acc * r.calculate() }

  override fun times(right: FNode): FNode = FTimes(values + right)
}

class FDiv(private val left: FNode, private val right: FNode) : FNode {
  override fun calculate(): Decimal = left.calculate() / right.calculate()
}

class FNone(val type: String) : FNode {
  override fun calculate() = NoneDecimal(type)

  override fun unaryMinus(): FNode = this

  override fun plus(right: FNode): FNode = throw UnsupportedOperationException()

  override fun minus(right: FNode): FNode = throw UnsupportedOperationException()

  override fun times(right: FNode): FNode = when (type) {
    "" -> right
    else -> super.times(right)
  }

  override fun div(right: FNode): FNode = throw UnsupportedOperationException()
}

interface FAbstractNodeList : FNode {
  fun values(): List<FNode>

  override fun min(): FNode = object : FNode {
    override fun calculate(): Decimal = values().map { it.calculate() }.min()
  }

  override fun max(): FNode = object : FNode {
    override fun calculate(): Decimal = values().map { it.calculate() }.max()
  }

  override fun sum(): FNode = object : FNode {
    override fun calculate(): Decimal = values().map { it.calculate() }.sum()
  }

  override fun count(): FNode = object : FNode {
    override fun calculate(): Decimal = values().size.toDecimal()
  }
}

class FNodeList(val values: List<FNode>) : FAbstractNodeList {
  override fun values(): List<FNode> = values

  override fun and(right: FNode): FNode = FNodeList(values + right)
}

object Formula {
  private val PATTERN = "[+-]?([0-9]*[.])?[0-9]+|\\(|\\)|,|\\+|-|\\*|/|&?([A-Za-z_])+|&?([ЁёА-я_])+|&?\"([A-Za-z _])+\"|&?\"([ЁёА-я _])+\"".toRegex()

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
          ")" -> return result

          "+" -> action = { a, b -> a + b }
          "-" -> action = { a, b -> a - b }
          "*" -> action = times
          "/" -> action = { a, b -> a / b }

          "," -> action = { a, b -> a and b }

          "MIN" -> result = action(result, parse(1).min())
          "MAX" -> result = action(result, parse(1).max())
          "SUM" -> result = action(result, parse(1).sum())
          "COUNT" -> result = action(result, parse(1).count())
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
          return result
        }
      }
    }

    return parse()
  }

  fun String?.toFormula(context: (String) -> List<Decimal>) = this
    ?.let { Formula(it, context) }
    ?: FNone("")
}

class Context(map: Map<String, List<Decimal>>) : ((String) -> List<Decimal>) {
  private val attributes = map.mapKeys { it.key.uppercase() }

  override fun invoke(value: String): List<Decimal> =
    when {
      value == "_" -> attributes.values.flatten()
      value.startsWith("&") ->
        value
          .substring(1)
          .stripQuotation()
          .let { type -> attributes.values.flatMap { it }.filter { it.type == type } }
      else ->
        value
          .stripQuotation()
          .let {
            attributes[it]
              ?: it.toBigDecimalOrNull()?.toDecimal()?.let { listOf(it) }
              ?: listOf(NoneDecimal(it))
          }
    }

  private fun String.stripQuotation() = when {
    startsWith("\"") -> substring(1, length - 1)
    else -> this
  }
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