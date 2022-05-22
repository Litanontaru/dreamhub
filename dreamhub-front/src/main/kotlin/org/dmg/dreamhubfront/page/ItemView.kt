package org.dmg.dreamhubfront.page

import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.treegrid.TreeGrid
import org.dmg.dreamhubfront.ItemController

class ItemView(
  private val itemController: ItemController,
  private val itemTreeDataProviderService: ItemTreeDataProviderService,
  private val settingId: Long
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
          val dataProvider = itemTreeDataProviderService(item)

          //ADD SELECTION TRACKER
          var selectedItem: ItemTreeNode? = null
          fun updateSelection(node: ItemTreeNode?) {
            if (node == null || selectedItem == null || node != selectedItem) {
              val oldSelected = selectedItem
              selectedItem = node
              if (oldSelected != null) {
                dataProvider.refreshItem(oldSelected)
              }
              if (selectedItem != null) {
                dataProvider.refreshItem(selectedItem)
              }
            }
          }

          tree.addSelectionListener { updateSelection(it.firstSelectedItem.orElse(null)) }

          tree.addComponentHierarchyColumn { item ->
            Lines.toComponent(item, item == selectedItem, itemController, settingId) { node, refreshChildren ->
              dataProvider.refreshItem(node, refreshChildren)
            }
          }.also {
            it.width = "100%"
            it.flexGrow = 1
            it.isAutoWidth = true
          }

          tree.setDataProvider(dataProvider)
          add(tree)
          width = "100%"
          height = "100%"
        }
        width = "100%"
      })
    }
  }
}