package org.dmg.dreamhubfront.page

import org.dmg.dreamhubfront.*
import org.dmg.dreamhubfront.StandardTypes.BOOLEAN
import org.dmg.dreamhubfront.StandardTypes.STRING
import org.dmg.dreamhubfront.StandardTypes.TYPE
import org.dmg.dreamhubfront.feign.ItemApi
import org.dmg.dreamhubfront.formula.NanDecimal
import org.dmg.dreamhubfront.formula.formula
import org.dmg.dreamhubfront.formula.rate
import org.dmg.dreamhubfront.formula.sum

fun <T> track(title: String, action: () -> T): T {
//  val nanoTime = System.nanoTime()
  val result = action()
//  println("$title\t${System.nanoTime() - nanoTime}")
  return result
}

abstract class ItemTreeNode(
  val parent: ItemTreeNode?,
  val readOnly: Boolean,
) {
  private var cHas: Boolean? = null
  private var cCount: Int? = null
  protected var cChildren: List<ItemTreeNode>? = null

  abstract fun name(): String?
  open fun rate(): String? = null
  open fun id(): Long? = null

  fun cacheHasChildren(): Boolean = cHas ?: hasChildren().also { cHas = it }
  protected abstract fun hasChildren(): Boolean
  fun cachedChildren(): List<ItemTreeNode> = cChildren ?: children().also { cChildren = it }
  protected abstract fun children(): List<ItemTreeNode>
  fun cachedCount(): Int = cCount ?: count().also { cCount = it }
  protected abstract fun count(): Int

  open fun canCompact(): Boolean = false

  fun add(value: ItemName) = inAdd(value).also { cHas = null }.also { cChildren = null }.also { cCount = null }
  fun create(value: ItemName) = inCreate(value).also { cHas = null }.also { cChildren = null }.also { cCount = null }
  fun remove(node: ItemTreeNode) = inRemove(node).also { cHas = null }.also { cChildren = null }.also { cCount = null }
  open fun inAdd(value: ItemName): Unit = throw UnsupportedOperationException()
  open fun inCreate(value: ItemName): Unit = throw UnsupportedOperationException()
  open fun inRemove(node: ItemTreeNode): Unit = throw UnsupportedOperationException()

  open fun types(): List<ItemName> = throw UnsupportedOperationException()
  open fun isSingle(): Boolean = true
  open fun allowNested(): Boolean = false
  open fun allowAdd(): Boolean = true
  open fun allowMove(): Boolean = false

  open fun getAsPrimitive(): Any? = throw UnsupportedOperationException()
  open fun setAsPrimitive(newValue: Any?): Unit = throw UnsupportedOperationException()

  fun last(): ItemTreeNode {
    var node = this
    while (node.canCompact()) {
      node = node.cachedChildren()[0]
    }
    return node
  }

  fun compacted(): Sequence<ItemTreeNode> {
    var node = this
    var result = sequenceOf(node)
    while (node.canCompact() && node.cacheHasChildren()) {
      try {
        node = node.cachedChildren()[0]
        result += node
      } catch (e: Exception) {
        println()
      }
    }
    return result
  }
}

interface MovableItem {
  fun moveUp(node: ItemTreeNode)
  fun moveDown(node: ItemTreeNode)
}

abstract class ItemDtoTreeNode(
  private var itemDto: AbstractItemDto,
  private val itemApi: ItemApi,
  parent: ItemTreeNode?,
  readOnly: Boolean,
) : ItemTreeNode(parent, readOnly) {
  override fun name() = itemDto.id.toString()
  override fun id(): Long = itemDto.id

  override fun rate() = track("rate\t${itemDto.id}/${itemDto.nestedId}") {
    itemDto.rate()?.let {
      when (it) {
        is NanDecimal -> itemDto.formula()
        else -> it.toString()
      }
    }
  }

  override fun hasChildren(): Boolean = cachedCount() > 0

  override fun count(): Int = cachedChildren().size

  fun childrenAttributes(): Sequence<ItemTreeNode> {
    val attributeDtoMap = itemDto.attributes.associate { it.name to it.showValues() }
    return itemDto
      .superMetadata()
      .map {
        val (value, inherited) = attributeDtoMap[it.attributeName] ?: (mutableListOf<ValueDto>() to mutableListOf())
        when {
          it.typeId < -1 -> PrimitiveAttributeNode(itemDto, itemApi, it, value.firstOrNull()?.primitive, inherited.firstOrNull()?.primitive, this, readOnly)
          it.isSingle -> ItemAttributeNode(itemDto, itemApi, it, value, inherited, this, readOnly)
          else -> MultipleItemAttributeNode(itemDto, itemApi, it, value, inherited, this, readOnly)
        }
      }
  }

  override fun canCompact() = cachedCount() == 1

  override fun types() = listOf(TYPE)

  override fun getAsPrimitive() = itemDto.name

  override fun setAsPrimitive(newValue: Any?) {
    when (newValue) {
      is String -> {
        itemApi.setName(itemDto.id, itemDto.nestedId, "'$newValue'")
        itemDto.name = newValue
      }

      is ItemDto -> {
        itemDto = newValue
      }
    }
  }
}

