package org.dmg.dreamhubfront.page

import org.dmg.dreamhubfront.*
import org.dmg.dreamhubfront.StandardTypes.STRING
import org.dmg.dreamhubfront.StandardTypes.TYPE
import org.dmg.dreamhubfront.formula.NanDecimal
import org.dmg.dreamhubfront.formula.formula
import org.dmg.dreamhubfront.formula.rate

abstract class ItemTreeNode(
  val parent: ItemTreeNode?
) {
  abstract fun name(): String?
  open fun rate(): String? = null

  abstract fun hasChildren(): Boolean
  abstract fun children(): List<ItemTreeNode>
  abstract fun count(): Int
  open fun canCompact(): Boolean = false

  open fun add(value: ItemName): Unit = throw UnsupportedOperationException()
  open fun remove(node: ItemTreeNode): Unit = throw UnsupportedOperationException()
  open fun replace(value: ItemName): Unit = throw UnsupportedOperationException()
  open fun createNested(): NestedItemDto = throw UnsupportedOperationException()
  open fun types(): List<ItemName> = throw UnsupportedOperationException()
  open fun isSingle(): Boolean = true
  open fun allowNested(): Boolean = false

  open fun getAsPrimitive(): Any? = throw UnsupportedOperationException()
  open fun setAsPrimitive(newValue: Any?): Unit = throw UnsupportedOperationException()

  fun last(): ItemTreeNode {
    var node = this
    while (node.canCompact()) {
      node = node.children()[0]
    }
    return node
  }

  fun compacted(): Sequence<ItemTreeNode> {
    var node = this
    var result = sequenceOf(node)
    while (node.canCompact() && node.hasChildren()) {
      try {
        node = node.children()[0]
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
  private val itemController: ItemController,
  parent: ItemTreeNode?
) : ItemTreeNode(parent) {
  fun id(): Long = itemDto.id

  override fun name() = null

  override fun rate() = itemDto.rate()?.let {
    when (it) {
      is NanDecimal -> itemDto.formula()
      else -> it.toString()
    }
  }

  override fun hasChildren(): Boolean = count() > 0

  fun childrenAttributes(): List<ItemTreeNode> {
    val children = mutableListOf<ItemTreeNode>()

    val attributeDtoMap = itemDto.attributes.associate { it.name to it.values }

    itemDto
      .getMetadata()
      .forEach {
        when {
          it.typeId < -1 -> PrimitiveAttributeNode(itemDto, itemController, it, attributeDtoMap[it.attributeName] ?: mutableListOf(), this)
          else -> ItemAttributeNode(itemDto, itemController, it, attributeDtoMap[it.attributeName] ?: mutableListOf(), this)
        }.let { children.add(it) }
      }

    return children
  }

  fun attributesCount(): Int = itemDto.getMetadata().count()

  override fun canCompact() = count() == 1

  override fun types() = listOf(TYPE)

  override fun getAsPrimitive() = itemDto.name

  override fun setAsPrimitive(newValue: Any?) {
    when (newValue) {
      is String -> {
        itemController.setName(id = itemDto.id, newName = newValue)
        itemDto.name = newValue
      }
      is ItemDto -> {
        itemDto = newValue
      }
    }
  }

  override fun createNested(): NestedItemDto = when (val item = itemDto) {
    is ItemDto -> NestedItemDto().apply {
      id = item.id
      nestedId = item.nextNestedId
      item.nextNestedId = item.nextNestedId + 1
    }
    else -> throw IllegalStateException()
  }
}

class MainItemDtoTreeNode(
  private var itemDto: ItemDto,
  private val itemController: ItemController,
) : ItemDtoTreeNode(itemDto, itemController, null) {

  override fun children(): List<ItemTreeNode> =
    mutableListOf(
      FormulaNode(itemDto, itemController, this),
      IsTypeNode(itemDto, itemController, this),
      IsFinalNode(itemDto, itemController, this),
      AllowedExtensionsNode(itemDto, itemController, this),
      ExtendsNode(itemDto, itemController, this)
    ) +
        childrenAttributes() +
        itemDto.metadata.map { MetadataNode(itemDto, it, itemController, this) }

  override fun count(): Int = 5 + attributesCount() + itemDto.metadata.size

  override fun add(value: ItemName) {
    val metadataDto = MetadataDto().apply { attributeName = value.name }
    itemController.addMetadata(itemDto.id, metadataDto)
    itemDto.metadata.add(metadataDto)
  }

  override fun remove(node: ItemTreeNode) {
    when (node) {
      is MetadataNode -> {
        itemController.removeMetadata(itemDto.id, node.name())
        itemDto.metadata.removeIf { it.attributeName == node.name() }
      }
    }
  }
}

open class ValueItemDtoTreeNode(
  private val itemDto: AbstractItemDto,
  private val itemController: ItemController,
  val index: Int,
  parent: ItemTreeNode?
) : ItemDtoTreeNode(itemDto, itemController, parent) {
  override fun count(): Int = attributesCount().let {
    when {
      itemDto.nonFinalExtends().count() > 0 -> 1 + it
      else -> it
    }
  }

  override fun children(): List<ItemTreeNode> = childrenAttributes().let {
    when {
      itemDto.nonFinalExtends().count() > 0 ->
        listOf(ExtendsNode(itemDto, itemController, this)) + it
      else -> it
    }
  }
}

class ReferenceItemDtoTreeNode(
  private val itemDto: AbstractItemDto,
  private val itemController: ItemController,
  index: Int,
  parent: ItemTreeNode?
) : ValueItemDtoTreeNode(itemDto, itemController, index, parent) {
  override fun count(): Int = attributesCount().let {
    when {
      itemDto is ItemDto && itemDto.isFinal -> it
      else -> 1 + it
    }
  }

  override fun children(): List<ItemTreeNode> = childrenAttributes().let {
    when {
      itemDto is ItemDto && itemDto.isFinal -> it
      else -> listOf(ExtendsNode(itemDto, itemController, this)) + it
    }
  }
}

class MetadataNode(
  private val itemDto: ItemDto,
  private var metadataDto: MetadataDto,
  private val itemController: ItemController,
  parent: ItemTreeNode
) : ItemTreeNode(parent) {
  override fun name(): String = metadataDto.attributeName

  override fun hasChildren(): Boolean = false

  override fun children(): List<ItemTreeNode> = listOf()

  override fun count(): Int = 0

  override fun getAsPrimitive() = metadataDto

  override fun setAsPrimitive(newValue: Any?) {
    when (newValue) {
      is MetadataDto -> {
        itemController.modifyMetadata(itemDto.id, newValue)
        itemDto.metadata[itemDto.metadata.indexOfFirst { it.attributeName == newValue.attributeName }] = newValue
      }
    }
  }
}

abstract class ValueNode(
  private val name: String,
  private val type: ItemName,
  parent: ItemTreeNode
) : ItemTreeNode(parent) {
  override fun name() = name

  override fun hasChildren(): Boolean = false

  override fun children(): List<ItemTreeNode> = listOf()

  override fun count() = 0

  override fun types() = listOf(type)
}

class FormulaNode(
  val itemDto: ItemDto,
  val itemController: ItemController,
  parent: ItemTreeNode
) : ValueNode("Формула", STRING, parent) {
  override fun getAsPrimitive() = itemDto.formula

  override fun setAsPrimitive(newValue: Any?) {
    when (newValue) {
      is String -> {
        itemController.setFormula(itemDto.id, newValue)
        itemDto.formula = newValue
      }
    }
  }
}

class IsTypeNode(
  val itemDto: ItemDto,
  val itemController: ItemController,
  parent: ItemTreeNode
) : ValueNode("Это тип", StandardTypes.BOOLEAN, parent) {
  override fun getAsPrimitive() = itemDto.isType

  override fun setAsPrimitive(newValue: Any?) {
    when (newValue) {
      is Boolean -> {
        itemController.setIsType(itemDto.id, newValue)
        itemDto.isType = newValue
      }
    }
  }
}

class IsFinalNode(
  val itemDto: ItemDto,
  val itemController: ItemController,
  parent: ItemTreeNode
) : ValueNode("Финальное", StandardTypes.BOOLEAN, parent) {
  override fun getAsPrimitive() = itemDto.isFinal

  override fun setAsPrimitive(newValue: Any?) {
    when (newValue) {
      is Boolean -> {
        itemController.setIsFinal(itemDto.id, newValue)
        itemDto.isFinal = newValue
      }
    }
  }
}

class ExtendsNode(
  private val itemDto: AbstractItemDto,
  private val itemController: ItemController,
  parent: ItemTreeNode
) : ItemTreeNode(parent) {
  override fun name(): String = "Основа"

  private fun extends() = when (parent) {
    is MainItemDtoTreeNode -> itemDto.extends.mapNotNull { it.item }
    else -> itemDto.nonFinalExtends()
  }

  override fun hasChildren(): Boolean = extends().isNotEmpty()

  override fun children(): List<ItemTreeNode> =
    extends()
      .withIndex()
      .map { ReferenceItemDtoTreeNode(it.value, itemController, it.index, this) }.toList()

  override fun count() = extends().count()

  override fun add(value: ItemName) {
    itemController
      .addExtends(itemDto.id, itemDto.nestedId(), value.id)
      .let { parent?.setAsPrimitive(it) }
  }

  override fun remove(node: ItemTreeNode) {
    when (node) {
      is ItemDtoTreeNode -> {
        itemController
          .removeExtends(itemDto.id, itemDto.nestedId(), node.id())
          .let { parent?.setAsPrimitive(it) }
      }
    }
  }

  override fun isSingle(): Boolean = false

  override fun types(): List<ItemName> = itemDto.allowedExtensions()
}

class AllowedExtensionsNode(
  private val itemDto: ItemDto,
  private val itemController: ItemController,
  parent: ItemTreeNode
) : ItemTreeNode(parent) {
  override fun name(): String = "Разрешены основы"

  override fun hasChildren(): Boolean = itemDto.allowedExtensions.isNotEmpty()

  override fun children(): List<ItemTreeNode> = itemDto.allowedExtensions.map { ItemNameNode(it, this) }

  override fun count(): Int = itemDto.allowedExtensions.size

  override fun add(value: ItemName) {
    itemController.addAllowedExtensions(itemDto.id, value.id)
    itemDto.allowedExtensions.add(value)
  }

  override fun remove(node: ItemTreeNode) {
    when (node) {
      is ItemNameNode -> {
        itemController.removeAllowedExtensions(itemDto.id, node.id())
        itemDto.allowedExtensions.removeIf { it.id == node.id() }
      }
    }
  }

  override fun types() = listOf(TYPE)

  override fun isSingle(): Boolean = false
}

class ItemNameNode(private val itemName: ItemName, parent: ItemTreeNode) : ItemTreeNode(parent) {
  fun id() = itemName.id

  override fun name() = itemName.name

  override fun hasChildren(): Boolean = false

  override fun children(): List<ItemTreeNode> = listOf()

  override fun count(): Int = 0
}

class PrimitiveAttributeNode(
  private val itemDto: AbstractItemDto,
  private val itemController: ItemController,
  private val metadataDto: MetadataDto,
  private val values: MutableList<ValueDto>,
  parent: ItemTreeNode
) : ValueNode(metadataDto.attributeName, metadataDto.toItemName(), parent) {
  override fun getAsPrimitive() = values.firstOrNull()?.primitive

  override fun setAsPrimitive(newValue: Any?) {
    when (newValue) {
      is String -> {
        val new = ValueDto().apply { primitive = newValue }
        if (values.isEmpty()) {
          itemController.addAttributeValue(itemDto.id, itemDto.nestedId(), metadataDto.attributeName, new)
          values.add(new)
        } else {
          itemController.modifyAttributeValue(itemDto.id, itemDto.nestedId(), metadataDto.attributeName, 0, new)
          values.set(0, new)
        }
      }
      null -> {
        if (values.isNotEmpty()) {
          itemController.removeAttributeValue(itemDto.id, itemDto.nestedId(), metadataDto.attributeName, 0)
          values.clear()
        }
      }
    }
  }
}

class ItemAttributeNode(
  private val itemDto: AbstractItemDto,
  private val itemController: ItemController,
  private val metadataDto: MetadataDto,
  private val values: MutableList<ValueDto>,
  parent: ItemTreeNode
) : ItemTreeNode(parent) {
  override fun name() = metadataDto.attributeName

  override fun hasChildren() = values.isNotEmpty()

  override fun children(): List<ItemTreeNode> = values.withIndex().mapNotNull { value ->
    value.value.terminal?.item?.let { ReferenceItemDtoTreeNode(it, itemController, value.index, this) }
      ?: value.value.nested?.let { ValueItemDtoTreeNode(it, itemController, value.index, this) }
  }

  override fun count(): Int = values.size

  override fun canCompact() = metadataDto.isSingle

  override fun add(value: ItemName) {
    when (value) {
      is NestedItemDto -> ValueDto().apply { nested = value }
      else -> ValueDto().apply {
        terminal = RefDto().apply {
          id = value.id
          item = itemController.get(id)
        }
      }
    }.let {
      itemController
        .addAttributeValue(itemDto.id, itemDto.nestedId(), metadataDto.attributeName, it)
      values.add(it)
    }
  }

  override fun remove(node: ItemTreeNode) {
    when (node) {
      is ValueItemDtoTreeNode -> {
        itemController.removeAttributeValue(itemDto.id, itemDto.nestedId(), metadataDto.attributeName, node.index)
        values.removeAt(node.index)
      }
    }
  }

  override fun replace(value: ItemName) {
    when (value) {
      is NestedItemDto -> ValueDto().apply { nested = value }
      else -> ValueDto().apply { terminal = RefDto().apply { id = value.id } }
    }.let {
      itemController.modifyAttributeValue(itemDto.id, itemDto.nestedId(), metadataDto.attributeName, 0, it)
      values[0] = it
    }
  }

  override fun createNested(): NestedItemDto =
    generateSequence(parent) { it.parent }.last().createNested()

  override fun types(): List<ItemName> = listOf(ItemName().apply { id = metadataDto.typeId })

  override fun isSingle(): Boolean = metadataDto.isSingle

  override fun allowNested(): Boolean = metadataDto.allowCreate
}