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
import org.dmg.dreamhubfront.ItemDto
import org.dmg.dreamhubfront.ItemListDto
import org.dmg.dreamhubfront.feign.ItemApi
import javax.annotation.security.PermitAll

@Route("settings/:settingId/:itemId")
@PermitAll
class SettingView(
  private val itemApi: ItemApi,
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
      val view = ItemView(itemApi, itemTreeDataProviderService, settingId)

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
              itemApi.add(ItemDto().also {
                it.name = fullName.substring(fullName.lastIndexOf('.') + 1)
                it.settingId = settingId
                it.path = when {
                  fullName.lastIndexOf('.') > 0 -> fullName.substring(0, fullName.lastIndexOf('.'))
                  else -> ""
                }
              }).also {
                dataProvider.add(it.toListDto())
                dataProvider.refreshAll()
              }
            }.open()
          }

          val copy = addItem("Копировать") {
            val item = it.item.get()
            if (!item.isFolder) {
              itemApi.copy(item.item!!.id)
            }
          }

          val delete = addItem("Удалить") {
            val item = it.item.get()
            if (item.isFolder) {
              //todo
            } else {
              itemApi.remove(item.item!!.id)
              dataProvider.refreshAll()
            }
          }

          val move = addItem("Переместить") {
            val item = it.item.get()
            EditDialog("Путь", item.path) { newPath ->
              item.item?.let {
                itemApi.setPath(it.id, newPath)
                it.path = newPath
                dataProvider.refreshAll()
              }
            }.open()
          }

          val renameFolder = addItem("Переименовать папку") {
            val item = it.item.get()
            EditDialog("Имя папки", item.name) { newName ->
              val length = item.path.length + 1 + item.name.length
              val newPath = item.path + "." + newName

              fun recursiveChildren(subItem : ItemListView): Sequence<ItemListDto> {
                return (if (subItem.isFolder) {
                  dataProvider.children(subItem)?.flatMap { recursiveChildren(it) }?.asSequence()
                } else {
                  subItem.item?.let { sequenceOf(it) }
                })
                  ?: emptySequence()
              }

              recursiveChildren(item)
                .forEach {
                  itemApi.setPath(it.id, newPath + it.path.substring(length))
                  it.path = newName
                }

              item.name = newName
            }.open()
          }

          dynamicContentHandler = SerializablePredicate {
            copy.isVisible = it != null && !it.isFolder
            delete.isVisible = it != null
            move.isVisible = it != null && !it.isFolder
            renameFolder.isVisible =  it != null && it.isFolder

            true
          }
        }

        tree.setDataProvider(dataProvider)
        tree.addThemeVariants(GridVariant.LUMO_COMPACT)
        tree.addItemClickListener {
          it.item.item?.let { view.itemId = it.id }
        }

        tree.width = "30%"
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
  it.rank = rank
}