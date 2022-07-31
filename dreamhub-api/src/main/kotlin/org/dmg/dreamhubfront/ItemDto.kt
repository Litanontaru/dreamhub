package org.dmg.dreamhubfront

open class ItemName {
  var id: Long = 0
  var name: String = ""

  override fun equals(other: Any?): Boolean = (this === other) || (other is ItemName && id == other.id)

  override fun hashCode(): Int = id.hashCode()
}

open class ItemListDto : ItemName() {
  var path: String = ""
  var settingId: Long = 0
  var rank: Int = 0
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

  fun comboAllowedExtensions(): List<ItemName> = (mainAllowedExtensions() + superAllowedExtensions()).distinct()

  fun superAllowedExtensions() = extendsItems().flatMap { it.comboAllowedExtensions() }.distinct()

  open fun mainGroups() = listOf<String>()

  fun comboGroups(): List<String> = (mainGroups() + (extendsItems().flatMap { it.comboGroups() })).distinct()

  open fun mainMetadata(): Sequence<MetadataDto> = emptySequence()

  fun comboMetadata(): Sequence<MetadataDto> = superMetadata() + mainMetadata()

  fun comboMetadata(attributeName: String): MetadataDto? =
    mainMetadata().find { it.attributeName == attributeName }
      ?: superMetadata(attributeName)

  fun superMetadata(): Sequence<MetadataDto> =
    extendsItems()
      .flatMap { it.comboMetadata() }
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

  open operator fun not(): AbstractItemDto = apply { inherit() }

  fun inherit() {
    extendsItems()
      .toList()
      .reversed()
      .fold(this) { acc, i -> acc += !i; acc }
    attributes
      .asSequence()
      .flatMap { it.values.asSequence() }
      .forEach { it.item()?.not() }
  }

  operator fun plusAssign(right: AbstractItemDto) {
    comboMetadata().forEach { meta ->
      val l = attributes.find { it.name == meta.attributeName }
      val r = right.attributes.find { it.name == meta.attributeName }
      when {
        r == null -> {} //do nothing
        l == null -> attributes.add(AttributeDto().also {
          it.name = meta.attributeName
          it.inherited = r.comboValues().toMutableList()
        })
        meta.isSingle -> r.comboValues().takeIf { it.isNotEmpty() }?.let { l += it[0] }
        else -> l += r.comboValues()
      }
    }
  }
}

class ItemDto : AbstractItemDto() {
  var path: String = ""
  var settingId: Long = 0
  var allowedExtensions: MutableList<ItemName> = mutableListOf()
  var groups: String = ""
  var metadata: MutableList<MetadataDto> = mutableListOf()
  var formula: String = ""
  var isType: Boolean = false
  var isFinal: Boolean = false
  var description: String = ""
  var rank: Int = 0

  override fun mainAllowedExtensions() = allowedExtensions

  override fun mainGroups() = groups.split(",").map { it.trim() }.filter { it.isNotBlank() }

  override fun mainMetadata(): Sequence<MetadataDto> = metadata.asSequence()

  override operator fun not(): ItemDto = apply { inherit() }
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

  var inherited: MutableList<ValueDto>? = null

  fun comboValues(): List<ValueDto> =
    inherited
      ?.shadowedBy(values)
      ?.let { it.first + it.second }
      ?: values.toList()

  fun showValues() =
    inherited
      ?.shadowedBy(values)
      ?: (values to mutableListOf())

  operator fun plusAssign(right: ValueDto) {
    if (comboValues().isEmpty()) {
      inherited = mutableListOf(right)
    } else {
      val left = comboValues()[0]
      when {
        left.primitive != null -> {} //do nothing
        right.primitive != null -> inherited = mutableListOf(right)
        else -> !left.item()!! += !right.item()!!
      }
    }
  }

  operator fun plusAssign(values: List<ValueDto>) {
    inherited
      ?.let {  inherited = values.shadowedBy(it).let { (a, b) -> a + b }.toMutableList() }
      ?: run { inherited = values.toMutableList() }
  }
}

class ValueDto {
  var nested: AbstractItemDto? = null
  var terminal: RefDto? = null
  var primitive: String? = null

  fun item(): AbstractItemDto? = terminal?.item ?: nested
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

fun ItemDto.isAbstract(): Boolean = isAbstract(setOf())

fun ItemDto.isAbstract(definedAttributeNames: Set<String>): Boolean {
  val aFieldIsNotDefined = !metadata.filter { it.isRequired }.all { definedAttributeNames.contains(it.attributeName) }

  val defined = definedAttributeNames + attributes.map { it.name }
  val anExtendsIsAbstract = extendsItems().any { it.isAbstract(defined) }

  return aFieldIsNotDefined || anExtendsIsAbstract
}

fun List<ValueDto>.shadowedBy(values: MutableList<ValueDto>): Pair<MutableList<ValueDto>, MutableList<ValueDto>> {
  val names = values.mapNotNull { it.item()?.name }.toSet()
  val groups = values.mapNotNull { it.item()?.comboGroups() }.flatten().toSet()

  val (shadowed, other) = this.partition { it.item()?.let { (it.name in names) || (it.comboGroups().any { it in groups }) } ?: false }

  if (!shadowed.isEmpty()) {
    val byName = shadowed.associateBy { it.item()!!.name }
    for (it in values) {
      byName[it.item()!!.name]?.let { shadow ->
        it.item()!! += shadow.item()!!
      }
    }
  }
  return values to other.toMutableList()
}