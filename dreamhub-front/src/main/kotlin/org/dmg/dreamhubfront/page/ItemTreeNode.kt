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
    while (node.canCompact()) {
      node = node.children()[0]
      result += node
    }
    return result
  }
}

open class ItemDtoTreeNode(
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

    children.add(ExtendsNode(itemDto, itemController, this))
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

  override fun count(): Int = 1 + itemDto.getMetadata().count()

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
): ItemDtoTreeNode(itemDto, itemController, null) {
  override fun children(): List<ItemTreeNode> {
    val children = mutableListOf<ItemTreeNode>()
    children.add(FormulaNode(itemDto, itemController, this))
    children.add(IsTypeNode(itemDto, itemController, this))
    children.add(AllowedExtensionsNode(itemDto, itemController, this))

    children += super.children()

    itemDto.metadata.forEach {
      children.add(MetadataField(itemDto, it, itemController, this))
    }

    return children
  }

  override fun count(): Int = 3 + super.count() + itemDto.metadata.size

  override fun add(value: ItemName) {
    val metadataDto = MetadataDto().apply { attributeName = value.name }
    itemController.addMetadata(itemDto.id, metadataDto)
    itemDto.metadata.add(metadataDto)
  }

  override fun remove(node: ItemTreeNode) {
    when (node) {
      is MetadataField -> {
        itemController.removeMetadata(itemDto.id, node.name())
        itemDto.metadata.removeIf {it.attributeName == node.name()}
      }
    }
  }
}

class MetadataField(
  private val itemDto: ItemDto,
  private var metadataDto: MetadataDto,
  private val itemController: ItemController,
  parent: ItemTreeNode
): ItemTreeNode(parent) {
  override fun name(): String = metadataDto.attributeName

  override fun hasChildren(): Boolean = false

  override fun children(): List<ItemTreeNode> = listOf()

  override fun count(): Int  = 0

  override fun canCompact(): Boolean = false

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

  override fun canCompact() = false

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

  override fun types() = listOf(TYPE)
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

  override fun children(): List<ItemTreeNode> = values.mapNotNull {
    it.terminal?.item?.let { ItemDtoTreeNode(it, itemController, this) }
      ?: it.nested?.let { ItemDtoTreeNode(it, itemController, this) }
  }

  override fun count(): Int = values.size

  override fun canCompact() = metadataDto.isSingle

  override fun add(value: ItemName) {
    when (value) {
      is NestedItemDto -> ValueDto().apply { nested = value }
      else -> ValueDto().apply { terminal = RefDto().apply { id = value.id } }
    }.let {
      itemController
        .addAttributeValue(itemDto.id, itemDto.nestedId(), metadataDto.attributeName, it)
      values.add(it)
    }
  }

  override fun remove(node: ItemTreeNode) {
    val indexOf = children().indexOf(node)
    itemController.removeAttributeValue(itemDto.id, itemDto.nestedId(), metadataDto.attributeName, indexOf)
    values.removeAt(indexOf)
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