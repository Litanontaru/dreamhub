package org.dmg.dreamhubserver.controller

import org.dmg.dreamhubfront.*
import org.dmg.dreamhubserver.service.SettingMemberService
import org.dmg.dreamhubserver.service.SettingService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController

@RestController
@Transactional
class SettingControllerImpl(
  val settingService: SettingService,
  val settingMemberService: SettingMemberService
): SettingController {
  override fun getAllSettings(): List<ItemName> = settingService.getAllSettings(UserSession.userEmail)

  override fun getSettingById(settingId: Long): SettingDto?  = settingService.getSettingById(UserSession.userEmail, settingId)

  override fun addSetting(setting: SettingDto) {
    settingService.addSetting(UserSession.userEmail, setting)
  }

  override fun removeSetting(settingId: Long) {
    settingService.removeSetting(UserSession.userEmail, settingId)
  }

  override fun setName(settingId: Long, newName: String) {
    settingService.setName(settingId, newName)
  }

  override fun setDescription(settingId: Long, newDescription: String) {
    settingService.setDescription(settingId, newDescription)
  }

  override fun addDependency(settingId: Long, newDependencyId: Long) {
    settingService.addDependency(settingId, newDependencyId)
  }

  override fun removeDependency(settingId: Long, oldDependencyId: Long) {
    settingService.removeDependency(settingId, oldDependencyId)
  }

  override fun getMembers(settingId: Long): List<SettingMember> {
    return settingMemberService.getMembers(settingId)
  }

  override fun grantRoleToMember(settingId: Long, newMember: SettingMember) {
    settingMemberService.grantRoleToMember(settingId, newMember)
  }

  override fun revokeAccess(settingId: Long, email: String) {
    settingMemberService.revokeAccess(settingId, email)
  }
}