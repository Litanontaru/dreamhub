package org.dmg.dreamhubserver.model

import org.hibernate.annotations.Type
import javax.persistence.Entity

@Entity
class Item: DBObject() {
  var origin: Long = 0

  var name: String = ""

  var path: String = ""

  var settingId: Long = 0

  @Type(type = "text")
  var extends: String = ""

  var isType: Boolean = false

  @Type(type = "text")
  var definition: String = ""

  fun extends() = extends.split(",").filter { it.isNotBlank() }.mapNotNull { it.toLongOrNull() }
}