class MainItemDtoTreeNode(
  private var itemDto: ItemDto,
  private val itemApi: ItemApi,
) : ItemDtoTreeNode(itemDto, itemApi, null, false) {

  override fun children(): List<ItemTreeNode> {
    return track("all\t${itemDto.id}/${itemDto.nestedId}") {
      val children = track("generic\t${itemDto.id}/${itemDto.nestedId}") {
        mutableListOf(
          FormulaNode(itemDto, itemApi, this, false),
          IsTypeNode(itemDto, itemApi, this, false),
          DescriptionNode(itemDto, itemApi, this, false),
          GroupsNode(itemDto, itemApi, this, false),
          TypeNode(itemDto, itemApi, this, false),
          AllowedExtensionsNode(itemDto, itemApi, this, false),
        )
      }
      if (itemDto.comboAllowedExtensions().isNotEmpty()) {
        children += ExtensionNode(itemDto, itemApi, this, false)
      }
      val childrenAttributes = track("attributes\t${itemDto.id}/${itemDto.nestedId}") { childrenAttributes() }
      val metadataNodes = track("metadata\t${itemDto.id}/${itemDto.nestedId}") { itemDto.metadata.asSequence().map { MetadataNode(itemDto, it, itemApi, this, false) } }

      track("toList\t${itemDto.id}/${itemDto.nestedId}") { (children + childrenAttributes + metadataNodes).toList() }
    }
  }

  override fun inAdd(value: ItemName) {
    val metadataDto = MetadataDto().apply { attributeName = value.name }
    itemApi.addMetadata(itemDto.id, metadataDto)
    itemDto.metadata.add(metadataDto)
  }

  override fun inRemove(node: ItemTreeNode) {
    when (node) {
      is MetadataNode -> {
        itemApi.removeMetadata(itemDto.id, node.name())
        itemDto.metadata.removeIf { it.attributeName == node.name() }
      }
    }
  }
}

open class ValueItemDtoTreeNode(
  private val itemDto: AbstractItemDto,
  private val itemApi: ItemApi,
  val index: Int,
  parent: ItemTreeNode?,
  readOnly: Boolean,
) : ItemDtoTreeNode(itemDto, itemApi, parent, readOnly) {
  override fun children(): List<ItemTreeNode> {
    return when {
      itemDto.comboAllowedExtensions().isEmpty() -> childrenAttributes()
      else -> {
        sequenceOf(ExtensionNode(itemDto, itemApi, this, false)) + childrenAttributes()
      }
    }.toList()
  }
}

class ReferenceItemDtoTreeNode(
  itemDto: AbstractItemDto,
  itemApi: ItemApi,
  index: Int,
  parent: ItemTreeNode?,
  readOnly: Boolean,
) : ValueItemDtoTreeNode(itemDto, itemApi, index, parent, readOnly) {
  override fun children(): List<ItemTreeNode> = childrenAttributes().toList()
}

class MetadataNode(
  private val itemDto: ItemDto,
  private var metadataDto: MetadataDto,
  private val itemApi: ItemApi,
  parent: ItemTreeNode,
  readOnly: Boolean,
) : ItemTreeNode(parent, readOnly) {
  override fun name(): String = metadataDto.attributeName

  override fun hasChildren(): Boolean = false

  override fun children(): List<ItemTreeNode> = listOf()

  override fun count(): Int = 0

  override fun getAsPrimitive() = metadataDto

  override fun setAsPrimitive(newValue: Any?) {
    when (newValue) {
      is MetadataDto -> {
        itemApi.modifyMetadata(itemDto.id, newValue)
        itemDto.metadata[itemDto.metadata.indexOfFirst { it.attributeName == newValue.attributeName }] = newValue
      }
    }
  }
}

abstract class ValueNode(
  private val name: String,
  private val type: ItemName,
  parent: ItemTreeNode,
  readOnly: Boolean,
) : ItemTreeNode(parent, readOnly) {
  override fun name() = name

  override fun hasChildren(): Boolean = false

  override fun children(): List<ItemTreeNode> = listOf()

  override fun count() = 0

  override fun types() = listOf(type)
}

