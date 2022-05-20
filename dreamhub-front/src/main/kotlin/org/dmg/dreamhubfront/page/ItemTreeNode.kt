package org.dmg.dreamhubfront.page

import org.dmg.dreamhubfront.*
import org.dmg.dreamhubfront.StandardTypes.STRING
import org.dmg.dreamhubfront.StandardTypes.TYPE

abstract class ItemTreeNode(
  val parent: ItemTreeNode?
) {
  abstract fun name(): String?
  open fun rate(): String? = null

  abstract fun hasChildren(): Boolean
  abstract fun children(): List<ItemTreeNode>
  abstract fun count(): Int
  abstract fun canCompact(): Boolean

  open fun add(value: ItemName): Unit = throw UnsupportedOperationException()
  open fun remove(node: ItemTreeNode): Unit = throw UnsupportedOperationException()
  open fun replace(value: ItemName): Unit = throw UnsupportedOperationException()
  open fun type(): List<ItemName> = throw UnsupportedOperationException()
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
    while (node.canCompact()) {
      node = node.children()[0]
      result += node
    }
    return result
  }
}

class ItemDtoTreeNode(
  private var itemDto: AbstractItemDto,
  private val itemController: ItemController,
  parent: ItemTreeNode?
) : ItemTreeNode(parent) {
  fun id(): Long = itemDto.id

  override fun name() = null

  override fun rate() = itemDto.rate

  override fun hasChildren(): Boolean = true

  override fun children(): List<ItemTreeNode> {
    val children = mutableListOf<ItemTreeNode>()

    when (val dto = itemDto) {
      is ItemDto -> {
        if (parent == null) {
          children.add(FormulaNode(dto, itemController, this))
          children.add(IsTypeNode(dto, itemController, this))
          children.add(AllowedExtensionsNode(dto, itemController, this))
        }
      }
    }
    children.add(ExtendsNode(itemDto, itemController, this))
    itemDto
      .getMetadata()
      .forEach {
        when {
          it.type.id < -1 -> PrimitiveAttributeNode(itemDto, itemController, it, this)
          else -> ItemAttributeNode(itemDto, itemController, it, this)
        }.let { children.add(it) }
      }
    itemDto
      .attributes
      .asSequence()
      .filter { it.type.id == TYPE.id }
      .forEach { children.add(PrimitiveAttributeNode(itemDto, itemController, it, this)) }

    return children
  }

  override fun count(): Int =
    when (itemDto) {
      is ItemDto -> when (parent) {
        null -> 3
        else -> 0
      }
      else -> 0
    } + 1 + itemDto.getMetadata().count() + itemDto.attributes.asSequence().filter { it.type.id == TYPE.id }.count()

  override fun canCompact() = count() == 1

  override fun type() = listOf(STRING, TYPE)

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

  override fun canCompact() = false

  override fun type() = listOf(type)
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

class ExtendsNode(
  private val itemDto: AbstractItemDto,
  private val itemController: ItemController,
  parent: ItemTreeNode
) : ItemTreeNode(parent) {
  override fun name(): String = "Основа"

  override fun hasChildren(): Boolean = itemDto.extends.isNotEmpty()

  override fun children(): List<ItemTreeNode> =
    itemDto.extends.asSequence().mapNotNull { it.item }.map { ItemDtoTreeNode(it, itemController, this) }.toList()

  override fun count() = itemDto.extends.size

  override fun canCompact(): Boolean = false

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

  override fun type(): List<ItemName> = itemDto.allowedExtensions()
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

  override fun canCompact(): Boolean = false

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

  override fun type() = listOf(TYPE)
}

class ItemNameNode(private val itemName: ItemName, parent: ItemTreeNode) : ItemTreeNode(parent) {
  fun id() = itemName.id

  override fun name() = itemName.name

  override fun hasChildren(): Boolean = false

  override fun children(): List<ItemTreeNode> = listOf()

  override fun count(): Int = 0

  override fun canCompact(): Boolean = false

}

class PrimitiveAttributeNode(
  private val itemDto: AbstractItemDto,
  private val itemController: ItemController,
  private val attributeDto: AttributeDto,
  parent: ItemTreeNode
) : ValueNode(attributeDto.name, attributeDto.type.toItemName(), parent) {
  override fun getAsPrimitive() = attributeDto.values.firstOrNull()?.primitive

  override fun setAsPrimitive(newValue: Any?) {
    when (newValue) {
      is String -> {
        val new = ValueDto().apply { primitive = newValue }
        if (attributeDto.values.isEmpty()) {
          itemController.addAttributeValue(itemDto.id, itemDto.nestedId(), attributeDto.name, new)
          attributeDto.values.add(new)
        } else {
          itemController.modifyAttributeValue(itemDto.id, itemDto.nestedId(), attributeDto.name, 0, new)
          attributeDto.values.set(0, new)
        }
      }
      null -> {
        if (attributeDto.values.isNotEmpty()) {
          itemController.removeAttributeValue(itemDto.id, itemDto.nestedId(), attributeDto.name, 0)
          attributeDto.values.clear()
        }
      }
    }
  }
}

class ItemAttributeNode(
  private val itemDto: AbstractItemDto,
  private val itemController: ItemController,
  private val attributeDto: AttributeDto,
  parent: ItemTreeNode
) : ItemTreeNode(parent) {
  override fun name() = attributeDto.name

  override fun hasChildren() = attributeDto.values.isNotEmpty()

  override fun children(): List<ItemTreeNode> = attributeDto.values.mapNotNull {
    it.terminal?.item?.let { ItemDtoTreeNode(it, itemController, this) }
      ?: it.nested?.let { ItemDtoTreeNode(it, itemController, this) }
  }

  override fun count(): Int = attributeDto.values.size

  override fun canCompact() = attributeDto.type.isSingle

  override fun add(value: ItemName) {
    //todo
  }

  override fun remove(node: ItemTreeNode) {
    //todo
  }

  override fun replace(value: ItemName) {
    //todo
  }

  override fun type(): List<ItemName> {
    TODO()
  }

  override fun allowNested(): Boolean {
    TODO()
  }
}