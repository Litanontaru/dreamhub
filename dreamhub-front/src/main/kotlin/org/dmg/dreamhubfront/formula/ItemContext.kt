package org.dmg.dreamhubfront.formula

import org.dmg.dreamhubfront.*
import org.dmg.dreamhubfront.formula.Formula.toFormula

fun AbstractItemDto.rate(): Decimal? = formula()?.toFormula(getContext())?.calculate()

fun AbstractItemDto.getContext(): Context =
  attributesLegacy()
    .groupBy({ it.name }, { it.values })
    .mapValues { it.value.flatMap { it }.let { value -> value.map { it.rate() } } }
    .let { attributes ->
      attributes + superMetadata().map { it.attributeName }.filter { !attributes.containsKey(it) }.map { it to listOf(NanDecimal) }
    }
    .let { Context(it) }

fun AbstractItemDto.formula(): String? =
  when (val item = this) {
    is ItemDto -> item.formula.takeIf { it.isNotBlank() }
    else -> null
  }
    ?: extendsItems().mapNotNull { it.formula() }.firstOrNull()

fun ValueDto.rate() = terminal?.item?.rate() ?: nested?.rate() ?: primitive?.toDecimalOrNull() ?: Decimal.NONE