package org.dmg.dreamhubserver.controller

import org.dmg.dreamhubfront.*
import org.dmg.dreamhubserver.service.ItemService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController

@RestController
@Transactional
class ItemControllerImpl(
  val service: ItemService
) : ItemController {
  override fun getAll(settingId: Long, filter: String?, findUsages: Long?): List<ItemListDto> =
    filter?.let { service.getAll(settingId, it) }
      ?: findUsages?.let { service.getAll(settingId, it) }
      ?: service.getAll(settingId)

  override fun getSubItems(settingId: Long, superTypeIds: List<Long>) = service.getAllRecursiveSetting(settingId, superTypeIds)

  override fun getAllTypes(settingId: Long) = service.getAllTypes(settingId)

  override fun get(id: Long) = service.get(id)

  override fun add(newItem: ItemDto): ItemDto = service.add(newItem)

  override fun remove(id: Long) {
    service.remove(id)
  }

  override fun setName(id: Long, nestedId: Long, newName: String) {
    service.setName(id, nestedId, newName)
  }

  override fun setPath(id: Long, newPath: String) {
    service.setPath(id, newPath)
  }

  override fun setSetting(id: Long, newSetting: Long) {
    service.setSetting(id, newSetting)
  }

  override fun setFormula(id: Long, newFormula: String) {
    service.setFormula(id, newFormula)
  }

  override fun setIsType(id: Long, newIsType: Boolean) {
    service.setIsType(id, newIsType)
  }

  override fun setIsFinal(id: Long, newIsFinal: Boolean) {
    service.setIsFinal(id, newIsFinal)
  }

  override fun addExtends(id: Long, nestedId: Long, newExtendsId: Long) = service.addExtends(id, nestedId, newExtendsId)

  override fun removeExtends(id: Long, nestedId: Long, oldExtendsId: Long) = service.removeExtends(id, nestedId, oldExtendsId)

  override fun addAllowedExtensions(id: Long, newAllowedExtensionId: Long) {
    service.addAllowedExtensions(id, newAllowedExtensionId)
  }

  override fun removeAllowedExtensions(id: Long, oldAllowedExtensionId: Long) {
    service.removeAllowedExtensions(id, oldAllowedExtensionId)
  }

  override fun addMetadata(id: Long, newMetadata: MetadataDto) {
    service.addMetadata(id, newMetadata)
  }

  override fun removeMetadata(id: Long, attributeName: String) {
    service.removeMetadata(id, attributeName)
  }

  override fun modifyMetadata(id: Long, newMetadata: MetadataDto) {
    service.modifyMetadata(id, newMetadata)
  }


  override fun addAttributeValue(id: Long, nestedId: Long, attributeName: String, newValue: ValueDto) {
    service.addAttributeValue(id, nestedId, attributeName, newValue)
  }

  override fun addAttributePrimitiveValue(id: Long, nestedId: Long, attributeName: String, newValue: String): ValueDto {
    return service.addAttributePrimitiveValue(id, nestedId, attributeName, newValue)
  }

  override fun addAttributeTerminalValue(id: Long, nestedId: Long, attributeName: String, newValue: Long): ValueDto {
    return service.addAttributeTerminalValue(id, nestedId, attributeName, newValue)
  }

  override fun addAttributeNestedValue(id: Long, nestedId: Long, attributeName: String, newValue: NestedItemDto): ValueDto {
    return service.addAttributeNestedValue(id, nestedId, attributeName, newValue)
  }

  override fun removeAttributeValue(id: Long, nestedId: Long, attributeName: String, valueIndex: Int) {
    service.removeAttributeValue(id, nestedId, attributeName, valueIndex)
  }

  override fun modifyAttributeValue(id: Long, nestedId: Long, attributeName: String, valueIndex: Int, newValue: ValueDto) {
    service.modifyAttributeValue(id, nestedId, attributeName, valueIndex, newValue)
  }

  override fun modifyAttributePrimitiveValue(id: Long, nestedId: Long, attributeName: String, valueIndex: Int, newValue: String): ValueDto {
    return service.modifyAttributePrimitiveValue(id, nestedId, attributeName, valueIndex, newValue)
  }
}