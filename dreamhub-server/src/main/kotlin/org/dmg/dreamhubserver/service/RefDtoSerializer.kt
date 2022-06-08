package org.dmg.dreamhubserver.service

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.dmg.dreamhubfront.RefDto

class RefDtoSerializer(private val defaultSerializer: JsonSerializer<Any?>): JsonSerializer<RefDto>() {
  override fun serialize(value: RefDto, jgen: JsonGenerator, provider: SerializerProvider) {
    val old = value.item
    value.item = null
    defaultSerializer.serialize(value, jgen, provider)
    value.item = old
  }
}