class FormulaNode(
  val itemDto: ItemDto,
  val itemApi: ItemApi,
  parent: ItemTreeNode,
  readOnly: Boolean,
) : ValueNode("Формула", STRING, parent, readOnly) {
  override fun getAsPrimitive() = itemDto.formula

  override fun setAsPrimitive(newValue: Any?) {
    when (newValue) {
      is String -> {
        itemApi.setFormula(itemDto.id, newValue)
        itemDto.formula = newValue
      }
    }
  }
}

class IsTypeNode(
  val itemDto: ItemDto,
  val itemApi: ItemApi,
  parent: ItemTreeNode,
  readOnly: Boolean,
) : ValueNode("Это тип", BOOLEAN, parent, readOnly) {
  override fun getAsPrimitive() = itemDto.isType

  override fun setAsPrimitive(newValue: Any?) {
    when (newValue) {
      is Boolean -> {
        itemApi.setIsType(itemDto.id, newValue)
        itemDto.isType = newValue
      }
    }
  }
}

class DescriptionNode(
  val itemDto: ItemDto,
  val itemApi: ItemApi,
  parent: ItemTreeNode,
  readOnly: Boolean,
) : ValueNode("Описание", STRING, parent, readOnly) {
  override fun getAsPrimitive() = itemDto.description

  override fun setAsPrimitive(newValue: Any?) {
    when (newValue) {
      is String -> {
        itemApi.setDescription(itemDto.id, newValue)
        itemDto.description = newValue
      }
    }
  }
}

class GroupsNode(
  val itemDto: ItemDto,
  val itemApi: ItemApi,
  parent: ItemTreeNode,
  readOnly: Boolean,
) : ValueNode("Группы", STRING, parent, readOnly) {
  override fun getAsPrimitive() = itemDto.groups

  override fun setAsPrimitive(newValue: Any?) {
    when (newValue) {
      is String -> {
        itemApi.setGroup(itemDto.id, newValue)
        itemDto.groups = newValue
      }
    }
  }
}

abstract class ExtendsNode(
  private val name: String,
  private val itemDto: AbstractItemDto,
  private val itemApi: ItemApi,
  parent: ItemTreeNode,
  readOnly: Boolean,
) : ItemTreeNode(parent, readOnly) {
  override fun name(): String = name
  override fun id(): Long = itemDto.id

  protected fun innerExtends() = when (parent) {
    is MainItemDtoTreeNode -> itemDto.extendsItems()
    else -> sequenceOf()
  }

  protected abstract fun extends(): Sequence<ItemDto>

  override fun hasChildren(): Boolean = extends().any()

  override fun children(): List<ItemTreeNode> =
    extends()
      .withIndex()
      .map { ReferenceItemDtoTreeNode(it.value, itemApi, it.index, this, readOnly) }.toList()

  override fun count() = extends().count()

  override fun inAdd(value: ItemName) {
    itemApi
      .addExtends(itemDto.id, itemDto.nestedId, value.id)
      .let { parent?.setAsPrimitive(it) }
  }

  override fun inRemove(node: ItemTreeNode) {
    when (node) {
      is ItemDtoTreeNode -> {
        itemApi
          .removeExtends(itemDto.id, itemDto.nestedId, node.id())
          .let { parent?.setAsPrimitive(it) }
      }
    }
  }

  override fun isSingle(): Boolean = false
}

class TypeNode(
  private val itemDto: AbstractItemDto,
  itemApi: ItemApi,
  parent: ItemTreeNode,
  readOnly: Boolean,
) : ExtendsNode("Тип", itemDto, itemApi, parent, readOnly) {
  override fun extends() = itemDto.extendsItems().filter { it.isType }

  override fun types() = listOf(TYPE)
}

class ExtensionNode(
  private val itemDto: AbstractItemDto,
  itemApi: ItemApi,
  parent: ItemTreeNode,
  readOnly: Boolean,
) : ExtendsNode("Расширение", itemDto, itemApi, parent, readOnly) {
  override fun extends(): Sequence<ItemDto> = itemDto.extendsItems().toList().filter { !it.isType }.asSequence()

  override fun types(): List<ItemName> = itemDto.comboAllowedExtensions()
}

