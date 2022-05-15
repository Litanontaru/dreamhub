package org.dmg.dreamhubserver.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.dmg.dreamhubfront.TypeDto
import org.dmg.dreamhubserver.repository.SettingRepository
import org.springframework.stereotype.Service

@Service
class SettingService(
  val repository: SettingRepository
) {
  fun getAllDependencyTypes(settingId: Long): List<TypeDto> =
    (getDependencies(settingId) + settingId)
      .let { repository.findAllById(it) }
      .flatMap { it.allTypeItems.toListDto() }

  fun getDependencies(settingId: Long): List<Long> =
    repository
      .findById(settingId)
      .get()
      .dependencies
      .split(",")
      .mapNotNull { it.toLongOrNull() }
}

fun String.toListDto(): List<TypeDto> = ObjectMapper().readValue(this, object : TypeReference<List<TypeDto>>() {})