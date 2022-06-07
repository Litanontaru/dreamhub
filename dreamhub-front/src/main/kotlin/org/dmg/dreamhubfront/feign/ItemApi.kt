package org.dmg.dreamhubfront.feign

import org.dmg.dreamhubfront.*
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class ItemApi(private val itemController: ItemController) {
  fun getAll(settingId: Long, filter: String?, findUsages: Long?): List<ItemListDto> {
    return itemController.getAll(settingId, filter, findUsages)
  }

  @Cacheable(CacheNames.SETTING_TYPES)
  fun getAllTypes(settingId: Long): List<TypeDto> {
    return itemController.getAllTypes(settingId)
  }

  fun getSubItems(settingId: Long, superTypeIds: List<Long>): List<ItemListDto> {
    return itemController.getSubItems(settingId, superTypeIds)
  }

  operator fun get(id: Long): ItemDto {
    return itemController.get(id)
  }

  fun add(newItem: ItemDto): ItemDto {
    return itemController.add(newItem)
  }

  fun remove(id: Long) {
    itemController.remove(id)
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
    itemController.setFormula(id, newFormula)
  }

  fun setIsType(id: Long, newIsType: Boolean) {
    itemController.setIsType(id, newIsType)
  }

  fun setIsFinal(id: Long, newIsFinal: Boolean) {
    itemController.setIsFinal(id, newIsFinal)
  }

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

  fun addMetadata(id: Long, newMetadata: MetadataDto) {
    itemController.addMetadata(id, newMetadata)
  }

  fun removeMetadata(id: Long, attributeName: String) {
    itemController.removeMetadata(id, attributeName)
  }

  fun modifyMetadata(id: Long, newMetadata: MetadataDto) {
    itemController.modifyMetadata(id, newMetadata)
  }

  fun addAttributePrimitiveValue(id: Long, nestedId: Long = -1, attributeName: String, newValue: String): ValueDto {
    return itemController.addAttributePrimitiveValue(id, nestedId, attributeName, newValue)
  }

  fun addAttributeTerminalValue(id: Long, nestedId: Long = -1, attributeName: String, newValue: Long): ValueDto {
    return itemController.addAttributeTerminalValue(id, nestedId, attributeName, newValue)
  }

  fun addAttributeNestedValue(id: Long, nestedId: Long = -1, attributeName: String, newValue: Long): ValueDto {
    return itemController.addAttributeNestedValue(id, nestedId, attributeName, newValue)
  }

  fun removeAttributeValue(id: Long, nestedId: Long = -1, attributeName: String, valueIndex: Int) {
    itemController.removeAttributeValue(id, nestedId, attributeName, valueIndex)
  }

  fun modifyAttributePrimitiveValue(id: Long, nestedId: Long = -1, attributeName: String, valueIndex: Int, newValue: String): ValueDto {
    return itemController.modifyAttributePrimitiveValue(id, nestedId, attributeName, valueIndex, newValue)
  }
}