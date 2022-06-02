package org.dmg.dreamhubfront.feign

import org.dmg.dreamhubfront.CacheNames.SETTING_TYPES
import org.dmg.dreamhubfront.ItemController
import org.dmg.dreamhubfront.TypeDto
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class CachedItemApi(
  private val itemController: ItemController
) {
  @Cacheable(SETTING_TYPES)
  fun getAllTypes(settingId: Long): List<TypeDto> = itemController.getAllTypes(settingId)
}