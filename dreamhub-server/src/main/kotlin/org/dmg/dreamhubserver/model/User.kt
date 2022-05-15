package org.dmg.dreamhubserver.model

import org.hibernate.annotations.Type
import javax.persistence.Entity

@Entity
class User: DBObject() {
  var email: String = ""

  var displayName: String = ""

  @Type(type = "text")
  var info: String = ""
}