package org.dmg.dreamhubserver.service

import org.dmg.dreamhubserver.model.Item
import org.dmg.dreamhubserver.model.ItemIndex
import org.dmg.dreamhubserver.repository.ItemIndexRepository
import org.dmg.dreamhubserver.repository.ItemRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ItemIndexService(
  private val itemRepository: ItemRepository,
  private val itemIndexRepository: ItemIndexRepository
) {
  @Async
  @Transactional
  fun reindexAll() {
    println("REINDEX IS REQUESTED")
    itemIndexRepository.deleteAll();
    itemRepository.findAll().forEach { reindexRecursive(it) }
    println("REINDEX IS DONE")
  }

  @Async
  @Transactional
  fun reindexRecursive(item: Item) {
    item.reindexExtends()
    itemIndexRepository
      .findAllByRef(item.id)
      .asSequence()
      .flatMap { it.ids() }
      .toList()
      .let { itemRepository.findAllById(it) }
      .forEach { it.reindexThis() }
  }

  private fun Item.reindexExtends() {
    reindexThis()
    itemRepository.findAllById(extends()).forEach { it.reindexExtends() }
  }

  private fun Item.reindexThis() {
    val newId = superItems()
    val old = indexedSuperItems()
    val oldId = old.asSequence().map { it.ref }.toSet()

    newId
      .filter { !oldId.contains(it) }
      .forEach {
        itemIndexRepository
          .findAllByRefAndSettingId(it, settingId)
          ?.also { it.ids = it.ids + "$id," }
          ?: ItemIndex().apply { ref = it }
            .also {
              it.settingId = settingId
              it.ids = ",$id,"
            }
            .also(itemIndexRepository::save)
      }

    old.filter { !newId.contains(it.id) }
      .forEach { it.ids = it.ids.replace(",$id,", ",") }
  }

  private fun Item.superItems(): List<Long> =
    extends().let {
      (it + itemRepository
        .findAllById(it)
        .flatMap { it.superItems() })
        .distinct()
    }

  private fun Item.indexedSuperItems(): List<ItemIndex> = itemIndexRepository.findAllByIdsLike(",$id,")
}