class AllowedExtensionsNode(
  private val itemDto: ItemDto,
  private val itemApi: ItemApi,
  parent: ItemTreeNode,
  readOnly: Boolean,
) : ItemTreeNode(parent, readOnly) {
  override fun name(): String = "Разрешены основы"

  override fun hasChildren(): Boolean = itemDto.allowedExtensions.isNotEmpty()

  override fun children(): List<ItemTreeNode> = itemDto.allowedExtensions.map { ItemNameNode(it, this, readOnly) }

  override fun count(): Int = itemDto.allowedExtensions.size

  override fun inAdd(value: ItemName) {
    itemApi.addAllowedExtensions(itemDto.id, value.id)
    itemDto.allowedExtensions.add(value)
  }

  override fun inRemove(node: ItemTreeNode) {
    when (node) {
      is ItemNameNode -> {
        itemApi.removeAllowedExtensions(itemDto.id, node.id())
        itemDto.allowedExtensions.removeIf { it.id == node.id() }
      }
    }
  }

  override fun types() = listOf(TYPE)

  override fun isSingle(): Boolean = false
}

class ItemNameNode(private val itemName: ItemName, parent: ItemTreeNode, readOnly: Boolean) : ItemTreeNode(parent, readOnly) {
  override fun name() = itemName.name
  override fun id(): Long = itemName.id

  override fun hasChildren(): Boolean = false

  override fun children(): List<ItemTreeNode> = listOf()

  override fun count(): Int = 0
}

class PrimitiveAttributeNode(
  private val itemDto: AbstractItemDto,
  private val itemApi: ItemApi,
  private val metadataDto: MetadataDto,
  private var value: String?,
  private var inherited: String?,
  parent: ItemTreeNode,
  readOnly: Boolean,
) : ValueNode(metadataDto.attributeName, metadataDto.toItemName(), parent, readOnly) {
  override fun getAsPrimitive() = value to inherited

  override fun setAsPrimitive(newValue: Any?) {
    when (newValue) {
      is String -> setAsString(newValue)
      is Boolean -> setAsString(newValue.toString())
      null -> {
        if (value != null) {
          itemApi.removeAttributeValue(itemDto.id, itemDto.nestedId, metadataDto.attributeName, 0)
          value = null
        }
      }
    }
  }

  private fun setAsString(newValue: String) {
    if (value == null) {
      itemApi.addAttributePrimitiveValue(itemDto.id, itemDto.nestedId, metadataDto.attributeName, newValue)
    } else {
      itemApi.modifyAttributePrimitiveValue(itemDto.id, itemDto.nestedId, metadataDto.attributeName, 0, newValue)
    }
    value = newValue
  }
}

open class ItemAttributeNode(
  protected val itemDto: AbstractItemDto,
  protected val itemApi: ItemApi,
  protected val metadataDto: MetadataDto,
  protected val values: MutableList<ValueDto>,
  private val inherited: MutableList<ValueDto>,
  parent: ItemTreeNode,
  readOnly: Boolean,
) : ItemTreeNode(parent, readOnly) {
  override fun name() = metadataDto.attributeName

  override fun rate() = (values + inherited).mapNotNull { it.item()?.rate() }.sum().toString()

  override fun hasChildren() =
    values.isNotEmpty() || inherited.isNotEmpty()

  fun hasOwnValue() = values.isNotEmpty()

  override fun children(): List<ItemTreeNode> =
    values.withIndex().mapNotNull { valueToNode(it.value, it.index, readOnly) } + inherited.mapNotNull { valueToNode(it, -2, true) }

  private fun valueToNode(v: ValueDto, i: Int, readOnly: Boolean) =
    (v.terminal?.item?.let { ReferenceItemDtoTreeNode(it, itemApi, i, this, readOnly) }
      ?: v.nested?.let { ValueItemDtoTreeNode(it, itemApi, i, this, readOnly) })


  override fun count(): Int = values.size + inherited.size

  override fun canCompact() = metadataDto.isSingle

  override fun inAdd(value: ItemName) {
    if (itemApi[value.id].isAbstract()) {
      inCreate(value)
    } else {
      itemApi.addAttributeTerminalValue(itemDto.id, itemDto.nestedId, metadataDto.attributeName, value.id).also { values.add(it) }
    }
  }

  override fun inCreate(value: ItemName) {
    itemApi.addAttributeNestedValue(itemDto.id, itemDto.nestedId, metadataDto.attributeName, value.id).also { values.add(it) }
  }

  override fun inRemove(node: ItemTreeNode) {
    when (node) {
      is ValueItemDtoTreeNode -> {
        itemApi.removeAttributeValue(itemDto.id, itemDto.nestedId, metadataDto.attributeName, node.index)
        values.removeAt(node.index)
      }
    }
  }

  override fun types(): List<ItemName> = listOf(ItemName().apply { id = metadataDto.typeId })

  override fun isSingle(): Boolean = metadataDto.isSingle

  override fun allowNested(): Boolean = metadataDto.allowCreate && !(isSingle() && hasOwnValue())

  override fun allowAdd(): Boolean = metadataDto.allowReference && !(isSingle() && hasOwnValue())
}

