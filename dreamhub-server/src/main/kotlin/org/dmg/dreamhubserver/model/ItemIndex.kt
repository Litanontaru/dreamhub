package org.dmg.dreamhubserver.model

import org.hibernate.annotations.Type
import javax.persistence.Entity

@Entity
class ItemIndex(
  val ref: Long,
  val settingId: Long
): DBObject() {
  @Type(type = "text")
  var ids: String = ""

  fun ids() = ids.split(",").asSequence().filter { it.isNotBlank() }.map { it.toLong() }
}