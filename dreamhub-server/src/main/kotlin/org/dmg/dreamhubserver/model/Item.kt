package org.dmg.dreamhubserver.model

import org.hibernate.annotations.Type
import jakarta.persistence.Column
import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import java.sql.Types

@Entity
class Item: DBObject() {
  var origin: Long = 0

  var name: String = ""

  var path: String = ""

  var settingId: Long = 0

  @JdbcTypeCode(Types.LONGVARCHAR)
  var extends: String = ""

  var isType: Boolean = false

  @JdbcTypeCode(Types.LONGVARCHAR)
  var definition: String = ""

  @Column(name = "rnk")
  var rank: Int = 0

  fun extends() = extends.split(",").filter { it.isNotBlank() }.mapNotNull { it.trim().toLongOrNull() }
}