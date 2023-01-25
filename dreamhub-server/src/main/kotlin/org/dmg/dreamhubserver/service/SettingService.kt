package org.dmg.dreamhubserver.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.dmg.dreamhubfront.SettingDto
import org.dmg.dreamhubfront.SettingListDto
import org.dmg.dreamhubfront.TypeDto
import org.dmg.dreamhubserver.model.Setting
import org.dmg.dreamhubserver.model.User
import org.dmg.dreamhubserver.model.UserRole
import org.dmg.dreamhubfront.UserRoleType
import org.dmg.dreamhubserver.repository.SettingList
import org.dmg.dreamhubserver.repository.SettingRepository
import org.dmg.dreamhubserver.repository.UserRepository
import org.dmg.dreamhubserver.repository.UserRoleRepository
import org.springframework.stereotype.Service

@Service
class SettingService(
  val settingRepository: SettingRepository,
  val userRepository: UserRepository,
  val userRoleRepository: UserRoleRepository
) {
  private fun getRoles(userEmail: String): Map<Long, UserRoleType> =
    userRepository
      .findByEmail(userEmail)
      ?.id
      ?.let { userRoleRepository.findAllByUserId(it) }
      ?.associate { it.settingId to it.role }
      ?: mapOf()

  private fun getRole(userEmail: String, settingId: Long): UserRoleType? =
    userRepository
      .findByEmail(userEmail)
      ?.id
      ?.let { userRoleRepository.findByUserIdAndSettingId(it, settingId) }
      ?.role

  fun getAllSettings(userEmail: String): List<SettingListDto> =
    getRoles(userEmail)
      .let { settingRepository.getAll(it.keys.toList()) }
      .map { it.toDto() }

  fun getSettingById(userEmail: String, settingId: Long): SettingDto? =
    getRole(userEmail, settingId)
      ?.let { settingRepository.findById(settingId) }
      ?.orElse(null)
      ?.let {
        SettingDto().apply {
          id = it.id
          name = it.name
          description = it.description
          dependencies = it.dependencies
            .split(",")
            .mapNotNull { it.toLongOrNull() }
            .let { settingRepository.getAll(it) }
            .map { it.toDto() }.toMutableList()
        }
      }

  fun addSetting(userEmail: String, setting: SettingDto) {
    val newSettingId = Setting().let {
      it.name = setting.name
      it.description = setting.description
      it.dependencies = setting.dependencies.map { it.id }.joinToString()
      settingRepository.save(it)
      it.id
    }
    (userRepository.findByEmail(userEmail) ?: User().apply {
      email = userEmail
      userRepository.save(this)
    }).let {
      UserRole().apply {
        userId = it.id
        settingId = newSettingId
        role = UserRoleType.OWNER
        userRoleRepository.save(this)
      }
    }
  }

  fun removeSetting(userEmail: String, settingId: Long) {
    getRole(userEmail, settingId)
      ?.takeIf { it == UserRoleType.OWNER }
      ?.let { settingRepository.deleteById(settingId) }
  }

  //--------------------------------------------------------------------------------------------------------------------

  fun getDependencies(settingId: Long): List<Long> =
    listOf(settingId) + settingRepository
      .findById(settingId)
      .get()
      .dependencies
      .split(",")
      .mapNotNull { it.toLongOrNull() }
      .flatMap { getDependencies(it) }
}

fun String.toListDto(): List<TypeDto> = when {
  isBlank() -> listOf()
  else -> ObjectMapper().readValue(this, object : TypeReference<List<TypeDto>>() {})
}

fun SettingList.toDto(): SettingListDto {
  return SettingListDto().also {
    it.id = this.getId()
    it.name = this.getName()
  }
}