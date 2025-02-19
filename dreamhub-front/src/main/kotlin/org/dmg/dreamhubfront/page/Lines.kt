package org.dmg.dreamhubfront.page

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Html
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Label
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.menubar.MenuBar
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.RouteConfiguration
import com.vaadin.flow.router.RouteParameters
import org.dmg.dreamhubfront.*
import org.dmg.dreamhubfront.StandardTypes.BOOLEAN
import org.dmg.dreamhubfront.StandardTypes.DECIMAL
import org.dmg.dreamhubfront.StandardTypes.INT
import org.dmg.dreamhubfront.StandardTypes.POSITIVE
import org.dmg.dreamhubfront.StandardTypes.STRING
import org.dmg.dreamhubfront.feign.ItemApi
import org.dmg.dreamhubfront.formula.toDecimalOrNull
import org.dmg.dreamhubfront.page.Lines.checkPositive

object Lines {
  fun toComponent(
    item: ItemTreeNode,
    editing: Boolean,
    itemApi: ItemApi,
    settingId: Long,
    refreshItem: (ItemTreeNode, Boolean) -> Unit,
    refreshAll: () -> Unit,
  ): HorizontalLayout = item
    .compacted()
    .toList()
    .map {
      toComponent(it).also {
        it.itemApi = itemApi
        it.itemApi = itemApi
        it.settingId = settingId
        it.refreshItem = refreshItem
        it.refreshAll = refreshAll
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
    is SettingItemTreeNode -> SettingItemDtoLine(node)
    is ReferenceItemDtoTreeNode -> ReferenceLine(node)
    is ReferenceSettingItemTreeNode -> ReferenceSettingItemDtoLine(node)
    is ItemDtoTreeNode -> ItemDtoLine(node)
    is ValueNode -> when (node.types().first()) {
      STRING -> StringLine(node, "35em")
      POSITIVE -> StringLine(node, "6em") { it.checkPositive() }
      INT -> StringLine(node, "6em") { it.checkInt() }
      DECIMAL -> StringLine(node, "6em") { it.checkDecimal() }
      BOOLEAN -> BooleanLine(node)
      else -> throw UnsupportedOperationException("Unknown type ${node.types().first()}")
    }

    is ValuesTreeNode -> ListRefLine(node)
    is MetadataNode -> MetadataLine(node)
    is SettingMemberListTreeNode -> SettingMemberListLine(node)
    is SettingMemberTreeNode -> SettingMemberLine(node)
    else -> RefLine(node)
  }

  private fun String.modifier() = when {
    startsWith("(") && endsWith(")") -> substring(1, length - 1)
    else -> this
  }

  private fun String.checkPositive() = modifier().toIntOrNull()?.takeIf { it >= 0L }.let { this } ?: ""

  private fun String.checkInt() = modifier().toIntOrNull()?.let { this } ?: ""

  private fun String.checkDecimal() = modifier().toDecimalOrNull()?.let { this } ?: ""
}

interface LineElement {
  fun concat(right: LineElement): List<LineElement>
  fun toComponents(): List<Component>
}

class ComponentLineElement : LineElement {
  private val list: List<Component>

  constructor(vararg components: Component) {
    list = components.toList()
  }

  constructor(components: List<Component>) {
    list = components
  }

  override fun concat(right: LineElement): List<LineElement> = when (right) {
    is ComponentLineElement -> listOf(ComponentLineElement(*(list + right.list).toTypedArray()))
    else -> listOf(this, right)
  }

  override fun toComponents(): List<Component> = list
}

class StringLineElement(private val value: String, private val readOnly: Boolean = false) : LineElement {
  override fun concat(right: LineElement): List<LineElement> = when {
    right is StringLineElement && readOnly == right.readOnly -> listOf(StringLineElement("$value ${right.value}", readOnly))
    else -> listOf(this, right)
  }

  override fun toComponents(): List<Component> = when {
    value.isBlank() -> listOf()
    readOnly -> listOf(Html("<em>$value </em>"))
    else -> listOf(Label(value))
  }
}

open class EditableLine {
  var settingId: Long = -1
  lateinit var itemApi: ItemApi
  lateinit var refreshItem: (ItemTreeNode, Boolean) -> Unit
  lateinit var refreshAll: () -> Unit

  open fun getElements(editing: Boolean): List<LineElement> = listOf()

