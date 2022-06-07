package org.dmg.dreamhubfront.page

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.html.Label
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.menubar.MenuBar
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.textfield.TextField
import org.dmg.dreamhubfront.*
import org.dmg.dreamhubfront.StandardTypes.BOOLEAN
import org.dmg.dreamhubfront.StandardTypes.DECIMAL
import org.dmg.dreamhubfront.StandardTypes.INT
import org.dmg.dreamhubfront.StandardTypes.POSITIVE
import org.dmg.dreamhubfront.StandardTypes.STRING
import org.dmg.dreamhubfront.feign.ItemApi
import org.dmg.dreamhubfront.formula.toDecimalOrNull

object Lines {
  fun toComponent(
    item: ItemTreeNode,
    editing: Boolean,
    itemApi: ItemApi,
    settingId: Long,
    refreshItem: (ItemTreeNode, Boolean) -> Unit
  ): HorizontalLayout = item
    .compacted()
    .toList()
    .map {
      toComponent(it).also {
        it.itemApi = itemApi
        it.itemApi = itemApi
        it.settingId = settingId
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

  private fun toComponent(node: ItemTreeNode): EditableLine = when (node) {
    is MainItemDtoTreeNode -> MainItemDtoLine(node)
    is ReferenceItemDtoTreeNode -> ReferenceLine(node)
    is ItemDtoTreeNode -> ItemDtoLine(node)
    is ValueNode -> when (node.types().first()) {
      STRING -> StringLine(node, "25em")
      POSITIVE -> StringLine(node, "25em") { it.toIntOrNull()?.takeIf { it > 0L }?.toString() ?: "" }
      INT -> StringLine(node, "25em") { it.toIntOrNull()?.toString() ?: "" }
      DECIMAL -> StringLine(node, "25em") { it.toDecimalOrNull()?.toString() ?: "" }
      BOOLEAN -> BooleanLine(node)
      else -> throw UnsupportedOperationException("Unknown type ${node.types().first()}")
    }
    is MetadataNode -> MetadataLine(node)
    is ItemNameNode -> ItemNameLine(node)
    else -> when {
      node.isSingle() -> RefLine(node)
      else -> MultiRefLine(node)
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
  var settingId: Long = -1
  lateinit var itemApi: ItemApi
  lateinit var refreshItem: (ItemTreeNode, Boolean) -> Unit

  open fun getElements(editing: Boolean): List<LineElement> = listOf()
}

class StringLine(private val item: ItemTreeNode, private val editWidth: String, private val validator: (String) -> String = { it }) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val initial = (item.getAsPrimitive() as String?) ?: ""
    return if (editing) {
      val editField = TextField().apply {
        value = initial
        width = editWidth

        addValueChangeListener { item.setAsPrimitive(validator(it.value)) }
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

class MetadataLine(private val item: MetadataNode) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val names = (this.itemApi.getAllTypes(settingId) + StandardTypes.ALL)
      .associate { it.id to it.name }

    return if (editing) {
      val editType = ComboBox<Long>().apply {
        setItems(names.keys.toList())
        setItemLabelGenerator { names[it] }

        value = item.getAsPrimitive().typeId
        width = "25em"

        addValueChangeListener {
          item.setAsPrimitive(item.getAsPrimitive().apply { typeId = it.value })
        }
      }
      val isSingle = Checkbox("Один").apply {
        value = item.getAsPrimitive().isSingle

        addValueChangeListener {
          item.setAsPrimitive(item.getAsPrimitive().apply { isSingle = it.value })
        }
      }
      val allowCreate = Checkbox("Вложенные").apply {
        value = item.getAsPrimitive().allowCreate

        addValueChangeListener {
          item.setAsPrimitive(item.getAsPrimitive().apply { allowCreate = it.value })
        }
      }
      val allowReference = Checkbox("Референс").apply {
        value = item.getAsPrimitive().allowReference

        addValueChangeListener {
          item.setAsPrimitive(item.getAsPrimitive().apply { allowReference = it.value })
        }
      }
      val isRequired = Checkbox("Обязательное").apply {
        value = item.getAsPrimitive().isRequired

        addValueChangeListener {
          item.setAsPrimitive(item.getAsPrimitive().apply { isRequired = it.value })
        }
      }
      val removeButton = Button(Icon(VaadinIcon.CLOSE)) {
        item.parent!!.remove(item)
        refreshItem(item.parent, true)
      }

      listOf(StringLineElement(item.name()), ComponentLineElement(editType, isSingle, allowCreate, allowReference, isRequired, removeButton))
    } else {
      names[item.getAsPrimitive().typeId]
        ?.let { listOf(StringLineElement("${item.name()}: $it")) }
        ?: listOf(StringLineElement("${item.name()}: ---"))
    }
  }
}

class ItemNameLine(private val item: ItemTreeNode) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    return if (editing) {
      val removeButton = Button(Icon(VaadinIcon.CLOSE)) {
        item.parent!!.remove(item)
        refreshItem(item.parent, true)
      }
      listOf(StringLineElement("${item.name()}"), ComponentLineElement(removeButton))
    } else {
      listOf(StringLineElement("${item.name()}"))
    }
  }
}

class ReferenceLine(private val item: ItemTreeNode) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val name = item.getAsPrimitive() as String
    return if (editing) {
      val removeButton = Button(Icon(VaadinIcon.CLOSE)) {
        item.parent!!.remove(item)
        refreshItem(item.parent, true)
      }
      listOf(StringLineElement(name), ComponentLineElement(removeButton))
    } else {
      listOf(StringLineElement(name))
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
      val removeButton = Button(Icon(VaadinIcon.CLOSE)) {
        item.parent!!.remove(item)
        refreshItem(item.parent, true)
      }
      listOf(ComponentLineElement(editField, removeButton))
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
        EditDialog("Новый атрибут", "") { attributeName ->
          item.add(ItemName().also { it.name = attributeName })
          refreshItem(item, true)
        }.open()
      }

      listOf(ComponentLineElement(editField, addButton))
    } else {
      listOf(StringLineElement(name))
    }
  }
}

open class RefLine(private val item: ItemTreeNode) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val name = item.name()?.let { "$it:" } ?: ""

    return if (editing && canAdd()) {
      val addButton = if (item.allowAdd()) {
        Button(Icon(VaadinIcon.PLUS)) {
          OptionSelection(
            itemApi,
            item.types(),
            settingId
          ) {
            item.add(it)
            refreshItem(item, true)
          }.open()
        }
      } else {
        null
      }
      val createButton = if (item.allowNested()) {
        if (item.types().size == 1) {
          Button(Icon(VaadinIcon.MAGIC)) {
            item.create(ItemName().apply { id = item.types()[0].id })
            refreshItem(item, true)
          }
        } else {
          MenuBar().apply {
            val button = addItem(Icon(VaadinIcon.MAGIC))
            item.types().forEach { type ->
              button.subMenu.addItem(type.name) {
                item.create(type)
                refreshItem(item, true)
              }
            }
          }
        }
      } else {
        null
      }
      if (addButton != null) {
        if (createButton != null) {
          listOf(StringLineElement(name), ComponentLineElement(addButton, createButton))
        } else {
          listOf(StringLineElement(name), ComponentLineElement(addButton))
        }
      } else {
        if (createButton != null) {
          listOf(StringLineElement(name), ComponentLineElement(createButton))
        } else {
          listOf(StringLineElement(name))
        }
      }
    } else {
      listOf(StringLineElement(name))
    }
  }

  open fun canAdd() = !item.hasChildren()
}

class MultiRefLine(item: ItemTreeNode) : RefLine(item) {
  override fun canAdd() = true
}