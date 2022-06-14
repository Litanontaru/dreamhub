package org.dmg.dreamhubfront.page

import org.dmg.dreamhubfront.*
import org.dmg.dreamhubfront.StandardTypes.STRING
import org.dmg.dreamhubfront.StandardTypes.TYPE
import org.dmg.dreamhubfront.feign.ItemApi
import org.dmg.dreamhubfront.formula.NanDecimal
import org.dmg.dreamhubfront.formula.formula
import org.dmg.dreamhubfront.formula.rate
import org.dmg.dreamhubfront.formula.sum

abstract class ItemTreeNode(
  val parent: ItemTreeNode?,
  val readOnly: Boolean,
) {
  private var cHas: Boolean? = null
  private var cCount: Int? = null
  private var cChilddren: List<ItemTreeNode>? = null

  abstract fun name(): String?
  open fun rate(): String? = null

  fun cacheHasChildren(): Boolean = cHas ?: hasChildren().also { cHas = it }
  protected abstract fun hasChildren(): Boolean
  fun cachedChildren(): List<ItemTreeNode> = cChilddren ?: children().also { cChilddren = it }
  protected abstract fun children(): List<ItemTreeNode>
  fun cachedCount(): Int = cCount ?: count().also { cCount = it }
  protected abstract fun count(): Int

  open fun canCompact(): Boolean = false

  fun add(value: ItemName) = inAdd(value).also { cHas = null }.also { cChilddren = null }.also { cCount = null }
  fun create(value: ItemName) = inCreate(value).also { cHas = null }.also { cChilddren = null }.also { cCount = null }
  fun remove(node: ItemTreeNode) = inRemove(node).also { cHas = null }.also { cChilddren = null }.also { cCount = null }
  open fun inAdd(value: ItemName): Unit = throw UnsupportedOperationException()
  open fun inCreate(value: ItemName): Unit = throw UnsupportedOperationException()
  open fun inRemove(node: ItemTreeNode): Unit = throw UnsupportedOperationException()

  open fun types(): List<ItemName> = throw UnsupportedOperationException()
  open fun isSingle(): Boolean = true
  open fun allowNested(): Boolean = false
  open fun allowAdd(): Boolean = true

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
    while (node.canCompact() && node.hasChildren()) {
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

abstract class ItemDtoTreeNode(
  private var itemDto: AbstractItemDto,
  private val itemApi: ItemApi,
  parent: ItemTreeNode?,
  readOnly: Boolean,
) : ItemTreeNode(parent, readOnly) {
  fun id(): Long = itemDto.id

  override fun name() = itemDto.id.toString()

  override fun rate() = itemDto.rate()?.let {
    when (it) {
      is NanDecimal -> itemDto.formula()
      else -> it.toString()
    }
  }

  override fun hasChildren(): Boolean = count() > 0

  fun childrenAttributes(): List<ItemTreeNode> {
    val children = mutableListOf<ItemTreeNode>()

    val attributeDtoMap = itemDto.attributes.associate { it.name to it.showValues() }

    itemDto
      .superMetadata()
      .forEach {
        val (value, inherited) = attributeDtoMap[it.attributeName] ?: (mutableListOf<ValueDto>() to mutableListOf<ValueDto>())
        when {
          it.typeId < -1 -> PrimitiveAttributeNode(itemDto, itemApi, it, value.firstOrNull()?.primitive, inherited.firstOrNull()?.primitive, this, readOnly)
          else -> ItemAttributeNode(itemDto, itemApi, it, value, inherited, this, readOnly)
        }.let { children.add(it) }
      }

    return children
  }

  fun attributesCount(): Int = itemDto.superMetadata().count()

  override fun canCompact() = count() == 1

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

  override fun children(): List<ItemTreeNode> =
    mutableListOf(
      FormulaNode(itemDto, itemApi, this, false),
      IsTypeNode(itemDto, itemApi, this, false),
      IsFinalNode(itemDto, itemApi, this, false),
      AllowedExtensionsNode(itemDto, itemApi, this, false),
      ExtendsNode(itemDto, itemApi, this, false)
    ) +
        childrenAttributes() +
        itemDto.metadata.map { MetadataNode(itemDto, it, itemApi, this, false) }

  override fun count(): Int = 5 + attributesCount() + itemDto.metadata.size

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
  override fun count(): Int = attributesCount().let {
    when {
      itemDto.nonFinalExtends().count() > 0 -> 1 + it
      else -> it
    }
  }

  override fun children(): List<ItemTreeNode> = childrenAttributes().let {
    when {
      itemDto.nonFinalExtends().count() > 0 -> listOf(ExtendsNode(itemDto, itemApi, this, readOnly)) + it
      else -> it
    }
  }
}

class ReferenceItemDtoTreeNode(
  private val itemDto: AbstractItemDto,
  private val itemApi: ItemApi,
  index: Int,
  parent: ItemTreeNode?,
  readOnly: Boolean,
) : ValueItemDtoTreeNode(itemDto, itemApi, index, parent, readOnly) {
  override fun count(): Int = attributesCount().let {
    when {
      itemDto is ItemDto && itemDto.isFinal -> it
      else -> 1 + it
    }
  }

  override fun children(): List<ItemTreeNode> = childrenAttributes().let {
    when {
      itemDto is ItemDto && itemDto.isFinal -> it
      else -> listOf(ExtendsNode(itemDto, itemApi, this, readOnly)) + it
    }
  }
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
) : ValueNode("Это тип", StandardTypes.BOOLEAN, parent, readOnly) {
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

class IsFinalNode(
  val itemDto: ItemDto,
  val itemApi: ItemApi,
  parent: ItemTreeNode,
  readOnly: Boolean,
) : ValueNode("Финальное", StandardTypes.BOOLEAN, parent, readOnly) {
  override fun getAsPrimitive() = itemDto.isFinal

  override fun setAsPrimitive(newValue: Any?) {
    when (newValue) {
      is Boolean -> {
        itemApi.setIsFinal(itemDto.id, newValue)
        itemDto.isFinal = newValue
      }
    }
  }
}

class ExtendsNode(
  private val itemDto: AbstractItemDto,
  private val itemApi: ItemApi,
  parent: ItemTreeNode,
  readOnly: Boolean,
) : ItemTreeNode(parent, readOnly) {
  override fun name(): String = "Основа"

  private fun extends() = when (parent) {
    is MainItemDtoTreeNode -> itemDto.extendsItems()
    else -> itemDto.nonFinalExtends()
  }

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
  fun id() = itemName.id

  override fun name() = itemName.name

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
      is String -> {
        if (value == null) {
          itemApi.addAttributePrimitiveValue(itemDto.id, itemDto.nestedId, metadataDto.attributeName, newValue)
          value = newValue
        } else {
          itemApi.modifyAttributePrimitiveValue(itemDto.id, itemDto.nestedId, metadataDto.attributeName, 0, newValue)
          value = newValue
        }
      }
      null -> {
        if (value != null) {
          itemApi.removeAttributeValue(itemDto.id, itemDto.nestedId, metadataDto.attributeName, 0)
          value = null
        }
      }
    }
  }
}

class ItemAttributeNode(
  private val itemDto: AbstractItemDto,
  private val itemApi: ItemApi,
  private val metadataDto: MetadataDto,
  private val values: MutableList<ValueDto>,
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
    if (itemApi.get(value.id).isAbstract()) {
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