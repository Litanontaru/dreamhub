package org.dmg.dreamhubfront.page

import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery
import org.dmg.dreamhubfront.ItemDto
import org.dmg.dreamhubfront.feign.ItemApi
import org.springframework.stereotype.Service
import java.util.stream.Stream

@Service
class ItemTreeDataProviderService(
  private val itemApi: ItemApi,
) {
  operator fun invoke(itemDto: ItemDto): ItemTreeDataProvider = ItemTreeDataProvider(itemDto, itemApi)
}

class ItemTreeDataProvider(
  itemDto: ItemDto,
  itemApi: ItemApi,
) : AbstractBackEndHierarchicalDataProvider<ItemTreeNode, Any>() {
  val root = MainItemDtoTreeNode(itemDto, itemApi)

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