package org.dmg.dreamhubserver.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.dmg.dreamhubfront.*
import org.dmg.dreamhubfront.StandardTypes.TYPE
import org.dmg.dreamhubserver.model.Item
import org.dmg.dreamhubserver.model.ItemIndex
import org.dmg.dreamhubserver.repository.ItemIndexRepository
import org.dmg.dreamhubserver.repository.ItemList
import org.dmg.dreamhubserver.repository.ItemListWithExtends
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

  fun getAllRecursiveSetting(settingId: Long, superTypeIds: List<Long>): List<ItemListDto> =
    (settingService.getDependencies(settingId) + settingId)
      .let { itemIndexRepository.findAllByRefInAndSettingIdIn(superTypeIds, it) }
      .flatMap { it.ids() }
      .distinct()
      .let { itemRepository.getAll(it) }
      .let {
        when {
          superTypeIds.contains(TYPE.id) -> it + getAllTypeList(settingId)
          else -> it
        }
      }
      .distinctBy { it.getId() }
      .map { it.toDto() }

  fun getAllTypes(settingId: Long): List<TypeDto> = getAllTypeList(settingId).map { it.toDto() }

  private fun getAllTypeList(settingId: Long) =
    (settingService.getDependencies(settingId) + settingId).let { itemRepository.getAllTypes(it) }

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

  private fun AbstractItemDto.getRefItems() {
    (extends.asSequence() +
        attributes
          .asSequence()
          .flatMap { it.values }
          .mapNotNull { it.terminal })
      .forEach { it.item = get(it.id) }

    attributes
      .asSequence()
      .flatMap { it.values }
      .mapNotNull { it.nested }
      .forEach { it.getRefItems() }
  }

  private fun AbstractItemDto.refreshExtendsOnly(): AbstractItemDto {
    extends
      .forEach() {
        it.item = itemRepository.getDefinitionById(it.id).toDto()
        it.item?.refreshExtendsOnly()
      }
    return this
  }

  fun add(newItem: ItemDto): ItemDto {
    newItem.removeRefItems()
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

  private fun AbstractItemDto.removeRefItems() {
    (extends.asSequence() +
        attributes
          .asSequence()
          .flatMap { it.values }
          .mapNotNull { it.terminal })
      .forEach { it.item = null }
  }

  fun remove(id: Long) {
    itemRepository.deleteById(id)
  }

  private fun Item.modify(action: (ItemDto) -> Unit) {
    definition = definition.toDto().also { action(it) }.apply { removeRefItems() }.toJson()
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
    definition = root.apply { removeRefItems() }.toJson()
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

  fun setIsFinal(id: Long, newIsFinal: Boolean) {
    itemRepository.findById(id).get().modify { it.isFinal = newIsFinal }
  }

  fun addExtends(id: Long, nestedId: Long, newExtendsId: Long): ItemDto =
    itemRepository
      .findById(id)
      .get()
      .let { item ->
        item
          .modify(nestedId) { it.extends.add(RefDto().also { it.id = newExtendsId }) }
          .prepare(id)
          .also {
            if (nestedId == -1L) {
              item.extends = (item.extends() + newExtendsId).joinToString()
            }
            item.reindexRecursive()
          }
      }

  fun removeExtends(id: Long, nestedId: Long, oldExtendsId: Long): ItemDto =
    itemRepository
      .findById(id)
      .get()
      .let { item ->
        item
          .modify(nestedId) { it.extends.removeIf { it.id == oldExtendsId } }
          .prepare(id)
          .also {
            if (nestedId == -1L) {
              item.extends = item.extends().filter { it != oldExtendsId }.joinToString()
            }
            item.reindexRecursive()
          }
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

  fun addMetadata(id: Long, newMetadata: MetadataDto) {
    itemRepository
      .findById(id)
      .get()
      .modify { it.metadata.add(newMetadata) }
  }

  fun removeMetadata(id: Long, attributeName: String) {
    itemRepository
      .findById(id)
      .get()
      .modify { it.metadata.removeIf { it.attributeName == attributeName } }
  }

  fun modifyMetadata(id: Long, newMetadata: MetadataDto) {
    itemRepository
      .findById(id)
      .get()
      .modify {
        it.metadata.filter { it.attributeName == newMetadata.attributeName }.forEach {
          it.typeId = newMetadata.typeId
          it.isSingle = newMetadata.isSingle
          it.allowCreate = newMetadata.allowCreate
        }
      }
  }

  fun addAttributeValue(id: Long, nestedId: Long, attributeName: String, newValue: ValueDto) {
    val item = itemRepository.findById(id).get()
    item.modify(nestedId) { dto ->
      val attribute = dto
        .attributes
        .firstOrNull { it.name == attributeName }                            //Данные уже есть
        ?: dto
          .refreshExtendsOnly()
          .getMetadata(attributeName)
          ?.let { metatdata ->        //Данных нет, но метаданные есть
            AttributeDto().also {
              it.name = attributeName
              dto.attributes.add(it)
            }
          }
        ?: throw IllegalStateException("Cannot find attribute with name $attributeName")

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

  private fun Item.reindexExtends() {
    reindexThis()
    itemRepository.findAllById(extends()).forEach { it.reindexExtends() }
  }

  private fun Item.reindexRecursive() {
    reindexExtends()
    itemIndexRepository
      .findAllByRef(id)
      .asSequence()
      .flatMap { it.ids() }
      .toList()
      .let { itemRepository.findAllById(it) }
      .forEach { it.reindexThis() }
  }
}

private fun String.toDto() = ObjectMapper()
  .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  .readValue(this, ItemDto::class.java)

private fun ItemDto.toJson() = ObjectMapper().writeValueAsString(this)

fun ItemList.toDto(): ItemListDto = ItemListDto().also {
  it.id = getId()
  it.name = getName()
  it.path = getPath()
  it.settingId = getSettingId()
}

fun ItemListWithExtends.toDto() = TypeDto().apply {
  id = getId()
  name = getName()
  path = getPath()
  settingId = getSettingId()
  superTypeIds = getExtends().split(",").mapNotNull { it.toLongOrNull() }
}