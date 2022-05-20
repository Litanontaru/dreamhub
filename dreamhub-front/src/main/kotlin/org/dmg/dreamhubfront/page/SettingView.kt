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

@Route("settings/:settingId/:itemId")
@PermitAll
class SettingView(
  private val itemController: ItemController,
  private val itemsTreeDataProviderService: ItemsTreeDataProviderService,
  private val itemTreeDataProviderService: ItemTreeDataProviderService
) : HorizontalLayout(), BeforeEnterObserver {
  var settingId: Long = -1
  var itemId: Long = -1

  init {
    width = "100%"
    height = "100%"
    isPadding = false
  }

  override fun beforeEnter(event: BeforeEnterEvent?) {
    if (event != null) {
      set(event.routeParameters["settingId"].get().toLong())
    }
  }

  private fun set(settingId: Long) {
    if (settingId != this.settingId) {
      val view = ItemView(itemController, itemTreeDataProviderService)

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
        tree.addItemClickListener {
          it.item.item?.let { view.itemId = it.id }
        }

        tree.width = "100%"
        tree.height = "100%"

        add(tree)
      }

      add(view)

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
          NewItemDialog { name ->
            itemController.add(ItemDto().also {
              it.name = name
              it.settingId = settingId
              it.path = item?.fullName ?: ""
            }).also {
              dataProvider.add(it.toListDto())
            }
          }.open()
        }
        types.forEach { type ->
          //todo
        }
      }
      if (item != null) {
        if (item.isFolder) {
          it.addItem("Удалить папку") {
            //todo
          }
        } else {
          it.addItem("Удалить") {
            itemController.remove(item.item!!.id)
          }
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