package org.dmg.dreamhubfront.page

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.GridVariant
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import org.dmg.dreamhubfront.ItemController
import org.dmg.dreamhubfront.ItemListDto
import org.dmg.dreamhubfront.ItemName

class OptionSelection(
  private val itemController: ItemController,
  private val types: List<ItemName>,
  private val settingId: Long,
  private val onSelect: (ItemListDto) -> Unit
) : Dialog() {
  init {
    add(VerticalLayout().apply {
      val items = itemController.getSubItems(settingId, types.map { it.id })

      val filter = TextField().apply {
        width = "100%"

        addValueChangeListener {
//          dataProvider.filter = it.value
        }
      }

      val grid = Grid<ItemListDto>().apply {
        addColumn { it.name }
//        addColumn { it.rate }

        setItems(items)

        addThemeVariants(GridVariant.LUMO_NO_BORDER)
      }

      add(filter)
      add(grid)
      add(HorizontalLayout().apply {
        add(Button("Принять") {
          grid
            .selectedItems
            .singleOrNull()
            ?.let { onSelect(it) }
          close()
        })
        add(Button("Закрыть") { close() })
      })


      width = "100%"
      height = "100%"
      isPadding = false
      isSpacing = false
    })

    width = "100%"
    height = "100%"
  }
}