package org.dmg.dreamhubserver.repository

import org.dmg.dreamhubserver.model.ItemIndex
import org.springframework.data.repository.CrudRepository

interface ItemIndexRepository: CrudRepository<ItemIndex, Long> {
  fun findAllByRefAndSettingIdIn(ref: Long, settingId: List<Long>): List<ItemIndex>
}