package org.dmg.dreamhubserver.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.dmg.dreamhubfront.*
import org.dmg.dreamhubfront.StandardTypes.TYPE
import org.dmg.dreamhubserver.model.Item
import org.dmg.dreamhubserver.model.ItemIndex
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
    (settingService.getDependencies(settingId) + settingId)
      .let { itemIndexRepository.findAllByRefAndSettingIdIn(superTypeId, it) }
      .flatMap { it.ids() }
      .let { itemRepository.getAll(it) }
      .map { it.toDto() }

  fun getAllTypes(settingId: Long): List<TypeDto> = settingService.getAllDependencyTypes(settingId)

  fun get(id: Long): ItemDto = itemRepository.getDefinitionById(id).toDto().prepare(id)

  private fun ItemDto.prepare(id: Long): ItemDto {
    fun AbstractItemDto.maxNestedId(): Long =
      attributes
        .asSequence()
        .flatMap { it.values }
        .mapNotNull { it.nested }
        .flatMap { sequenceOf(it.nestedId, it.maxNestedId()) }
        .maxOfOrNull { it }
        ?: 0

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

    item.reindexRecursive()

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
      .let { item ->
        item
          .modify(nestedId) { it.extends.add(RefDto().also { it.id = newExtendsId }) }
          .prepare(id)
          .also { item.reindexRecursive() }
      }


  fun removeExtends(id: Long, nestedId: Long, oldExtendsId: Long): ItemDto =
    itemRepository
      .findById(id)
      .get()
      .let { item ->
        item
          .modify(nestedId) { it.extends.removeIf { it.id == oldExtendsId } }
          .prepare(id)
          .also { item.reindexRecursive() }
      }

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
            it.type = metatdata.values[0].toAttributeTypeDto()
            dto.attributes.add(it)
          }
        })
        ?: AttributeDto().also {                                      //Метаданных нет, значит это новое поле - метаданные
          it.name = attributeName
          it.attributeOwnerId = id
          it.type.id = TYPE.id
          it.type.isSingle = true
          it.type.allowCreate = false
          dto.attributes.add(it)
        }
      attribute
        .values
        .add(newValue)
    }
  }

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

  private fun Item.superItems(): List<Long> =
    extends().let {
      (it + itemRepository
        .findAllById(it)
        .flatMap { it.superItems() })
        .distinct()
    }

  private fun Item.indexedSuperItems(): List<ItemIndex> = itemIndexRepository.findAllByIdsLike(",$id,")

  private fun Item.reindexThis() {
    val newId = superItems()
    val old = indexedSuperItems()
    val oldId = old.asSequence().map { it.ref }.toSet()

    newId
      .filter { !oldId.contains(it) }
      .forEach {
        itemIndexRepository
          .findAllByRefAndSettingId(it, settingId)
          ?.also { it.ids = it.ids + "$id," }
          ?: ItemIndex(it, settingId)
            .also {
              it.ids = ",$id,"
              itemIndexRepository.save(it)
            }
      }

    old.filter { !newId.contains(it.id) }
      .forEach { it.ids = it.ids.replace(",$id,", ",") }
  }

  private fun Item.reindexRecursive() {
    reindexThis()
    itemIndexRepository
      .findAllByRef(id)
      .asSequence()
      .flatMap { it.ids() }
      .toList()
      .let { itemRepository.findAllById(it) }
      .forEach { it.reindexThis() }
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