package org.dmg.dreamhubserver.controller

import org.dmg.dreamhubfront.ItemController
import org.dmg.dreamhubfront.ItemDto
import org.dmg.dreamhubfront.ItemListDto
import org.dmg.dreamhubfront.ValueDto
import org.dmg.dreamhubserver.service.ItemService
import org.springframework.web.bind.annotation.RestController

@RestController
class ItemControllerImpl(
  val service: ItemService
) : ItemController {
  override fun getAll(settingId: Long, filter: String?, findUsages: Long?): List<ItemListDto> =
    filter?.let { service.getAll(settingId, it) }
      ?: findUsages?.let { service.getAll(settingId, it) }
      ?: service.getAll(settingId)

  override fun getAll(settingId: Long, superTypeId: Long) = service.getAllRecursiveSetting(settingId, superTypeId)

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

  override fun addExtends(id: Long, nestedId: Long, newExtendsId: Long) = service.addExtends(id, nestedId, newExtendsId)

  override fun removeExtends(id: Long, nestedId: Long, oldExtendsId: Long) = service.removeExtends(id, nestedId, oldExtendsId)

  override fun addAllowedExtensions(id: Long, newAllowedExtensionId: Long) {
    service.addAllowedExtensions(id, newAllowedExtensionId)
  }

  override fun removeAllowedExtensions(id: Long, oldAllowedExtensionId: Long) {
    service.removeAllowedExtensions(id, oldAllowedExtensionId)
  }

  override fun addAttributeValue(id: Long, nestedId: Long, attributeName: String, newValue: ValueDto) {
    service.addAttributeValue(id, nestedId, attributeName, newValue)
  }

  override fun removeAttributeValue(id: Long, nestedId: Long, attributeName: String, valueIndex: Int) {
    service.removeAttributeValue(id, nestedId, attributeName, valueIndex)
  }

  override fun modifyAttributeValue(
    id: Long,
    nestedId: Long,
    attributeName: String,
    valueIndex: Int,
    newValue: ValueDto
  ) {
    service.modifyAttributeValue(id, nestedId, attributeName, valueIndex, newValue)
  }
}