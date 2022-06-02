package org.dmg.dreamhubfront.page

import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery
import org.dmg.dreamhubfront.ItemDto
import org.dmg.dreamhubfront.feign.ItemApi
import org.springframework.stereotype.Service
import java.util.stream.Stream

@Service
class ItemTreeDataProviderService(
  private val itemApi: ItemApi
) {
  operator fun invoke(itemDto: ItemDto): ItemTreeDataProvider = ItemTreeDataProvider(itemDto, itemApi)
}

class ItemTreeDataProvider(
  itemDto: ItemDto,
  itemApi: ItemApi
): AbstractBackEndHierarchicalDataProvider<ItemTreeNode, Any>() {
  val root = MainItemDtoTreeNode(itemDto, itemApi)

  override fun getChildCount(query: HierarchicalQuery<ItemTreeNode, Any>?): Int =
    query?.parent?.compacted()?.last()?.count() ?: 1

  override fun hasChildren(node: ItemTreeNode?): Boolean = node?.compacted()?.last()?.hasChildren() ?: true

  override fun fetchChildrenFromBackEnd(query: HierarchicalQuery<ItemTreeNode, Any>?): Stream<ItemTreeNode> {
    return query?.parent?.compacted()?.last()?.children()?.stream() ?: Stream.of(root)
  }
}