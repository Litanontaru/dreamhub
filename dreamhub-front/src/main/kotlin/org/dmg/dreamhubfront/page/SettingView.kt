package org.dmg.dreamhubfront.page

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.grid.GridVariant
import com.vaadin.flow.component.html.Label
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.treegrid.TreeGrid
import com.vaadin.flow.function.SerializablePredicate
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import org.dmg.dreamhubfront.ItemController
import org.dmg.dreamhubfront.ItemDto
import org.dmg.dreamhubfront.ItemListDto
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
      val view = ItemView(itemController, itemTreeDataProviderService, settingId)

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

          line/*.apply { contextMenu(item, types, dataProvider) }*/
        }.also {
          it.isAutoWidth = true
        }

        tree.addContextMenu().apply {
          addItem("Создать...") {
            val path = it.item.map { it.fullName }.orElse("")
            val template = if (path.isBlank()) "" else "$path."

            EditDialog("Название", template) { fullName ->
              itemController.add(ItemDto().also {
                it.name = fullName.substring(fullName.lastIndexOf('.') + 1)
                it.settingId = settingId
                it.path = fullName.substring(0, fullName.lastIndexOf('.'))
              }).also {
                dataProvider.add(it.toListDto())
                dataProvider.refreshAll()
              }
            }.open()
          }

          val delete = addItem("Удалить") {
            val item = it.item.get()
            if (item.isFolder) {
              //todo
            } else {
              itemController.remove(item.item!!.id)
              dataProvider.refreshAll()
            }
          }

          val move = addItem("Переместить") {
            val item = it.item.get()
            EditDialog("Путь", item.path) { newPath ->
              item.item?.let {
                itemController.setPath(it.id, newPath)
                it.path = newPath
                dataProvider.refreshAll()
              }
            }.open()
          }

          dynamicContentHandler = SerializablePredicate {
            delete.isVisible = it != null
            move.isVisible = it != null && !it.isFolder

            true
          }
        }

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
}

fun ItemDto.toListDto() = ItemListDto().also {
  it.id = id
  it.name = name
  it.path = path
  it.settingId = settingId
}