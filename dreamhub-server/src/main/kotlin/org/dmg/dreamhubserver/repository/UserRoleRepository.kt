package org.dmg.dreamhubserver.repository

import org.dmg.dreamhubserver.model.UserRole
import org.springframework.data.repository.CrudRepository

interface UserRoleRepository: CrudRepository<UserRole, Long> {
  fun findAllByUserId(userId: Long): List<UserRole>

  fun findByUserIdAndSettingId(userId: Long, settingId: Long): UserRole?

  fun findAllBySettingId(settingId: Long): List<UserRole>

  fun countAllBySettingId(settingId: Long): Int
}