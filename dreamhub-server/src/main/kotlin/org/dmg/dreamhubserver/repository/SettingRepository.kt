package org.dmg.dreamhubserver.repository

import org.dmg.dreamhubserver.model.Setting
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface SettingRepository: CrudRepository<Setting, Long> {
  @Query("SELECT s.id, s.name FROM Setting s where id in :ids")
  fun getAll(ids: List<Long>): List<SettingList>
}

interface SettingList {
  fun getId(): Long
  fun getName(): String
}