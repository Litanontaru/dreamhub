package org.dmg.dreamhubserver.repository

import org.dmg.dreamhubserver.model.ItemIndex
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface ItemIndexRepository: CrudRepository<ItemIndex, Long> {
  fun findAllByRef(ref: Long): List<ItemIndex>

  fun findAllByRefAndSettingId(ref: Long, settingId: Long): ItemIndex?

  fun findAllByIdsLike(@Param("ids") ids: String): List<ItemIndex>

  fun findAllByRefInAndSettingIdIn(refs: List<Long>, settingId: List<Long>): List<ItemIndex>
}