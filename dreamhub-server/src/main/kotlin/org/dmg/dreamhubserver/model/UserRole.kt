package org.dmg.dreamhubserver.model

import javax.persistence.Entity

@Entity
class UserRole: DBObject() {
  var settingId: Long = 0
  var userId: Long = 0
  var role: UserRoleType = UserRoleType.GUEST
}