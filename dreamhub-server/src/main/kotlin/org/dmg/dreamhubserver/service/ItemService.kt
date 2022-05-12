package org.dmg.dreamhubserver.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.dmg.dreamhubfront.*
import org.dmg.dreamhubserver.*
import org.dmg.dreamhubserver.model.Item
import org.dmg.dreamhubserver.repository.ItemIndexRepository
import org.dmg.dreamhubserver.repository.ItemList
import org.dmg.dreamhubserver.repository.ItemRepository
import org.springframework.stereotype.Service

@Service
class ItemService(
  private val itemRepository: ItemRepository,
  private val itemIndexRepository: ItemIndexRepository,
  private val settingService: SettingService
) {

  fun getAll(settingId: Long): List<ItemListDto> = itemRepository.getAll(settingId).map { it.toDto() }

  fun getAll(settingId: Long, filter: String): List<ItemListDto> =
    itemRepository.getAll(settingId, "%$filter%").map { it.toDto() }

  fun getAll(settingId: Long, findUsages: Long): List<ItemListDto> = TODO("Not yet implemented")

  fun getAllRecursiveSetting(settingId: Long, superTypeId: Long): List<ItemListDto> =
    settingService
      .getDependencies(settingId)
      .let { itemIndexRepository.findAllByRefAndSettingIdIn(superTypeId, it) }
      .asSequence()
      .flatMap { it.ids.split(",") }
      .map { it.toLong() }
      .toList()
      .let { itemRepository.getAll(it) }
      .map { it.toDto() }

  fun getAllTypes(settingId: Long): List<TypeDto> {
    val types = settingService
      .getDependencies(settingId)
      .let { itemRepository.getAllTypesWithExtends(it) }
      .map { it to it.getExtends().split(",").map { it.toLong() } }
      .associateBy { it.first.getId() }

    fun process(map: Map<Long, List<Long>>, id: Long): Map<Long, List<Long>> =
      map[id]!!
        .filter { !map.containsKey(it) }
        .let { itemRepository.getAllTypesWithExtendsByIds(it) }
        .associate { it.getId() to it.getExtends().split(",").map { it.toLong() } }
        .let { it.map { it.key }.fold(map + it, ::process) }

    val all = types.map { it.key }.fold(types.mapValues { it.value.second }, ::process)

    val subTypes = types.mapValues {
      generateSequence(it.value.second) { it.filter { !types.containsKey(it) }.flatMap { all[it]!! } }
        .takeWhile { it.isNotEmpty() }
        .flatMap { it }
        .distinct()
        .toList()
    }

    return types.map {
      TypeDto().apply {
        id = it.value.first.getId()
        name = it.value.first.getName()
        path = it.value.first.getPath()
        this.settingId = it.value.first.getSettingId()
        subtypeIds = subTypes[it.key]!!
      }
    }
  }

  fun get(id: Long): ItemDto = itemRepository.getDefinitionById(id).toDto().prepare(id)

  private fun ItemDto.prepare(id: Long): ItemDto {
    fun AbstractItemDto.maxNestedId(): Long =
      attributes
        .asSequence()
        .flatMap { it.values }
        .mapNotNull { it.nested }
        .flatMap { sequenceOf(it.nestedId, it.maxNestedId()) }
        .maxOf { it }

    this.id = id
    this.nextNestedId = maxNestedId() + 1
    getRefItems()
    return this
  }

  private fun ItemDto.getRefItems() {
    (extends.asSequence() +
        attributes
          .asSequence()
          .flatMap { it.values }
          .mapNotNull { it.terminal })
      .forEach { it.item = get(it.id) }
  }

  fun add(newItem: ItemDto): ItemDto {
    removeRefItems(newItem)
    val item = Item().apply {
      name = newItem.name
      path = newItem.path
      settingId = newItem.settingId
      extends = newItem.extends.map { it.id }.joinToString()
      isType = newItem.isType
      definition = newItem.toJson()
    }
    itemRepository.save(item)
    return get(item.id)
  }

  private fun removeRefItems(dto: ItemDto) {
    (dto.extends.asSequence() +
        dto.attributes
          .asSequence()
          .flatMap { it.values }
          .mapNotNull { it.terminal })
      .forEach { it.item = null }
  }

  fun remove(id: Long) {
    itemRepository.deleteById(id)
  }

  private fun Item.modify(action: (ItemDto) -> Unit) {
    definition = definition.toDto().also { action(it) }.toJson()
  }

  private fun Item.modify(nestedId: Long, action: (AbstractItemDto) -> Unit): ItemDto {
    val root = definition.toDto()
    if (nestedId == -1L) {
      action(root)
    } else {
      root
        .attributes
        .asSequence()
        .flatMap { it.values }
        .mapNotNull { it.nested }
        .find { it.modify(nestedId, action) }
    }
    definition = root.toJson()
    return root
  }

  private fun NestedItemDto.modify(nestedId: Long, action: (NestedItemDto) -> Unit): Boolean =
    if (this.nestedId == nestedId) {
      action(this)
      true
    } else {
      attributes
        .asSequence()
        .flatMap { it.values }
        .mapNotNull { it.nested }
        .find { it.modify(nestedId, action) } != null
    }

  fun setName(id: Long, nestedId: Long, newName: String) {
    val item = itemRepository.findById(id).get()
    item.modify(nestedId) { it.name = newName }
    if (nestedId == -1L) {
      item.name = newName
    }
  }

  fun setPath(id: Long, newPath: String) {
    val item = itemRepository.findById(id).get()
    item.modify { it.path = newPath }
    item.path = newPath
  }

  fun setSetting(id: Long, newSetting: Long) {
    val item = itemRepository.findById(id).get()
    item.modify { it.settingId = newSetting }
    item.settingId = newSetting
  }

  fun setFormula(id: Long, newFormula: String) {
    itemRepository.findById(id).get().modify { it.formula = newFormula }
  }

  fun setIsType(id: Long, newIsType: Boolean) {
    val item = itemRepository.findById(id).get()
    item.modify { it.isType = newIsType }
    item.isType = newIsType
  }

  fun addExtends(id: Long, nestedId: Long, newExtendsId: Long): ItemDto =
    itemRepository
      .findById(id)
      .get()
      .modify(nestedId) { it.extends.add(RefDto().also { it.id = newExtendsId }) }
      .prepare(id)

  fun removeExtends(id: Long, nestedId: Long, oldExtendsId: Long): ItemDto =
    itemRepository
      .findById(id)
      .get()
      .modify(nestedId) { it.extends.removeIf { it.id == oldExtendsId } }
      .prepare(id)

  fun addAllowedExtensions(id: Long, newAllowedExtensionId: Long) {
    itemRepository
      .findById(id)
      .get()
      .modify {
        it.allowedExtensions.add(ItemName().also {
          it.id = newAllowedExtensionId
          it.name = itemRepository.getNameById(newAllowedExtensionId)
        })
      }
  }

  fun removeAllowedExtensions(id: Long, oldAllowedExtensionId: Long) {
    itemRepository
      .findById(id)
      .get()
      .modify { it.allowedExtensions.removeIf { it.id == oldAllowedExtensionId } }
  }

  fun addAttributeValue(id: Long, nestedId: Long, attributeName: String, newValue: ValueDto) {
    val item = itemRepository.findById(id).get()
    item.modify(nestedId) { dto ->
      val attribute = (dto
        .attributes
        .find { it.name == attributeName }                            //Данные уже есть
        ?: dto.getMetadata(attributeName)?.also { metatdata ->        //Данных нет, но метаданные есть
          AttributeDto().also {
            it.name = attributeName
            it.attributeOwnerId = metatdata.attributeOwnerId
            it.typeId = metatdata.values[0].primitive!!.toLong()
            it.typeName = itemRepository.getNameById(it.typeId)
            it.isSingle = "[]" == metatdata.values[1].primitive!!
            it.allowCreate = "+" == metatdata.values[2].primitive!!
            dto.attributes.add(it)
          }
        })
        ?: AttributeDto().also {                                      //Метаданных нет, значит это новое поле - метаданные
          it.name = attributeName
          it.attributeOwnerId = id
          it.typeId = -1
          it.typeName = ""
          it.isSingle = true
          it.allowCreate = false
          dto.attributes.add(it)
        }
      attribute
        .values
        .add(newValue)
    }
  }

  private fun AbstractItemDto.getMetadata(attributeName: String): AttributeDto? =
    extends
      .asSequence()
      .mapNotNull { it.item }
      .mapNotNull {
        it
          .attributes
          .find { it.typeId == -1L && it.name == attributeName }
          ?: it.getMetadata(attributeName)
      }
      .firstOrNull()

  fun removeAttributeValue(id: Long, nestedId: Long, attributeName: String, valueIndex: Int) {
    itemRepository
      .findById(id)
      .get()
      .modify(nestedId) {
        it
          .attributes
          .find { it.name == attributeName }
          ?.values?.removeAt(valueIndex)
      }
  }

  fun modifyAttributeValue(
    id: Long,
    nestedId: Long,
    attributeName: String,
    valueIndex: Int,
    newValue: ValueDto
  ) {
    itemRepository
      .findById(id)
      .get()
      .modify(nestedId) {
        it
          .attributes
          .find { it.name == attributeName }
          ?.values
          ?.set(valueIndex, newValue)
      }
  }
}

private fun String.toDto() = ObjectMapper().readValue(this, ItemDto::class.java)

private fun ItemDto.toJson() = ObjectMapper().writeValueAsString(this)

fun ItemList.toDto(): ItemListDto = ItemListDto().also {
  it.id = getId()
  it.name = getName()
  it.path = getPath()
  it.settingId = getSettingId()
}