  protected fun arrowButtons(item: ItemTreeNode) = when (item.parent) {
    is MovableItem -> {
      val upButton = Button(Icon(VaadinIcon.ARROW_UP)) {
        item.parent.moveUp(item)
        refreshItem(item.parent, true)
      }
      val downButton = Button(Icon(VaadinIcon.ARROW_DOWN)) {
        item.parent.moveDown(item)
        refreshItem(item.parent, true)
      }
      listOf(upButton, downButton)
    }

    else -> listOf()
  }
}

class StringLine(private val item: ItemTreeNode, private val editWidth: String, private val validator: (String) -> String = { it }) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val (initial, default) = when (val i = item.getAsPrimitive()) {
      is String -> i to ""
      is Pair<*, *> -> (i.first?.toString() ?: "") to (i.second?.toString() ?: "")
      else -> "" to ""
    }
    return if (editing && !item.readOnly) {
      val editField = TextField().apply {
        value = initial
        placeholder = default
        width = editWidth

        addValueChangeListener { item.setAsPrimitive(validator(it.value)) }
      }

      listOf(StringLineElement("${item.name()}: "), ComponentLineElement(editField))
    } else {
      if (initial.isBlank()) {
        listOf(StringLineElement("${item.name()}: ", item.readOnly), StringLineElement(default, true))
      } else {
        listOf(StringLineElement("${item.name()}: $initial", item.readOnly))
      }
    }
  }
}

class BooleanLine(private val item: ValueNode) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val (initial, _) = when (val i = item.getAsPrimitive()) {
      is Boolean -> i to false
      is Pair<*, *> -> (i.first?.toString()?.toBoolean() ?: false) to (i.second?.toString()?.toBoolean() ?: false)
      else -> false to false
    }
    return if (editing) {
      val editField = Checkbox().apply {
        value = initial

        addValueChangeListener { item.setAsPrimitive(it.value) }
      }

      listOf(StringLineElement("${item.name()}: "), ComponentLineElement(editField))
    } else {
      listOf(StringLineElement("${item.name()}: ${if (initial) "Да" else "Нет"}", item.readOnly))
    }
  }
}

class MetadataLine(private val item: MetadataNode) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val names = (this.itemApi.getAllTypes(settingId).sortedBy { it.name } + StandardTypes.ALL)
      .associate { it.id to it.name }

    return if (editing && !item.readOnly) {
      val editType = ComboBox<Long>().apply {
        setItems(names.keys.toList())
        setItemLabelGenerator { names[it] }

        value = names[item.getAsPrimitive().typeId]?.let { item.getAsPrimitive().typeId } ?: -1
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
      val id = names[item.getAsPrimitive().typeId]?.let { item.getAsPrimitive().typeId } ?: -1L
      names[id]
        ?.let { listOf(StringLineElement(item.name()), ComponentLineElement(nameButton(it, id, settingId, item.readOnly))) }
        ?: listOf(StringLineElement("${item.name()}: ---", item.readOnly))
    }
  }
}

class ReferenceLine(private val item: ItemTreeNode) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val name = item.getAsPrimitive() as String
    return if (editing && !item.readOnly) {
      val removeButton = Button(Icon(VaadinIcon.CLOSE)) {
        item.parent!!.remove(item)
        refreshItem(item.parent, true)
      }
      val buttons = listOf(nameButton(name, item.id()!!, settingId, false), removeButton) + arrowButtons(item)

      listOf(ComponentLineElement(buttons))
    } else {
      listOf(ComponentLineElement(listOf(nameButton(name, item.id()!!, settingId, item.readOnly))))
    }
  }
}

class ItemDtoLine(private val item: ItemTreeNode) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val name = item.getAsPrimitive() as String
    return if (editing && !item.readOnly) {
      val removeButton = Button(Icon(VaadinIcon.CLOSE)) {
        item.parent!!.remove(item)
        refreshItem(item.parent, true)
      }
      val editButton = Button(Icon(VaadinIcon.EDIT)) {
        EditDialog("Переименовать", name) {
          item.setAsPrimitive(it)
          refreshItem(item, true)
        }.open()
      }
      val nameComponent = nameButton(name, item.id()!!, settingId, false)
      val button = listOf(nameComponent, editButton, removeButton) + arrowButtons(item)
      listOf(ComponentLineElement(button))
    } else {
      listOf(ComponentLineElement(listOf(nameButton(name, item.id()!!, settingId, item.readOnly))))
    }
  }
}

class MainItemDtoLine(private val item: ItemTreeNode) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val name = item.getAsPrimitive() as String
    return if (editing && !item.readOnly) {
      val editField = TextField().apply {
        value = name
        width = "25em"

        addValueChangeListener {
          item.setAsPrimitive(it.value)
        }
      }
      val addButton = Button(Icon(VaadinIcon.PLUS)) {
        EditDialog("Новый атрибут", "", true) { attributeName ->
          item.add(ItemName().also { it.name = attributeName })
          refreshItem(item, true)
        }.open()
      }

      listOf(ComponentLineElement(editField, addButton))
    } else {
      listOf(StringLineElement(name, item.readOnly))
    }
  }
}