class MultipleItemAttributeNode(
  itemDto: AbstractItemDto,
  itemApi: ItemApi,
  metadataDto: MetadataDto,
  values: MutableList<ValueDto>,
  inherited: MutableList<ValueDto>,
  parent: ItemTreeNode,
  readOnly: Boolean,
) : ItemAttributeNode(itemDto, itemApi, metadataDto, values, inherited, parent, readOnly), MovableItem {
  override fun moveUp(node: ItemTreeNode) {
    when (node) {
      is ValueItemDtoTreeNode -> {
        if (node.index > 0 && node.index < values.size) {
          itemApi.moveAttributeUp(itemDto.id, itemDto.nestedId, metadataDto.attributeName, node.index)
          values.swap(node.index, node.index - 1)
          cChildren = null
        }
      }
    }
  }

  override fun moveDown(node: ItemTreeNode) {
    when (node) {
      is ValueItemDtoTreeNode -> {
        if (node.index < values.size - 1) {
          itemApi.moveAttributeDown(itemDto.id, itemDto.nestedId, metadataDto.attributeName, node.index)
          values.swap(node.index, node.index + 1)
          cChildren = null
        }
      }
    }
  }
}

class SettingItemTreeNode(
  private val settingDto: SettingDto,
  private val settingController: SettingController,
  parent: ItemTreeNode?,
  readOnly: Boolean,
) : ItemTreeNode(parent, readOnly) {
  override fun name() = settingDto.id.toString()
  override fun id(): Long = settingDto.id

  override fun hasChildren(): Boolean = cachedCount() > 0

  override fun count(): Int = cachedChildren().size

  override fun canCompact() = cachedCount() == 1

  override fun types() = listOf(TYPE)

  override fun getAsPrimitive() = settingDto.name

  override fun setAsPrimitive(newValue: Any?) {
    when (newValue) {
      is String -> {
        settingController.setName(settingDto.id, newValue)
        settingDto.name = newValue
      }
    }
  }

  override fun children(): List<ItemTreeNode> {
    val children = mutableListOf(
      SettingDescriptionNode(settingDto, settingController, this, false),
      SettingDependencyNode(settingDto, settingController, this, false)
    )
    return children
  }
}

class SettingDescriptionNode(
  val settingDto: SettingDto,
  val settingController: SettingController,
  parent: ItemTreeNode,
  readOnly: Boolean,
) : ValueNode("Описание", STRING, parent, readOnly) {
  override fun getAsPrimitive() = settingDto.description

  override fun setAsPrimitive(newValue: Any?) {
    when (newValue) {
      is String -> {
        settingController.setDescription(settingDto.id, newValue)
        settingDto.description = newValue
      }
    }
  }
}

class SettingDependencyNode(
  private val settingDto: SettingDto,
  private val settingController: SettingController,
  parent: ItemTreeNode,
  readOnly: Boolean,
) : ItemTreeNode(parent, readOnly) {
  override fun name(): String = "Зависимости"
  override fun id(): Long = settingDto.id

  override fun hasChildren(): Boolean = settingDto.dependencies.isNotEmpty()

  override fun children(): List<ItemTreeNode> =
    settingDto.dependencies
      .withIndex()
      .map { ReferenceSettingItemTreeNode(it.value, this, readOnly) }.toList()

  override fun count() = settingDto.dependencies.count()

  override fun inAdd(value: ItemName) {
    settingController
      .addDependency(settingDto.id, value.id)
      .let { settingDto.dependencies.add(SettingListDto().apply { id = value.id; name = value.name }) }
  }

  override fun inRemove(node: ItemTreeNode) {
    when (node) {
      is ReferenceSettingItemTreeNode -> {
        settingController
          .removeDependency(settingDto.id, node.id()!!)
          .let { parent?.setAsPrimitive(it) }
      }
    }
  }

  override fun isSingle(): Boolean = false
}

class ReferenceSettingItemTreeNode(
  private val settingListDto: SettingListDto,
  parent: ItemTreeNode?,
  readOnly: Boolean,
) : ItemTreeNode(parent, readOnly) {
  override fun name() = settingListDto.id.toString()
  override fun getAsPrimitive() = settingListDto.name

  override fun hasChildren(): Boolean = false

  override fun children(): List<ItemTreeNode> = listOf()

  override fun count(): Int = 0
}