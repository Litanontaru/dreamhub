package org.dmg.dreamhubserver.model

import javax.persistence.Entity

@Entity
class UserRole: DBObject() {
  val setting: Long = 0
  val userId: Long = 0
}