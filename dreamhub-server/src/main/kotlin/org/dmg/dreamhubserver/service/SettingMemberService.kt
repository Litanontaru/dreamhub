package org.dmg.dreamhubserver.service

import org.dmg.dreamhubfront.SettingMember
import org.dmg.dreamhubserver.model.User
import org.dmg.dreamhubserver.model.UserRole
import org.dmg.dreamhubserver.repository.UserRepository
import org.dmg.dreamhubserver.repository.UserRoleRepository
import org.springframework.stereotype.Service

@Service
class SettingMemberService(
  val userRepository: UserRepository,
  val userRoleRepository: UserRoleRepository,
) {
  fun getMembers(settingId: Long): List<SettingMember> {
    val mapNotNull = userRoleRepository
      .findAllBySettingId(settingId)
      .mapNotNull { role ->
        userRepository.findById(role.userId)
          .map { SettingMember().apply { userEmail = it.email; userRole = role.role } }
          .orElse(null)
      }
    return mapNotNull
  }

  fun getMembersCount(settingId: Long) = userRoleRepository.countAllBySettingId(settingId)

  fun grantRoleToMember(settingId: Long, newMember: SettingMember) {
    val user = userRepository.findByEmail(newMember.userEmail) ?: User().apply { email = newMember.userEmail; userRepository.save(this) }
    val role = (userRoleRepository.findByUserIdAndSettingId(user.id, settingId) ?: UserRole().apply { userId = user.id; this.settingId = settingId; userRoleRepository.save(this) })

    role.role = newMember.userRole
  }

  fun revokeAccess(settingId: Long, email: String) {
    userRepository
      .findByEmail(email)
      ?.id
      ?.let { userRoleRepository.findByUserIdAndSettingId(it, settingId) }
      ?.run { userRoleRepository.delete(this) }
  }
}