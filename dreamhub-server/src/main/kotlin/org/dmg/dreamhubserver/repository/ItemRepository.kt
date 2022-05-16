package org.dmg.dreamhubserver.repository

import org.dmg.dreamhubserver.model.Item
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface ItemRepository : CrudRepository<Item, Long> {
  @Query("SELECT i.id as id, i.name as name, i.path as path, i.settingId as settingId FROM Item i WHERE i.settingId = :settingId")
  fun getAll(settingId: Long): List<ItemList>

  @Query("SELECT i.id as id, i.name as name, i.path as path, i.settingId as settingId FROM Item i WHERE i.settingId = :settingId AND i.name LIKE :filter")
  fun getAll(settingId: Long, filter: String): List<ItemList>

  @Query("SELECT i.id as id, i.name as name, i.path as path, i.settingId as settingId FROM Item i WHERE i.id in :ids")
  fun getAll(ids: List<Long>): List<ItemList>

  @Query("SELECT i.definition FROM Item i Where i.id = :id")
  fun getDefinitionById(id: Long): String

  @Query("SELECT i.name FROM Item i Where i.id = :id")
  fun getNameById(id: Long): String
}

interface ItemList {
  fun getId(): Long
  fun getName(): String
  fun getPath(): String
  fun getSettingId(): Long
}