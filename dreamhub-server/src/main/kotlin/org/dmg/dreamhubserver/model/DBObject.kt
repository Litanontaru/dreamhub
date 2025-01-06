package org.dmg.dreamhubserver.model

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass

@MappedSuperclass
open class DBObject {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  open var id: Long = 0

  @Transient
  var deleteOnly: Boolean = false
}