package org.dmg.dreamhubfront.feign

import org.dmg.dreamhubfront.*
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.lang.RuntimeException

@Service
class ItemApi(
  private val itemController: ItemController,
  private val settingController: SettingController,
) {
  fun reindexAll() {
    itemController.reindexAll();
  }

  fun getAll(settingId: Long, filter: String?, findUsages: Long?): List<ItemListDto> {
    return itemController.getAll(settingId, filter, findUsages)
  }

  @Cacheable(CacheNames.SETTING_TYPES)
  fun getAllTypes(settingId: Long): List<TypeDto> {
    return itemController.getAllTypes(settingId)
  }

  fun getSubItems(settingId: Long, superTypeIds: List<Long>): List<ItemListDto> {
    return if (superTypeIds.contains(SpecialTypes.SETTING.id)) {
      settingController.getAllSettings().map { ItemListDto().apply { id = it.id; name = it.name } }
    } else if (superTypeIds.contains(SpecialTypes.ROLE.id)) {
      UserRoleType.values().map { it.toItemListDto() }
    } else {
      itemController.getSubItems(settingId, superTypeIds)
    }
  }

  operator fun get(id: Long): ItemDto {
    try {
      return itemController.get(id)
    } catch (e: Exception) {
      throw RuntimeException("Cannot get item $id", e)
    }
  }

  fun add(newItem: ItemDto): ItemDto {
    return itemController.add(newItem)
  }

  fun remove(id: Long) {
    itemController.remove(id)
  }

  fun copy(id: Long): ItemDto {
    return itemController.copy(id)
  }

  fun setName(id: Long, nestedId: Long = -1, newName: String) {
    itemController.setName(id, nestedId, newName)
  }

  fun setPath(id: Long, newPath: String) {
    itemController.setPath(id, newPath)
  }

  fun setSetting(id: Long, newSetting: Long) {
    itemController.setSetting(id, newSetting)
  }

  fun setFormula(id: Long, newFormula: String) {
    itemController.setFormula(id, newFormula.emptySubstitute())
  }

  fun setIsType(id: Long, newIsType: Boolean) {
    itemController.setIsType(id, newIsType)
  }

  fun setDescription(id: Long, newDescription: String) {
    itemController.setDescription(id, newDescription)
  }

  fun setRank(id: Long, newRank: Int) = itemController.setRank(id, newRank)

  fun addExtends(id: Long, nestedId: Long = -1, newExtendsId: Long): ItemDto {
    return itemController.addExtends(id, nestedId, newExtendsId)
  }

  fun removeExtends(id: Long, nestedId: Long = -1, oldExtendsId: Long): ItemDto {
    return itemController.removeExtends(id, nestedId, oldExtendsId)
  }

  fun addAllowedExtensions(id: Long, newAllowedExtensionId: Long) {
    itemController.addAllowedExtensions(id, newAllowedExtensionId)
  }

  fun removeAllowedExtensions(id: Long, oldAllowedExtensionId: Long) {
    itemController.removeAllowedExtensions(id, oldAllowedExtensionId)
  }

  fun setGroup(id: Long, groups: String) {
    itemController.setGroup(id, groups.emptySubstitute())
  }

  fun addMetadata(id: Long, newMetadata: MetadataDto) {
    itemController.addMetadata(id, newMetadata)
  }

  fun removeMetadata(id: Long, attributeName: String) {
    itemController.removeMetadata(id, attributeName.emptySubstitute())
  }

  fun modifyMetadata(id: Long, newMetadata: MetadataDto) {
    itemController.modifyMetadata(id, newMetadata)
  }

  fun upMetadata(id: Long, attributeName: String) {
    itemController.upMetadata(id, attributeName.emptySubstitute())
  }

  fun downMetadata(id: Long, attributeName: String) {
    itemController.downMetadata(id, attributeName.emptySubstitute())
  }

  fun addAttributePrimitiveValue(id: Long, nestedId: Long = -1, attributeName: String, newValue: String): ValueDto {
    return itemController.addAttributePrimitiveValue(id, nestedId, attributeName.emptySubstitute(), newValue)
  }

  fun addAttributeTerminalValue(id: Long, nestedId: Long = -1, attributeName: String, newValue: Long): ValueDto {
    return itemController.addAttributeTerminalValue(id, nestedId, attributeName.emptySubstitute(), newValue)
  }

  fun addAttributeNestedValue(id: Long, nestedId: Long = -1, attributeName: String, newValue: Long): ValueDto {
    return itemController.addAttributeNestedValue(id, nestedId, attributeName.emptySubstitute(), newValue)
  }

  fun removeAttributeValue(id: Long, nestedId: Long = -1, attributeName: String, valueIndex: Int) {
    itemController.removeAttributeValue(id, nestedId, attributeName.emptySubstitute(), valueIndex)
  }

  fun modifyAttributePrimitiveValue(id: Long, nestedId: Long = -1, attributeName: String, valueIndex: Int, newValue: String): ValueDto {
    return itemController.modifyAttributePrimitiveValue(id, nestedId, attributeName.emptySubstitute(), valueIndex, newValue)
  }

  fun moveAttributeUp(id: Long, nestedId: Long, attributeName: String, valueIndex: Int) {
    return itemController.moveAttributeUp(id, nestedId, attributeName.emptySubstitute(), valueIndex)
  }

  fun moveAttributeDown(id: Long, nestedId: Long, attributeName: String, valueIndex: Int) {
    return itemController.moveAttributeDown(id, nestedId, attributeName.emptySubstitute(), valueIndex)
  }
}

fun String.emptySubstitute() = when (this) {
  "" -> "__"
  else -> this
}