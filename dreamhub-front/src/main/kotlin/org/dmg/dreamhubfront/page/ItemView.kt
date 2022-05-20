package org.dmg.dreamhubfront.page

import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.treegrid.TreeGrid
import org.dmg.dreamhubfront.ItemController

class ItemView(
  private val itemController: ItemController,
  private val itemTreeDataProviderService: ItemTreeDataProviderService
): VerticalLayout() {
  var itemId: Long = -1
    set(value) = setItem(value)

  init {
    isVisible = false
  }

  private fun setItem(itemId: Long) {
    if (this.itemId != itemId) {
      removeAll()

      if (itemId == -1L) {
        isVisible = false
        return
      } else {
        isVisible = true
      }
      var item = itemController.get(itemId)

      add(HorizontalLayout().apply {
        TreeGrid<ItemTreeNode>().also { tree ->
          tree.addHierarchyColumn { it.name() }
          tree.setDataProvider(itemTreeDataProviderService(item))
          add(tree)
          width = "100%"
          height = "100%"
        }
        width = "100%"
      })
    }
  }
}