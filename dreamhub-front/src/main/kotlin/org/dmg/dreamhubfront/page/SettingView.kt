package org.dmg.dreamhubfront.page

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.contextmenu.ContextMenu
import com.vaadin.flow.component.grid.GridVariant
import com.vaadin.flow.component.html.Label
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.treegrid.TreeGrid
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import org.dmg.dreamhubfront.ItemController
import org.dmg.dreamhubfront.ItemDto
import org.dmg.dreamhubfront.ItemListDto
import org.dmg.dreamhubfront.TypeDto
import javax.annotation.security.PermitAll

@Route("settings/:settingId")
@PermitAll
class SettingView(
  private val itemController: ItemController,
  private val itemsTreeDataProviderService: ItemsTreeDataProviderService
) : VerticalLayout(), BeforeEnterObserver {
  var settingId: Long = -1

  override fun beforeEnter(event: BeforeEnterEvent?) {
    if (event != null) {
      set(event.routeParameters["settingId"].get().toLong())
    }
  }

  private fun set(settingId: Long) {
    if (settingId != this.settingId) {

      val types = itemController.getAllTypes(settingId)
      val dataProvider = itemsTreeDataProviderService(settingId)

      TreeGrid<ItemListView>().also { tree ->
        tree.addComponentHierarchyColumn { item ->
          val line: Component = when {
            item.isFolder -> HorizontalLayout().apply {
              add(Icon(VaadinIcon.FOLDER_O))
              add(Label(item.name))

              width = "100%"
              isPadding = false
            }
            else -> Label(item.name)
          }

          line.apply { contextMenu(item, types, dataProvider) }
        }.also {
          it.isAutoWidth = true
        }
        tree.contextMenu(null, types, dataProvider)
        tree.setDataProvider(dataProvider)
        tree.addThemeVariants(GridVariant.LUMO_COMPACT)

        add(tree)
      }

      this.settingId = settingId
    }
  }

  private fun Component.contextMenu(item: ItemListView?, types: List<TypeDto>, dataProvider: ItemsTreeDataProvider) {
    ContextMenu().also {
      when {
        item == null -> it.addItem("Добавить в корень")
        item.isFolder -> it.addItem("Добавить")
        else -> null
      }?.also {
        it.subMenu.addItem("Создать...") {
          itemController.add(ItemDto().also {
            it.settingId = settingId
            it.path = item?.fullName ?: ""
          }).also {
            dataProvider.add(it.toListDto())
          }
        }
        types.forEach { type ->

        }
      }

      it.target = this
    }
  }
}

fun ItemDto.toListDto() = ItemListDto().also {
  it.id = id
  it.name = name
  it.path = path
  it.settingId = settingId
}