open class RefLine(private val item: ItemTreeNode) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val name = item.name()?.let { "$it:" } ?: ""

    return if (editing && !item.readOnly) {
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

      val buttons = listOf(addButton, createButton).mapNotNull { it }
      when {
        buttons.isNotEmpty() -> listOf(StringLineElement(name), ComponentLineElement(buttons))
        else -> listOf(StringLineElement(name))
      }
    } else {
      listOf(StringLineElement(name, item.readOnly))
    }
  }
}

class SettingItemDtoLine(private val item: ItemTreeNode) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val name = item.getAsPrimitive() as String
    return if (editing && !item.readOnly) {
      val editField = TextField().apply {
        value = name
        width = "25em"

        addValueChangeListener {
          item.setAsPrimitive(it.value)
        }
      }

      listOf(ComponentLineElement(editField))
    } else {
      listOf(StringLineElement(name, item.readOnly))
    }
  }
}

class ReferenceSettingItemDtoLine(private val item: ItemTreeNode) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val name = item.getAsPrimitive() as String
    return if (editing && !item.readOnly) {
      val removeButton = Button(Icon(VaadinIcon.CLOSE)) {
        item.parent!!.remove(item)
        refreshItem(item.parent, true)
      }
      val buttons = listOf(nameButton(name, -2, settingId, false), removeButton)

      listOf(ComponentLineElement(buttons))
    } else {
      listOf(ComponentLineElement(listOf(nameButton(name, -2, settingId, item.readOnly))))
    }
  }
}

class SettingMemberListLine(private val item: ItemTreeNode) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val name = item.name()?.let { "$it:" } ?: ""
    return if (editing && !item.readOnly) {
      val addButton = Button(Icon(VaadinIcon.PLUS)) {
        EditDialog("Email", "@gmail.com") {
          item.add(ItemName().apply { this.name = it })
          refreshItem(item, true)
        }.open()
      }
      listOf(StringLineElement(name), ComponentLineElement(listOf(addButton)))
    } else {
      listOf(StringLineElement(name, item.readOnly))
    }
  }
}

class SettingMemberLine(private val item: ItemTreeNode) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val initial = item.getAsPrimitive() as UserRoleType

    return if (editing && !item.readOnly) {
      val editField = ComboBox<UserRoleType>().apply {
        setItems(UserRoleType.values().toList())
        value = initial
        addValueChangeListener {
          item.setAsPrimitive(it.value)
        }
      }
      val removeButton = Button(Icon(VaadinIcon.CLOSE)) {
        item.parent!!.remove(item)
        refreshItem(item.parent, true)
      }

      listOf(StringLineElement("${item.name()}: "), ComponentLineElement(editField, removeButton))
    } else {
      listOf(StringLineElement("${item.name()}: $initial", item.readOnly))
    }
  }
}

class ListRefLine(private val item: ItemTreeNode) : EditableLine() {
  override fun getElements(editing: Boolean): List<LineElement> {
    val name = item.name()?.let { "$it:" } ?: ""
    val initial = item.getAsPrimitive() as List<ItemName>

    return if (editing && !item.readOnly) {
      val addButton = if (item.allowAdd()) {
        Button(Icon(VaadinIcon.PLUS)) {
          OptionSelection(
            itemApi,
            item.types(),
            settingId
          ) {
            item.setAsPrimitive(it to null)
            refreshItem(item, true)
            refreshAll()
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
      val buttons = listOf(addButton, createButton).mapNotNull { it }

      val list = initial.map { element ->
        ComponentLineElement(
          nameButton(element.name, element.id, settingId, item.readOnly),
          Button(Icon(VaadinIcon.CLOSE)) {
            item.setAsPrimitive(null to element)
            refreshItem(item, true)
            refreshAll()
          }
        )
      }

      if (buttons.isNotEmpty()) {
        listOf(StringLineElement(name)) + list + listOf(ComponentLineElement(buttons))
      } else {
        listOf(StringLineElement(name)) + list
      }
    } else {
      listOf(StringLineElement(name)) + listOf(ComponentLineElement(initial.map { nameButton(it.name, it.id, settingId, item.readOnly) }))
    }
  }
}

private fun nameButton(name: String, id: Long, settingId: Long, readOnly: Boolean): Component {
  val routeConfiguration = RouteConfiguration.forSessionScope()
  val parameters = RouteParameters(
    mutableMapOf(
      "settingId" to settingId.toString(),
      "itemId" to id.toString()
    )
  )
  val url = routeConfiguration.getUrl(SettingView::class.java, parameters)
  return Anchor(url, name).also {
    if (readOnly) {
      it.style["font-style"] = "italic"
    }
  }
}
