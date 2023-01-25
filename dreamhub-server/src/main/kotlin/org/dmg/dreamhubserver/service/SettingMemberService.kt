package org.dmg.dreamhubserver.service

import org.dmg.dreamhubfront.SettingMember
import org.dmg.dreamhubserver.model.UserRole
import org.dmg.dreamhubserver.repository.UserRepository
import org.dmg.dreamhubserver.repository.UserRoleRepository
import org.springframework.stereotype.Service

@Service
class SettingMemberService(
  val userRepository: UserRepository,
  val userRoleRepository: UserRoleRepository,
) {
  fun getMembers(settingId: Long): List<SettingMember> =
    userRoleRepository
      .findAllBySettingId(settingId)
      .mapNotNull { role ->
        userRepository.findById(role.userId)
          .map { SettingMember().apply { userEmail = it.email; userRole = role.role } }
          .orElse(null)
      }

  fun grantRoleToMember(settingId: Long, newMember: SettingMember) {
    userRepository
      .findByEmail(newMember.userEmail)
      ?.id
      ?.let { userId ->
        val role = userRoleRepository
          .findByUserIdAndSettingId(userId, settingId)
          ?: UserRole().also { it.userId = userId; it.settingId = settingId }

        role.role = newMember.userRole
      }
  }

  fun revokeAccess(settingId: Long, email: String) {
    userRepository
      .findByEmail(email)
      ?.id
      ?.let { userRoleRepository.findByUserIdAndSettingId(it, settingId) }
      ?.run { userRoleRepository.delete(this) }
  }
}