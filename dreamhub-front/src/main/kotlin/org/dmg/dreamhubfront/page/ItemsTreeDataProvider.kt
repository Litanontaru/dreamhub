package org.dmg.dreamhubfront.page

import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery
import org.dmg.dreamhubfront.ItemListDto
import org.dmg.dreamhubfront.SettingController
import org.dmg.dreamhubfront.SettingDto
import org.dmg.dreamhubfront.feign.ItemApi
import org.springframework.stereotype.Service
import java.util.stream.Stream

@Service
class ItemsTreeDataProviderService(
  private val settingController: SettingController,
  private val itemApi: ItemApi
) {
  operator fun invoke(settingId: Long): ItemsTreeDataProvider = ItemsTreeDataProvider(settingController, itemApi, settingId)
}

class ItemsTreeDataProvider(
  private val settingController: SettingController,
  private val itemApi: ItemApi,
  private val settingId: Long,
) : AbstractBackEndHierarchicalDataProvider<ItemListView, Any>() {

  private lateinit var setting: SettingDto
  private val root = "".toFolder()
  private var tree: MutableMap<ItemListView, MutableList<ItemListView>>
  private var parents: Map<ItemListView, ItemListView>
  private var index: Map<Long, ItemListView>

  init {
    setting = settingController.getSettingById(settingId)!!
    tree = readTree()
    tree[root]?.add(setting.toTerminal())
    parents = tree.entries.asSequence().flatMap { pair -> pair.value.asSequence().map { it to pair.key } }.associate { it }
    index = tree
      .values
      .asSequence()
      .flatten()
      .filter { it.item != null }
      .distinctBy { it.item!!.id }
      .associateBy { it.item!!.id }
  }

  private fun readTree(): MutableMap<ItemListView, MutableList<ItemListView>> =
    itemApi.getAll(settingId, null, null)
      .flatMap {
        (listOf("") + it.path.split(".") + it).let { parts ->
          (1 until parts.size)
            .map { 0..it }
            .map { it.map { parts[it] } }
            .map { part ->
              val path = part.take(part.size - 1).map { it as String }.filter { it.isNotBlank() }.joinToString(".")
              val element = when (part.last()) {
                is String -> part.map { it as String }.filter { it.isNotBlank() }.joinToString(".")
                is ItemListDto -> part.last()
                else -> throw IllegalStateException()
              }
              path to element
            }
        }
      }
      .distinct()
      .groupBy({ it.first.toFolder() }, { it.second })
      .mapValues {
        it
          .value
          .map {
            when (it) {
              is String -> it.toFolder()
              is ItemListDto -> it.toTerminal()
              else -> throw IllegalStateException()
            }
          }
          .toMutableList()
      }
      .toMutableMap()

  operator fun get(itemId: Long): List<ItemListView> =
    index[itemId]?.let { generateSequence(it) { parents[it] }.toList().reversed() } ?: listOf()

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
    (query?.parent ?: root)
      .let { children(it) }
      ?.sortedWith(compareByDescending<ItemListView> { it.isFolder }.thenBy { it.name })
      ?.stream()
      ?: Stream.empty()

  fun children(it: ItemListView) = tree[it]

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
          ?.let {
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
      .forEach { this.refreshItem(it.first, true) }
  }
}

data class ItemListView(
  var name: String,
  var path: String,
  val item: ItemListDto? = null,
  val setting: SettingDto? = null
) {
  val isFolder: Boolean = item == null && setting == null
  val isSetting: Boolean = setting != null
  val fullName = path.takeIf { it.isNotBlank() }?.let { "$it.$name" } ?: name
}

fun String.toFolder(): ItemListView =
  when (val index = lastIndexOf(".")) {
    -1 -> ItemListView(this, "")
    else -> ItemListView(substring(index + 1), substring(0, index))
  }

fun ItemListDto.toTerminal(): ItemListView = ItemListView(name, path, this)

fun SettingDto.toTerminal(): ItemListView = ItemListView(
  name = name,
  path = "",
  setting = this
)