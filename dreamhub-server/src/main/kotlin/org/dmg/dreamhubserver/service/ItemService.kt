package org.dmg.dreamhubserver.service

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import org.dmg.dreamhubfront.*
import org.dmg.dreamhubfront.StandardTypes.TYPE
import org.dmg.dreamhubserver.model.Item
import org.dmg.dreamhubserver.repository.ItemIndexRepository
import org.dmg.dreamhubserver.repository.ItemList
import org.dmg.dreamhubserver.repository.ItemListWithExtends
import org.dmg.dreamhubserver.repository.ItemRepository
import org.springframework.stereotype.Service

@Service
class ItemService(
  private val itemRepository: ItemRepository,
  private val itemIndexRepository: ItemIndexRepository,
  private val settingService: SettingService,
  private val itemIndexService: ItemIndexService
) {

  fun getAll(settingId: Long): List<ItemListDto> = itemRepository.getAll(settingId).map { it.toDto() }

  fun getAll(settingId: Long, filter: String): List<ItemListDto> =
    itemRepository.getAll(settingId, "%$filter%").map { it.toDto() }

  fun getAll(settingId: Long, findUsages: Long): List<ItemListDto> = TODO("Not yet implemented")

  fun getAllRecursiveSetting(settingId: Long, superTypeIds: List<Long>): List<ItemListDto> =
    (settingService.getDependencies(settingId) + settingId)
      .let { itemIndexRepository.findAllByRefInAndSettingIdIn(superTypeIds, it) }
      .flatMap { it.ids() }
      .distinct()
      .let { itemRepository.getAll(it) }
      .let {
        when {
          superTypeIds.contains(TYPE.id) -> it + getAllTypeList(settingId)
          else -> it
        }
      }
      .distinctBy { it.getId() }
      .map { it.toDto() }
      .sortedBy { it.name }

  fun getAllTypes(settingId: Long): List<TypeDto> = getAllTypeList(settingId).map { it.toDto() }

  private fun getAllTypeList(settingId: Long) =
    (settingService.getDependencies(settingId) + settingId).let { itemRepository.getAllTypes(it) }

  fun get(id: Long): ItemDto = itemRepository.getDefinitionById(id).toDto().prepare()

  private fun ItemDto.prepare(): ItemDto {
    getRefItems()
    return this
  }

  private fun AbstractItemDto.getRefItems() {
    (extends.asSequence() +
        attributes
          .asSequence()
          .flatMap { it.values }
          .mapNotNull { it.terminal })
      .forEach { it.item = get(it.id) }

    attributes
      .asSequence()
      .flatMap { it.values }
      .mapNotNull { it.nested }
      .forEach { it.getRefItems() }
  }

  private fun AbstractItemDto.refreshExtendsOnly(): AbstractItemDto {
    extends
      .forEach() {
        it.item = itemRepository.getDefinitionById(it.id).toDto()
        it.item?.refreshExtendsOnly()
      }
    return this
  }

  fun add(newItem: ItemDto): ItemDto {
    val item = Item().apply {
      name = newItem.name
      path = newItem.path
      settingId = newItem.settingId
      extends = newItem.extends.map { it.id }.joinToString()
      isType = newItem.isType
      definition = newItem.toJson()
    }
    itemRepository.save(item)
    item.modify { it.id = item.id }

    itemIndexService.reindexRecursive(item)

    return get(item.id)
  }

  fun remove(id: Long) {
    itemRepository.deleteById(id)
  }

  private fun Item.modify(action: (ItemDto) -> Unit) {
    definition = definition.toDto().also { action(it) }.toJson()
  }

  private fun Item.modify(nestedId: Long, prerequisite: (ItemDto) -> Unit = {}, action: (AbstractItemDto) -> Unit): ItemDto {
    val root = definition.toDto()
    prerequisite(root)
    if (nestedId == -1L) {
      action(root)
    } else {
      root
        .attributes
        .asSequence()
        .flatMap { it.values }
        .mapNotNull { it.nested }
        .find { it.modify(nestedId, action) }
    }
    definition = root.toJson()
    return root
  }

  private fun AbstractItemDto.modify(nestedId: Long, action: (AbstractItemDto) -> Unit): Boolean =
    if (this.nestedId == nestedId) {
      action(this)
      true
    } else {
      attributes
        .asSequence()
        .flatMap { it.values }
        .mapNotNull { it.nested }
        .find { it.modify(nestedId, action) } != null
    }

  fun setName(id: Long, nestedId: Long, newName: String) {
    val item = itemRepository.findById(id).get()
    item.modify(nestedId) { it.name = newName }
    if (nestedId == -1L) {
      item.name = newName
    }
  }

  fun setPath(id: Long, newPath: String) {
    val item = itemRepository.findById(id).get()
    item.modify { it.path = newPath }
    item.path = newPath
  }

  fun setSetting(id: Long, newSetting: Long) {
    val item = itemRepository.findById(id).get()
    item.modify { it.settingId = newSetting }
    item.settingId = newSetting
  }

  fun setFormula(id: Long, newFormula: String) {
    itemRepository.findById(id).get().modify { it.formula = newFormula }
  }

  fun setIsType(id: Long, newIsType: Boolean) {
    val item = itemRepository.findById(id).get()
    item.modify { it.isType = newIsType }
    item.isType = newIsType
  }

  fun setIsFinal(id: Long, newIsFinal: Boolean) {
    itemRepository.findById(id).get().modify { it.isFinal = newIsFinal }
  }

  fun addExtends(id: Long, nestedId: Long, newExtendsId: Long): ItemDto =
    itemRepository
      .findById(id)
      .get()
      .let { item ->
        item
          .modify(nestedId) { it.extends.add(RefDto().also { it.id = newExtendsId }) }
          .prepare()
          .also {
            if (nestedId == -1L) {
              item.extends = (item.extends() + newExtendsId).joinToString()
            }
            itemIndexService.reindexRecursive(item)
          }
      }

  fun removeExtends(id: Long, nestedId: Long, oldExtendsId: Long): ItemDto =
    itemRepository
      .findById(id)
      .get()
      .let { item ->
        item
          .modify(nestedId) { it.extends.removeIf { it.id == oldExtendsId } }
          .prepare()
          .also {
            if (nestedId == -1L) {
              item.extends = item.extends().filter { it != oldExtendsId }.joinToString()
            }
            itemIndexService.reindexRecursive(item)
          }
      }

  fun addAllowedExtensions(id: Long, newAllowedExtensionId: Long) {
    itemRepository
      .findById(id)
      .get()
      .modify {
        it.allowedExtensions.add(ItemName().also {
          it.id = newAllowedExtensionId
          it.name = itemRepository.getNameById(newAllowedExtensionId)
        })
      }
  }

  fun removeAllowedExtensions(id: Long, oldAllowedExtensionId: Long) {
    itemRepository
      .findById(id)
      .get()
      .modify { it.allowedExtensions.removeIf { it.id == oldAllowedExtensionId } }
  }

  fun addMetadata(id: Long, newMetadata: MetadataDto) {
    itemRepository
      .findById(id)
      .get()
      .modify { it.metadata.add(newMetadata) }
  }

  fun removeMetadata(id: Long, attributeName: String) {
    itemRepository
      .findById(id)
      .get()
      .modify { it.metadata.removeIf { it.attributeName == attributeName } }
  }

  fun modifyMetadata(id: Long, newMetadata: MetadataDto) {
    itemRepository
      .findById(id)
      .get()
      .modify {
        it.metadata.filter { it.attributeName == newMetadata.attributeName }.forEach {
          it.typeId = newMetadata.typeId
          it.isSingle = newMetadata.isSingle
          it.allowCreate = newMetadata.allowCreate
          it.allowReference = newMetadata.allowReference
          it.isRequired = newMetadata.isRequired
        }
      }
  }

  fun addAttributeValue(id: Long, nestedId: Long, attributeName: String, newValue: ValueDto, action: (ItemDto) -> Unit = {}) {
    val item = itemRepository.findById(id).get()
    item.modify(nestedId, action) { dto ->
      val attribute = dto
        .attributes
        .firstOrNull { it.name == attributeName }                            //Данные уже есть
        ?: dto
          .refreshExtendsOnly()
          .superMetadata(attributeName)
          ?.let { metatdata ->        //Данных нет, но метаданные есть
            AttributeDto().also {
              it.name = attributeName
              dto.attributes.add(it)
            }
          }
        ?: throw IllegalStateException("Cannot find attribute with name $attributeName")

      attribute
        .values
        .add(newValue)
    }
  }

  fun addAttributePrimitiveValue(id: Long, nestedId: Long, attributeName: String, newValue: String) = ValueDto().also {
      it.primitive = newValue
      addAttributeValue(id, nestedId, attributeName, it)
    }

  fun addAttributeTerminalValue(id: Long, nestedId: Long, attributeName: String, newValue: Long) = ValueDto().also {
    it.terminal = RefDto().apply {
      this.id = newValue
      item = get(newValue)
    }
    addAttributeValue(id, nestedId, attributeName, it)
  }

  fun AbstractItemDto.maxNestedId(): Long =
    attributes
      .asSequence()
      .flatMap { it.values }
      .mapNotNull { it.nested }
      .flatMap { sequenceOf(it.nestedId, it.maxNestedId()) }
      .maxOfOrNull { it }
      ?: 0

  fun addAttributeNestedValue(id: Long, nestedId: Long, attributeName: String, newValue: Long) = ValueDto().also {
    val item = get(newValue)
    addAttributeValue(id, nestedId, attributeName, it) { root ->
      it.nested = AbstractItemDto().apply {
        this.id = id
        this.name = item.name
        this.nestedId = root.maxNestedId() + 1

        this.extends.add(RefDto().apply {
          this.id = newValue
          this.item = item
        })
      }
    }
  }

  fun removeAttributeValue(id: Long, nestedId: Long, attributeName: String, valueIndex: Int) {
    itemRepository
      .findById(id)
      .get()
      .modify(nestedId) {
        it
          .attributes
          .find { it.name == attributeName }
          ?.values?.removeAt(valueIndex)
      }
  }

  fun modifyAttributeValue(
    id: Long,
    nestedId: Long,
    attributeName: String,
    valueIndex: Int,
    newValue: ValueDto
  ) {
    itemRepository
      .findById(id)
      .get()
      .modify(nestedId) {
        it
          .attributes
          .find { it.name == attributeName }
          ?.values
          ?.set(valueIndex, newValue)
      }
  }

  fun modifyAttributePrimitiveValue(id: Long, nestedId: Long, attributeName: String, valueIndex: Int, newValue: String) = ValueDto().also {
    it.primitive = newValue
    modifyAttributeValue(id, nestedId, attributeName, valueIndex, it)
  }
}

private fun String.toDto() = ObjectMapper()
  .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  .readValue(this, ItemDto::class.java)

private fun ItemDto.toJson() = mapper.writeValueAsString(this)

fun ItemList.toDto(): ItemListDto = ItemListDto().also {
  it.id = getId()
  it.name = getName()
  it.path = getPath()
  it.settingId = getSettingId()
}

fun ItemListWithExtends.toDto() = TypeDto().apply {
  id = getId()
  name = getName()
  path = getPath()
  settingId = getSettingId()
  superTypeIds = getExtends().split(",").mapNotNull { it.toLongOrNull() }
}

val mapper = ObjectMapper().apply {
  setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  registerModule(object : SimpleModule() {
    override fun setupModule(context: SetupContext) {
      super.setupModule(context)

      context.addBeanSerializerModifier(object : BeanSerializerModifier() {
        override fun modifySerializer(config: SerializationConfig, desc: BeanDescription, serializer: JsonSerializer<*>): JsonSerializer<*> {
          return when {
            RefDto::class.java.isAssignableFrom(desc.getBeanClass()) -> RefDtoSerializer(serializer as JsonSerializer<Any?>)
            else -> serializer
          }
        }
      });
    }
  })
}