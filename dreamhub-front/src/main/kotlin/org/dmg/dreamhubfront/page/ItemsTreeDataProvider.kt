package org.dmg.dreamhubfront.page

import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery
import org.dmg.dreamhubfront.ItemController
import org.dmg.dreamhubfront.ItemListDto
import org.springframework.stereotype.Service
import java.util.stream.Stream

@Service
class ItemsTreeDataProviderService(
  private val itemController: ItemController
) {
  operator fun invoke(settingId: Long): ItemsTreeDataProvider = ItemsTreeDataProvider(itemController, settingId)
}

class ItemsTreeDataProvider(
  private val itemController: ItemController,
  private val settingId: Long
) : AbstractBackEndHierarchicalDataProvider<ItemListView, Any>() {
  private val root = "".toFolder()
  private var tree: MutableMap<ItemListView, MutableList<ItemListView>>

  init {
    tree = readTree()
  }

  private fun readTree(): MutableMap<ItemListView, MutableList<ItemListView>> {
    val terminals = itemController.getAll(settingId, null, null)
      .groupBy { it.path }
      .mapValues { it.value.map { it.toTerminal() }.sortedBy { it.name } }
    val folders = terminals.keys
      .map { it.toFolder() }
      .filter { it.name.isNotBlank() }
      .groupBy { it.path }
    return (folders.asSequence() + terminals.asSequence())
      .groupBy { it.key }
      .mapValues { it.value.flatMap { it.value }.toMutableList() }
      .mapKeys { it.key.toFolder() }
      .toMutableMap()
  }

  override fun refreshAll() {
    tree = readTree()
    super.refreshAll()
  }

  override fun refreshItem(item: ItemListView?, refreshChildren: Boolean) {
    tree = readTree()
    super.refreshItem(item, refreshChildren)
  }

  override fun refreshItem(item: ItemListView?) {
    tree = readTree()
    super.refreshItem(item)
  }

  override fun getChildCount(query: HierarchicalQuery<ItemListView, Any>?): Int =
    (query?.parent ?: root).let { tree[it]?.size ?: 0 }

  override fun hasChildren(item: ItemListView?): Boolean = item?.isFolder ?: false

  override fun fetchChildrenFromBackEnd(query: HierarchicalQuery<ItemListView, Any>?): Stream<ItemListView> =
    (query?.parent ?: root).let { tree[it]?.stream() ?: Stream.empty() }

  fun add(item: ItemListDto) {
    val chain = item
      .path
      .split(".")
      .fold(listOf(root)) { acc, i -> acc + ItemListView(i, acc.last().fullName) }

    (chain + item.toTerminal())
      .windowed(2, 1)
      .map { it[0] to it[1] }
      .filter { (parent, child) ->
        tree[parent]
          ?. let {
            val added = !it.contains(child)
            if (added) {
              it.add(child)
            }
            added
          }
          ?: mutableListOf(child).let {
            tree.put(parent, it)
            true
          }
      }
      .forEach { this.refreshItem(it.first,  true) }
  }
}

data class ItemListView(val name: String, val path: String, val item: ItemListDto? = null) {
  val isFolder: Boolean = item == null
  val fullName = path.takeIf { it.isNotBlank() }?.let { "$it.$name" } ?: name
}

fun String.toFolder(): ItemListView =
  when (val index = lastIndexOf(".")) {
    -1 -> ItemListView(this, "")
    else -> ItemListView(substring(index + 1), substring(0, index - 1))
  }

fun ItemListDto.toTerminal(): ItemListView = ItemListView(name, path, this)