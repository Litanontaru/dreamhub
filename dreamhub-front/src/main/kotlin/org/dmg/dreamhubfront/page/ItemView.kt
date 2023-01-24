package org.dmg.dreamhubfront.page

import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.treegrid.TreeGrid
import org.dmg.dreamhubfront.feign.ItemApi

class ItemView(
  private val itemApi: ItemApi,
  private val itemTreeDataProviderService: ItemTreeDataProviderService,
  private val settingId: Long
) : VerticalLayout() {
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
      val item = !itemApi.get(itemId)

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
            Lines.toComponent(item, item == selectedItem, itemApi, settingId) { node, refreshChildren ->
              dataProvider.refreshItem(node, refreshChildren)
            }
          }.also {
            it.width = "100%"
            it.flexGrow = 1
            it.isAutoWidth = true
          }
          tree.addColumn { item ->
            val rates = item.compacted().mapNotNull { it.rate() }.filter { it.isNotBlank() }.distinct().toList()
            if (rates.size == 1) {
              rates.joinToString()
            } else {
              rates.filter { it != "0" }.joinToString()
            }
          }.also {
            it.width = "10em"
            it.flexGrow = 0
          }

          tree.setDataProvider(dataProvider)
          tree.expand(dataProvider.root)

          tree.width = "100%"
          tree.height = "100%"

          add(tree)
        }
        width = "100%"
        height = "100%"
        isSpacing = false
        isPadding = false
      })

      width = "100%"
      height = "100%"
      isSpacing = false
      isPadding = false
    }
  }
}