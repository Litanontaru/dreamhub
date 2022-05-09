package org.dmg.dreamhubserver.repository

import org.dmg.dreamhubserver.model.Item
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface ItemRepository : CrudRepository<Item, Long> {
  @Query("SELECT i.id, i.name, i.path, i.settingId FROM Item i WHERE i.settingId = :settingId")
  fun getAll(settingId: Long): List<ItemList>

  @Query("SELECT i.id, i.name, i.path, i.settingId FROM Item i WHERE i.settingId = :settingId AND i.name LIKE :filter")
  fun getAll(settingId: Long, filter: String): List<ItemList>

  @Query("SELECT i.id, i.name, i.path, i.settingId FROM Item i WHERE i.id in :ids")
  fun getAll(ids: List<Long>): List<ItemList>

  @Query("SELECT i.id, i.name, i.path, i.settingId, i.extends FROM Item i WHERE i.settingId in :settingIds AND i.isType = true")
  fun getAllTypesWithExtends(settingIds: List<Long>): List<ItemWithExtendsList>

  @Query("SELECT i.id, i.extends FROM Item i WHERE i.id in :ids")
  fun getAllTypesWithExtendsByIds(ids: List<Long>): List<ItemIdExtends>

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

interface ItemIdExtends {
  fun getId(): Long
  fun getExtends(): String
}

interface ItemWithExtendsList: ItemIdExtends {
  override fun getId(): Long
  fun getName(): String
  fun getPath(): String
  fun getSettingId(): Long
  override fun getExtends(): String
}