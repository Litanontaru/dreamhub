package org.dmg.dreamhubfront.page

import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery
import org.dmg.dreamhubfront.ItemDto
import org.dmg.dreamhubfront.SettingController
import org.dmg.dreamhubfront.SettingDto
import org.dmg.dreamhubfront.feign.ItemApi
import org.springframework.stereotype.Service
import java.util.stream.Stream

@Service
class ItemTreeDataProviderService(
  private val itemApi: ItemApi,
  private val settingController: SettingController
) {
  operator fun invoke(itemDto: ItemDto): ItemTreeDataProvider = ItemTreeDataProviderImpl(itemDto, itemApi)

  operator fun invoke(settingDto: SettingDto): ItemTreeDataProvider = SettingTreeDataProvider(settingDto, settingController)
}

interface ItemTreeDataProvider : HierarchicalDataProvider<ItemTreeNode, Any> {
  fun root(): ItemTreeNode;
}

class ItemTreeDataProviderImpl(
  itemDto: ItemDto,
  itemApi: ItemApi,
) : AbstractBackEndHierarchicalDataProvider<ItemTreeNode, Any>(), ItemTreeDataProvider {
  private val root = MainItemDtoTreeNode(itemDto, itemApi)

  override fun root(): ItemTreeNode = root

  override fun getChildCount(query: HierarchicalQuery<ItemTreeNode, Any>?): Int {
    return track("count\t${query?.parent?.name()}") {
      query?.parent?.compacted()?.last()?.cachedCount() ?: 1
    }
  }

  override fun hasChildren(node: ItemTreeNode?): Boolean {
    return track("has\t${node?.name()}") {
      node?.compacted()?.last()?.cacheHasChildren() ?: true
    }
  }

  override fun fetchChildrenFromBackEnd(query: HierarchicalQuery<ItemTreeNode, Any>?): Stream<ItemTreeNode> {
    return track("fetch\t${query?.parent?.name()}") {
      query?.parent?.compacted()?.last()?.cachedChildren()?.stream() ?: Stream.of(root)
    }
  }
}

class SettingTreeDataProvider(
  settingDto: SettingDto,
  settingController: SettingController,
) : AbstractBackEndHierarchicalDataProvider<ItemTreeNode, Any>(), ItemTreeDataProvider {
  private val root = SettingItemTreeNode(settingDto, settingController, null, false)

  override fun root(): ItemTreeNode = root

  override fun getChildCount(query: HierarchicalQuery<ItemTreeNode, Any>?): Int {
    return track("count\t${query?.parent?.name()}") {
      query?.parent?.compacted()?.last()?.cachedCount() ?: 1
    }
  }

  override fun hasChildren(node: ItemTreeNode?): Boolean {
    return track("has\t${node?.name()}") {
      node?.compacted()?.last()?.cacheHasChildren() ?: true
    }
  }

  override fun fetchChildrenFromBackEnd(query: HierarchicalQuery<ItemTreeNode, Any>?): Stream<ItemTreeNode> {
    return track("fetch\t${query?.parent?.name()}") {
      query?.parent?.compacted()?.last()?.cachedChildren()?.stream() ?: Stream.of(root)
    }
  }
}