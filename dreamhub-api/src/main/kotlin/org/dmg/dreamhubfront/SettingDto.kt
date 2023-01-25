package org.dmg.dreamhubfront

class SettingListDto{
  var id: Long = 0
  var name: String = ""
}

class SettingDto {
  var id: Long = 0
  var name: String = ""
  var description: String = ""
  var dependencies: MutableList<SettingListDto> = mutableListOf()
}

class SettingMember {
  var userEmail: String = ""
  var userRole: UserRoleType = UserRoleType.GUEST
}