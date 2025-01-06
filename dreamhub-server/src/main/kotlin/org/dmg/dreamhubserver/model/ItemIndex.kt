package org.dmg.dreamhubserver.model

import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import java.sql.Types

@Entity
class ItemIndex: DBObject() {
  var ref: Long = -1

  var settingId: Long = -1

  @JdbcTypeCode(Types.LONGVARCHAR)
  var ids: String = ""

  fun ids() = ids.split(",").asSequence().filter { it.isNotBlank() }.map { it.toLong() }
}