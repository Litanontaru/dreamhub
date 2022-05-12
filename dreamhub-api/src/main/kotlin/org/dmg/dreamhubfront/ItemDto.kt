package org.dmg.dreamhubfront

open class ItemName {
  var id: Long = 0
  var name: String = ""
}

open class ItemListDto: ItemName() {
  var path: String = ""
  var settingId: Long = 0
}

class TypeDto: ItemListDto() {
  var subtypeIds: List<Long> = listOf()
}

open class AbstractItemDto: ItemName() {
  var extends: MutableList<RefDto> = mutableListOf()
  var attributes: MutableList<AttributeDto> = mutableListOf()
  var superFormula: String = ""
  var rate: String = ""
}

class ItemDto: AbstractItemDto() {
  var path: String = ""
  var settingId: Long = 0
  var nextNestedId: Long = 0
  var allowedExtensions: MutableList<ItemName> = mutableListOf()
  var formula: String = ""
  var isType: Boolean = false
}

class NestedItemDto: AbstractItemDto() {
  var nestedId: Long = 0
}

class RefDto {
  var id: Long = 0
  var item: ItemDto? = null
}

class AttributeDto {
  var name: String = ""
  var attributeOwnerId: Long = -1

  var typeId: Long = -1
  var typeName: String = ""
  var isSingle: Boolean = false
  var allowCreate: Boolean = false

  var values: MutableList<ValueDto> = mutableListOf()
}

class ValueDto {
  var nested: NestedItemDto? = null
  var terminal: RefDto? = null
  var primitive: String? = null

  var itemOwnerId: Long = 0
}

class ItemListFilterDto(
  var setting: Long,
  var filter: String,
  var findUsages: Long,
)