package org.dmg.dreamhubserver.controller

import org.dmg.dreamhubfront.SettingController
import org.dmg.dreamhubfront.SettingDto
import org.dmg.dreamhubfront.SettingListDto
import org.dmg.dreamhubserver.service.SettingService
import org.springframework.web.bind.annotation.RestController

@RestController
class SettingControllerImpl(
  val settingService: SettingService
): SettingController {
  override fun getAllSettings(): List<SettingListDto> = settingService.getAllSettings(UserSession.userEmail)

  override fun getSettingById(settingId: Long): SettingDto?  = settingService.getSettingById(UserSession.userEmail, settingId)

  override fun addSetting(setting: SettingDto) {
    settingService.addSetting(UserSession.userEmail, setting)
  }

  override fun removeSetting(settingId: Long) {
    settingService.removeSetting(UserSession.userEmail, settingId)
  }

  override fun setName(settingId: Long, newName: String) {
    TODO("Not yet implemented")
  }

  override fun setDescription(settingId: Long, newDescription: String) {
    TODO("Not yet implemented")
  }

  override fun addDependency(settingId: Long, newDependencyId: Long) {
    TODO("Not yet implemented")
  }

  override fun removeDependency(settingId: Long, oldDependencyId: Long) {
    TODO("Not yet implemented")
  }

}