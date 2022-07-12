package org.dmg.dreamhubfront.page

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.GridVariant
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.provider.DataProvider
import org.dmg.dreamhubfront.ItemListDto
import org.dmg.dreamhubfront.ItemName
import org.dmg.dreamhubfront.feign.ItemApi

class OptionSelection(
  private val itemApi: ItemApi,
  private val types: List<ItemName>,
  private val settingId: Long,
  private val onSelect: (ItemListDto) -> Unit,
) : Dialog() {
  init {
    add(VerticalLayout().apply {
      val dataProvider = DataProvider.ofCollection(itemApi.getSubItems(settingId, types.map { it.id }))

      val filter = TextField().apply {
        width = "100%"

        addValueChangeListener { event ->
          when (event.value) {
            null -> dataProvider.clearFilters()
            else -> dataProvider.setFilter { it.name.contains(event.value, true) }
          }
        }
      }

      val grid = Grid<ItemListDto>().apply {
        addColumn { it.name }
//        addColumn { it.rate }

        this.setItems(dataProvider)

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