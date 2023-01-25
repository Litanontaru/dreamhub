package org.dmg.dreamhubfront

class SettingDto {
  var id: Long = 0
  var name: String = ""
  var description: String = ""
  var dependencies: MutableList<ItemName> = mutableListOf()
}

class SettingMember {
  var userEmail: String = ""
  var userRole: UserRoleType = UserRoleType.GUEST
}