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
  override fun reindexAll() = service.reindexAll()

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

  override fun copy(id: Long) = service.copy(id)

  override fun setName(id: Long, nestedId: Long, newName: String) {
    service.setName(id, nestedId, newName.substring(1, newName.length - 1))
  }

  override fun setPath(id: Long, newPath: String) {
    service.setPath(id, newPath)
  }

  override fun setSetting(id: Long, newSetting: Long) {
    service.setSetting(id, newSetting)
  }

  override fun setFormula(id: Long, newFormula: String) {
    service.setFormula(id, newFormula.emptySubstitute())
  }

  override fun setIsType(id: Long, newIsType: Boolean) {
    service.setIsType(id, newIsType)
  }

  override fun setDescription(id: Long, newDescription: String) {
    service.setDescription(id, newDescription)
  }

  override fun setRank(id: Long, newRank: Int) = service.setRank(id, newRank)

  override fun addExtends(id: Long, nestedId: Long, newExtendsId: Long) = service.addExtends(id, nestedId, newExtendsId)

  override fun removeExtends(id: Long, nestedId: Long, oldExtendsId: Long) = service.removeExtends(id, nestedId, oldExtendsId)

  override fun addAllowedExtensions(id: Long, newAllowedExtensionId: Long) {
    service.addAllowedExtensions(id, newAllowedExtensionId)
  }

  override fun removeAllowedExtensions(id: Long, oldAllowedExtensionId: Long) {
    service.removeAllowedExtensions(id, oldAllowedExtensionId)
  }

  override fun setGroup(id: Long, groups: String) {
    service.setGroup(id, groups.emptySubstitute())
  }

  override fun addMetadata(id: Long, newMetadata: MetadataDto) {
    service.addMetadata(id, newMetadata)
  }

  override fun removeMetadata(id: Long, attributeName: String) {
    service.removeMetadata(id, attributeName.emptySubstitute())
  }

  override fun modifyMetadata(id: Long, newMetadata: MetadataDto) {
    service.modifyMetadata(id, newMetadata)
  }

  override fun upMetadata(id: Long, attributeName: String) {
    service.upMetadata(id, attributeName.emptySubstitute())
  }

  override fun downMetadata(id: Long, attributeName: String) {
    service.downMetadata(id, attributeName.emptySubstitute())
  }

  override fun addAttributePrimitiveValue(id: Long, nestedId: Long, attributeName: String, newValue: String): ValueDto {
    return service.addAttributePrimitiveValue(id, nestedId, attributeName.emptySubstitute(), newValue.emptySubstitute())
  }

  override fun addAttributeTerminalValue(id: Long, nestedId: Long, attributeName: String, newValue: Long): ValueDto {
    return service.addAttributeTerminalValue(id, nestedId, attributeName.emptySubstitute(), newValue)
  }

  override fun addAttributeNestedValue(id: Long, nestedId: Long, attributeName: String, newValue: Long): ValueDto {
    return service.addAttributeNestedValue(id, nestedId, attributeName.emptySubstitute(), newValue)
  }

  override fun removeAttributeValue(id: Long, nestedId: Long, attributeName: String, valueIndex: Int) {
    service.removeAttributeValue(id, nestedId, attributeName.emptySubstitute(), valueIndex)
  }

  override fun modifyAttributePrimitiveValue(id: Long, nestedId: Long, attributeName: String, valueIndex: Int, newValue: String): ValueDto {
    return service.modifyAttributePrimitiveValue(id, nestedId, attributeName.emptySubstitute(), valueIndex, newValue.emptySubstitute())
  }

  override fun moveAttributeUp(id: Long, nestedId: Long, attributeName: String, valueIndex: Int) {
    return service.moveAttributeUp(id, nestedId, attributeName.emptySubstitute(), valueIndex)
  }

  override fun moveAttributeDown(id: Long, nestedId: Long, attributeName: String, valueIndex: Int) {
    return service.moveAttributeDown(id, nestedId, attributeName.emptySubstitute(), valueIndex)
  }
}

fun String.emptySubstitute() = when (this) {
  "__" -> ""
  else -> this
}