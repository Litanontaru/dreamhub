package org.dmg.dreamhubfront

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "api.item", url = "\${backend-url}")
interface ItemController {
  //--------------------------------------------------------------------------------------------------------------------

  @GetMapping("/settings/{settingId}/items")
  fun getAll(@PathVariable settingId: Long, @RequestParam filter: String?, @RequestParam findUsages: Long?): List<ItemListDto>

  @GetMapping("/settings/{settingId}/types")
  fun getAllTypes(@PathVariable settingId: Long): List<TypeDto>

  @GetMapping("/settings/{settingId}/subitems/")
  fun getSubItems(@PathVariable settingId: Long, @RequestParam superTypeIds: List<Long>): List<ItemListDto>

  //--------------------------------------------------------------------------------------------------------------------

  @GetMapping("/items/{id}")
  fun get(@PathVariable id: Long): ItemDto

  @PostMapping("/items")
  fun add(@RequestBody newItem: ItemDto): ItemDto

  @DeleteMapping("/items/{id}")
  fun remove(id: Long)

  //--------------------------------------------------------------------------------------------------------------------

  @PutMapping("/items/{id}/{nestedId}/name")
  fun setName(@PathVariable id: Long, @PathVariable nestedId: Long = -1, @RequestBody newName: String)

  @PutMapping("/items/{id}/path")
  fun setPath(@PathVariable id: Long, @RequestBody newPath: String)

  @PutMapping("/items/{id}/setting")
  fun setSetting(@PathVariable id: Long, @RequestBody newSetting: Long)

  @PutMapping("/items/{id}/formula")
  fun setFormula(@PathVariable id: Long, @RequestBody newFormula: String)

  @PutMapping("/items/{id}/istype/{newIsType}")
  fun setIsType(@PathVariable id: Long, @PathVariable newIsType: Boolean)

  //--------------------------------------------------------------------------------------------------------------------

  @PostMapping("/items/{id}/{nestedId}/extends/{newExtendsId}")
  fun addExtends(@PathVariable id: Long, @PathVariable nestedId: Long = -1, @PathVariable newExtendsId: Long): ItemDto

  @DeleteMapping("/items/{id}/{nestedId}/extends/{newExtendsId}")
  fun removeExtends(@PathVariable id: Long, @PathVariable nestedId: Long = -1, @PathVariable oldExtendsId: Long): ItemDto

  @PostMapping("/items/{id}/allowedextensions/{newAllowedExtensionId}")
  fun addAllowedExtensions(@PathVariable id: Long, @PathVariable newAllowedExtensionId: Long)

  @DeleteMapping("/items/{id}/allowedextensions/{oldAllowedExtensionId}")
  fun removeAllowedExtensions(@PathVariable id: Long, @PathVariable oldAllowedExtensionId: Long)

  //--------------------------------------------------------------------------------------------------------------------

  @PostMapping("/items/{id}/metadata")
  fun addMetadata(@PathVariable id: Long, @RequestBody newMetadata: MetadataDto)

  @PostMapping("/items/{id}/metadata/{attributeName}")
  fun removeMetadata(@PathVariable id: Long, @PathVariable attributeName: String)

  @PutMapping("/items/{id}/metadata")
  fun modifyMetadata(@PathVariable id: Long, @RequestBody newMetadata: MetadataDto)

  //--------------------------------------------------------------------------------------------------------------------

  @PostMapping("/items/{id}/{nestedId}/attribute/{attributeName}/values")
  fun addAttributeValue(@PathVariable id: Long, @PathVariable nestedId: Long = -1, @PathVariable attributeName: String, @RequestBody newValue: ValueDto)

  @DeleteMapping("/items/{id}/{nestedId}/attribute/{attributeName}/values/{valueIndex}")
  fun removeAttributeValue(@PathVariable id: Long, @PathVariable nestedId: Long = -1, @PathVariable attributeName: String, @PathVariable valueIndex: Int)

  @PutMapping("/items/{id}/{nestedId}/attribute/{attributeName}/values/{valueIndex}")
  fun modifyAttributeValue(@PathVariable id: Long, @PathVariable nestedId: Long = -1, @PathVariable attributeName: String, @PathVariable valueIndex: Int, @RequestBody newValue: ValueDto)
}