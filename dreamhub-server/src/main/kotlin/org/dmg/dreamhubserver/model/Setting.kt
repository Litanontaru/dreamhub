package org.dmg.dreamhubserver.model

import org.hibernate.annotations.Type
import javax.persistence.Entity

@Entity
class Setting: DBObject() {
  var origin: Long = 0

  var version: Long = 0

  var isSnapshot: Boolean = true

  var name: String = ""

  @Type(type = "text")
  var description: String = ""

  @Type(type = "text")
  var dependencies: String = ""

  @Type(type = "text")
  var allTypeItems: String  = ""
}