package org.dmg.dreamhubfront

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*

@FeignClient(name = "api", url = "\${backend-url}")
interface SettingController {
  @GetMapping("/settings")
  fun getAllSettings() : List<SettingListDto>

  @GetMapping("/settings/{settingId}")
  fun getSettingById(@PathVariable settingId: Long): SettingDto

  @PostMapping("/settings")
  fun addSetting(@RequestBody setting: SettingDto): SettingDto

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
}