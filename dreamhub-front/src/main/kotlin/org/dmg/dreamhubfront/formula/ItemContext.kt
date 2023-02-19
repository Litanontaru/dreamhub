package org.dmg.dreamhubfront.formula

import org.dmg.dreamhubfront.AbstractItemDto
import org.dmg.dreamhubfront.ItemDto
import org.dmg.dreamhubfront.ValueDto
import org.dmg.dreamhubfront.formula.Formula.toFormula

class ItemContext(private val item: AbstractItemDto) : ((String) -> List<Decimal>) {
  override fun invoke(value: String): List<Decimal> = when {
    value == "_" -> item.attributes.flatMap { it.comboValues() }.map { it.rate() }.filter { it !is NanDecimal }
    value.startsWith("&") -> value
      .substring(1)
      .stripQuotation()
      .let { key ->
        item
          .attributes
          .flatMap { it.comboValues() }
          .mapNotNull { it.item() }
          .partition { it.name.equals(key, ignoreCase = true) || it.extendsItems().any { it.name.equals(key, ignoreCase = true) } }
      }
      .let { (match, notMatch) ->
        match.mapNotNull { it.rate() } + notMatch.flatMap { it.getContext()(value) }
      }
      .filter { it !is NanDecimal }
    else -> value
      .stripQuotation()
      .let { key ->
        item
          .attributes
          .find { it.name.equals(key, ignoreCase = true) }
          ?.comboValues()
          ?.map { it.rate() }
          ?: key.toBigDecimalOrNull()?.toDecimal()?.let { listOf(it) }
          ?: listOf(NanDecimal)
      }
  }
}

private fun String.stripQuotation() = when {
  startsWith("\"") -> substring(1, length - 1)
  else -> this
}

fun AbstractItemDto.rate(): Decimal? = try {
  formula()?.toFormula(getContext())?.calculate()
} catch (e: Exception) {
  NanDecimal
}

fun AbstractItemDto.getContext(): ItemContext = ItemContext(this)

fun AbstractItemDto.formula(): String? =
  when (val item = this) {
    is ItemDto -> item.formula.takeIf { it.isNotBlank() }
    else -> null
  }
    ?: extendsItems().mapNotNull { it.formula() }.firstOrNull()

fun ValueDto.rate() = terminal?.item?.rate() ?: nested?.rate() ?: primitive?.toDecimalOrNull() ?: Decimal.NONE