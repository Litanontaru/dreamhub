package org.dmg.dreamhubfront.feign

import org.dmg.dreamhubfront.ItemName

object SpecialTypes {
  val SETTING = ItemName().apply { id = -5001; name = "Сеттинг" }
  val ROLE = ItemName().apply { id = -5002; name = "Роль" }
}