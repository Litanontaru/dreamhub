package org.dmg.dreamhubfront

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*

@FeignClient(name = "api.setting", url = "\${backend-url}")
interface SettingController {
  @GetMapping("/settings")
  fun getAllSettings() : List<ItemName>

  @GetMapping("/settings/{settingId}")
  fun getSettingById(@PathVariable settingId: Long): SettingDto?

  @PostMapping("/settings")
  fun addSetting(@RequestBody setting: SettingDto)

  @DeleteMapping("/settings/{settingId}")
  fun removeSetting(@PathVariable settingId: Long)

  @PutMapping("/settings/{settingId}/name")
  fun setName(@PathVariable settingId: Long, @RequestBody newName: String)

  @PutMapping("/settings/{settingId}/description")
  fun setDescription(@PathVariable settingId: Long, @RequestBody newDescription: String)

  @PostMapping("/settings/{settingId}/dependencies/{newDependencyId}")
  fun addDependency(@PathVariable settingId: Long, @PathVariable newDependencyId: Long)

  @DeleteMapping("/settings/{settingId}/dependencies/{oldDependencyId}")
  fun removeDependency(@PathVariable settingId: Long, @PathVariable oldDependencyId: Long)

  @GetMapping("/settings/{settingId}/members")
  fun getMembers(@PathVariable settingId: Long): List<SettingMember>

  @PutMapping("/settings/{settingId}/members")
  fun grantRoleToMember(@PathVariable settingId: Long, @RequestBody newMember: SettingMember)

  @DeleteMapping("/settings/{settingId}/members")
  fun revokeAccess(@PathVariable settingId: Long, @RequestBody email: String)
}