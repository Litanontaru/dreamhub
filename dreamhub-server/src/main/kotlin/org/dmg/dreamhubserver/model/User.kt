package org.dmg.dreamhubserver.model

import org.hibernate.annotations.Type
import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import java.sql.Types

@Entity
class User: DBObject() {
  var email: String = ""

  var displayName: String = ""

  @JdbcTypeCode(Types.LONGVARCHAR)
  var info: String = ""
}