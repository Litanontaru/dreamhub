package org.dmg.dreamhubfront

import org.dmg.dreamhubfront.StandardTypes.TYPE

open class ItemName {
  var id: Long = 0
  var name: String = ""

  override fun equals(other: Any?): Boolean = (this === other) || (other is ItemName && id == other.id)

  override fun hashCode(): Int = id.hashCode()
}

open class ItemListDto : ItemName() {
  var path: String = ""
  var settingId: Long = 0
}

class TypeDto : ItemListDto() {
  var superTypeIds: List<Long> = listOf()
}

open class AbstractItemDto : ItemName() {
  var nestedId: Long = -1
  var extends: MutableList<RefDto> = mutableListOf()
  var attributes: MutableList<AttributeDto> = mutableListOf()

  fun extendsItems() = extends.asSequence().mapNotNull { it.item }

  fun comboName(): String = name.takeIf { it.isNotBlank() } ?: superName()

  fun superName(): String = extendsItems().map { it.comboName() }.find { it.isNotBlank() } ?: ""

  open fun mainAllowedExtensions(): List<ItemName> = listOf()

  fun comboAllowedExtensions(): List<ItemName> = (listOf(TYPE) + mainAllowedExtensions() + superAllowedExtensions()).distinct()

  fun superAllowedExtensions() = extendsItems().flatMap { it.comboAllowedExtensions() }.distinct()

  open fun mainMetadata(): Sequence<MetadataDto> = emptySequence()

  fun comboMetadata(attributeName: String): MetadataDto? =
    mainMetadata().find { it.attributeName == attributeName }
      ?: superMetadata(attributeName)

  fun superMetadata(): Sequence<MetadataDto> =
    extendsItems()
      .flatMap { it.superMetadata() + it.metadata }
      .distinctBy { it.attributeName }

  fun superMetadata(attributeName: String): MetadataDto? =
    extendsItems()
      .mapNotNull {
        it
          .metadata
          .firstOrNull { it.attributeName == attributeName }
          ?: it.superMetadata(attributeName)
      }
      .firstOrNull()
}

class ItemDto : AbstractItemDto() {
  var path: String = ""
  var settingId: Long = 0
  var allowedExtensions: MutableList<ItemName> = mutableListOf()
  var metadata: MutableList<MetadataDto> = mutableListOf()
  var formula: String = ""
  var isType: Boolean = false
  var isFinal: Boolean = false

  override fun mainAllowedExtensions() = allowedExtensions

  override fun mainMetadata(): Sequence<MetadataDto> = metadata.asSequence()
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
  var allowReference: Boolean = false
  var isRequired: Boolean = false

  fun toItemName() = ItemName().also { it.id = typeId }
}

class AttributeDto {
  var name: String = ""
  var values: MutableList<ValueDto> = mutableListOf()
}

class ValueDto {
  var nested: AbstractItemDto? = null
  var terminal: RefDto? = null
  var primitive: String? = null

  var itemOwnerId: Long = 0
}

object StandardTypes {
  val NOTHING = ItemName().apply {
    id = -1
    name = "Не указан"
  }

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

  val ALL = listOf(NOTHING, STRING, POSITIVE, INT, DECIMAL, BOOLEAN, TYPE)
}

@Deprecated("old and wrong")
fun AbstractItemDto.attributesLegacy(): Sequence<AttributeDto> = attributes.asSequence() + extendsItems().flatMap { it.attributes }

fun ItemDto.isAbstract(): Boolean = isAbstract(setOf())

fun ItemDto.isAbstract(attributeNames: Set<String>): Boolean =
  metadata.filter { it.isRequired }.any { !attributeNames.contains(it.attributeName) } ||
      (attributeNames + attributes.map { it.name })
        .let { attributes -> extendsItems().any { it.isAbstract(attributes) } }

fun AbstractItemDto.nonFinalExtends() = extendsItems().filter { !it.isFinal }