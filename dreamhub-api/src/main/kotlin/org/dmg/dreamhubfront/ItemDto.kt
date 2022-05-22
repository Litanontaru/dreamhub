package org.dmg.dreamhubfront

import org.dmg.dreamhubfront.StandardTypes.TYPE

open class ItemName {
  var id: Long = 0
  var name: String = ""
}

open class ItemListDto : ItemName() {
  var path: String = ""
  var settingId: Long = 0
}

class TypeDto : ItemListDto() {
  var subtypeIds: List<Long> = listOf()
}

abstract class AbstractItemDto : ItemName() {
  var extends: MutableList<RefDto> = mutableListOf()
  var attributes: MutableList<AttributeDto> = mutableListOf()
  var rate: String = ""

  abstract fun nestedId(): Long
}

class ItemDto : AbstractItemDto() {
  var path: String = ""
  var settingId: Long = 0
  var nextNestedId: Long = 0
  var allowedExtensions: MutableList<ItemName> = mutableListOf()
  var metadata: MutableList<MetadataDto> = mutableListOf()
  var formula: String = ""
  var isType: Boolean = false

  override fun nestedId(): Long = -1
}

class NestedItemDto : AbstractItemDto() {
  var nestedId: Long = 0

  override fun nestedId(): Long = nestedId
}

class RefDto {
  var id: Long = 0
  var item: ItemDto? = null
}

class MetadataDto {
  var attributeName: String = ""
  var typeId: Long = -1
  var isSingle: Boolean = false
  var allowCreate: Boolean = false

  fun toItemName() = ItemName().also { it.id = typeId }
}

class AttributeDto {
  var name: String = ""
  var values: MutableList<ValueDto> = mutableListOf()
}

class ValueDto {
  var nested: NestedItemDto? = null
  var terminal: RefDto? = null
  var primitive: String? = null

  var itemOwnerId: Long = 0
}

object StandardTypes {
  val STRING = ItemName().apply {
    id = -2
    name = "Строка"
  }

  val POSITIVE = ItemName().apply {
    id = -3
    name = "Позитивное число"
  }

  val INT = ItemName().apply {
    id = -4
    name = "Целое число"
  }

  val DECIMAL = ItemName().apply {
    id = -5
    name = "Дробное число"
  }

  val BOOLEAN = ItemName().apply {
    id = -6
    name = "Бинарное"
  }

  val TYPE = ItemName().apply {
    id = -7
    name = "Тип"
  }
}

fun AbstractItemDto.getMetadata(attributeName: String): MetadataDto? =
  extends
    .asSequence()
    .mapNotNull { it.item }
    .mapNotNull {
      it
        .metadata
        .firstOrNull { it.attributeName == attributeName }
        ?: it.getMetadata(attributeName)
    }
    .firstOrNull()

fun AbstractItemDto.getMetadata(): Sequence<MetadataDto> =
  extends
    .asSequence()
    .mapNotNull { it.item }
    .flatMap { it.getMetadata() + it.metadata }

fun AbstractItemDto.allowedExtensions(): List<ItemName> = (listOf(TYPE) + extends.mapNotNull { it.item }.flatMap { it.allowedExtensions() }).distinct()