package org.dmg.dreamhubfront.page

import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery
import org.dmg.dreamhubfront.ItemController
import org.dmg.dreamhubfront.ItemDto
import org.springframework.stereotype.Service
import java.util.stream.Stream

@Service
class ItemTreeDataProviderService(
  private val itemController: ItemController
) {
  operator fun invoke(itemDto: ItemDto): ItemTreeDataProvider = ItemTreeDataProvider(itemDto, itemController)
}

class ItemTreeDataProvider(
  itemDto: ItemDto,
  itemController: ItemController
): AbstractBackEndHierarchicalDataProvider<ItemTreeNode, Any>() {
  val root = MainItemDtoTreeNode(itemDto, itemController)

  override fun getChildCount(query: HierarchicalQuery<ItemTreeNode, Any>?): Int =
    query?.parent?.count() ?: 1

  override fun hasChildren(node: ItemTreeNode?): Boolean = node?.hasChildren() ?: true

  override fun fetchChildrenFromBackEnd(query: HierarchicalQuery<ItemTreeNode, Any>?): Stream<ItemTreeNode> {
    return query?.parent?.children()?.stream() ?: Stream.of(root)
  }
}