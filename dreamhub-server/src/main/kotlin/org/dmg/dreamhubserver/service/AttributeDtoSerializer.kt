package org.dmg.dreamhubserver.service

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.dmg.dreamhubfront.AttributeDto

class AttributeDtoSerializer(private val defaultSerializer: JsonSerializer<Any?>): JsonSerializer<AttributeDto>() {
  override fun serialize(value: AttributeDto, jgen: JsonGenerator, provider: SerializerProvider) {
    val old = value.inherited
    value.inherited = null
    defaultSerializer.serialize(value, jgen, provider)
    value.inherited = old
  }
}