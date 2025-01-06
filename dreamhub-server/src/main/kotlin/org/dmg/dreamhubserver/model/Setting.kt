package org.dmg.dreamhubserver.model

import org.hibernate.annotations.Type
import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import java.sql.Types

@Entity
class Setting: DBObject() {
  var origin: Long = 0

  var version: Long = 0

  var isSnapshot: Boolean = true

  var name: String = ""

  @JdbcTypeCode(Types.LONGVARCHAR)
  var description: String = ""

  @JdbcTypeCode(Types.LONGVARCHAR)
  var dependencies: String = ""

  @JdbcTypeCode(Types.LONGVARCHAR)
  var allTypeItems: String  = ""
}