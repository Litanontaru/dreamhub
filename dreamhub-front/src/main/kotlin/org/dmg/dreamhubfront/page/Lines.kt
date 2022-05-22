package org.dmg.dreamhubfront.page

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Label
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.menubar.MenuBar
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import org.dmg.dreamhubfront.ItemController
import org.dmg.dreamhubfront.ItemName
import org.dmg.dreamhubfront.RefDto

object Lines {
  fun toComponent(
    item: ItemTreeNode,
    editing: Boolean,
    itemController: ItemController,
    settingId: Long,
    refreshItem: (ItemTreeNode, Boolean) -> Unit
  ): HorizontalLayout = item
    .compacted()
    .toList()
    .map {
      toComponent(it, settingId).also {
        it.itemController = itemController
        it.refreshItem = refreshItem
      }
    }
    .flatMap { it.getElements(editing) }
    .fold<LineElement, List<LineElement>>(listOf()) { result, element ->
      if (result.isEmpty()) {
        listOf(element)
      } else {
        result.subList(0, result.size - 1) + result.last().concat(element)
      }
    }
    .flatMap { it.toComponents() }
    .let {
      HorizontalLayout().apply {
        add(*it.toTypedArray())
        setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, *it.toTypedArray())

        width = "100%"
      }
    }

  private fun toComponent(node: ItemTreeNode, settingId: Long): EditableLine = when (node) {
    is MainItemDtoTreeNode -> MainItemDtoLine(node)
    is ItemDtoTreeNode -> ItemDtoLine(node)
    is IsTypeNode -> BooleanLine(node)
    is ValueNode -> StringLine(node)
    else -> when {
      node.isSingle() -> RefLine(node, settingId)
      else -> MultiRefLine(node, settingId)
    }
  }
}

interface LineElement {
  fun concat(right: LineElement): List<LineElement>
  fun toComponents(): List<Component>
}

class ComponentLineElement(vararg components: Component) : LineElement {
  private val list = components.toList()

  override fun concat(right: LineElement): List<LineElement> = when (right) {
    is ComponentLineElement -> listOf(ComponentLineElement(*(list + right.list).toTypedArray()))
    else -> listOf(this, right)
  }

  override fun toComponents(): List<Component> = list
}

class StringLineElement(private val value: String) : LineElement {
  override fun concat(right: LineElement): List<LineElement> = when (right) {
    is StringLineElement -> listOf(StringLineElement("$value ${right.value}"))
    else -> listOf(this, right)
  }

  override fun toComponents(): List<Component> = when {
    value.isBlank() -> listOf()
    else -> listOf(Label(value))
  }
}

open class EditableLine {
  lateinit var itemController: ItemController
  lateinit var refreshItem: (ItemTreeNode, Boolean) -> Unit

  open fun getElements(editing: Boolean): List<LineElement> = listOf()
}

class StringLine(private val item: ValueNode) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val initial = item.getAsPrimitive() as String?
    return if (editing) {
      val editField = TextField().apply {
        value = initial
        width = "6em"

        addValueChangeListener { item.setAsPrimitive(it.value) }
      }

      listOf(StringLineElement("${item.name()}: "), ComponentLineElement(editField))
    } else {
      listOf(StringLineElement("${item.name()}: $initial"))
    }
  }
}

class BooleanLine(private val item: ValueNode) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val initial = item.getAsPrimitive() as Boolean
    return if (editing) {
      val editField = Checkbox().apply {
        value = initial

        addValueChangeListener { item.setAsPrimitive(it.value) }
      }

      listOf(StringLineElement("${item.name()}: "), ComponentLineElement(editField))
    } else {
      listOf(StringLineElement("${item.name()}: ${if (initial) "Да" else "Нет"}"))
    }
  }
}

class ItemDtoLine(private val item: ItemTreeNode) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val name = item.getAsPrimitive() as String
    return if (editing) {
      val editField = TextField().apply {
        value = name
        width = "25em"

        addValueChangeListener {
          item.setAsPrimitive(it.value)
        }
      }
      listOf(ComponentLineElement(editField))
    } else {
      listOf(StringLineElement(name))
    }
  }
}

class MainItemDtoLine(private val item: ItemTreeNode) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val name = item.getAsPrimitive() as String
    return if (editing) {
      val editField = TextField().apply {
        value = name
        width = "25em"

        addValueChangeListener {
          item.setAsPrimitive(it.value)
        }
      }
      val addButton = Button(Icon(VaadinIcon.PLUS)) {
        NewMetadataDialog { attributeName ->
          item.add(ItemName().also { it.name = attributeName })
        }.open()
      }

      listOf(ComponentLineElement(editField, addButton))
    } else {
      listOf(StringLineElement(name))
    }
  }
}

class NewMetadataDialog(save: (String) -> Unit) : Dialog() {
  init {
    add(VerticalLayout().apply {
      val field = TextField("Новый атрибут").apply {
        value = ""
      }
      add(field)
      add(Button("Ок") {
        save(field.value)
        close()
      })
      add(Button("Отмена") { close() })
    })
  }
}

open class RefLine(
  private val item: ItemTreeNode,
  private val settingId: Long
) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val name = item.name()?.let { "$it:" } ?: ""

    return if (editing && canAdd()) {
      val addButton = Button(Icon(VaadinIcon.PLUS)) {
        OptionSelection(
          itemController,
          item.types(),
          settingId
        ) {
          item.add(it)
          refreshItem(item, true)
        }.open()
      }
      if (item.allowNested()) {
        val createButton = if (item.types().size == 1) {
          Button(Icon(VaadinIcon.MAGIC)) {
            item.createNested().apply {
              extends.add(RefDto().also { it.id = item.types()[0].id })
              item.add(this)
            }
            refreshItem(item, true)
          }
        } else {
          MenuBar().apply {
            val button = addItem(Icon(VaadinIcon.MAGIC))
            item.types().forEach { type ->
              button.subMenu.addItem(type.name) {
                item.createNested().apply {
                  extends.add(RefDto().also { it.id = type.id })
                  item.add(this)
                }
                refreshItem(item, true)
              }
            }
          }
        }
        listOf(StringLineElement(name), ComponentLineElement(addButton, createButton))
      } else {
        listOf(StringLineElement(name), ComponentLineElement(addButton))
      }
    } else {
      listOf(StringLineElement(name))
    }
  }

  open fun canAdd() = !item.hasChildren()
}

class MultiRefLine(item: ItemTreeNode, settingId: Long) : RefLine(item, settingId) {
  override fun canAdd() = true
}