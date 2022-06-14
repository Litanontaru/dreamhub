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
    val t = System.nanoTime()
    val result= query?.parent?.compacted()?.last()?.cachedCount() ?: 1
    println("count\t${query?.parent?.name()}\t${System.nanoTime() - t}")
    return result
  }

  override fun hasChildren(node: ItemTreeNode?): Boolean {
    val t = System.nanoTime()
    val result = node?.compacted()?.last()?.cacheHasChildren() ?: true
    println("has\t${node?.name()}\t${System.nanoTime() - t}")
    return result
  }

  override fun fetchChildrenFromBackEnd(query: HierarchicalQuery<ItemTreeNode, Any>?): Stream<ItemTreeNode> {
    val t = System.nanoTime()
    val result: Stream<ItemTreeNode> = query?.parent?.compacted()?.last()?.cachedChildren()?.stream() ?: Stream.of(root)
    println("fetch\t${query?.parent?.name()}\t${System.nanoTime() - t}")
    return result
  }
}