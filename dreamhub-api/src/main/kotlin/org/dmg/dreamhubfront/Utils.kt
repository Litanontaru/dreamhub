package org.dmg.dreamhubfront

fun <T> MutableList<T>.swap(one: Int, another: Int) {
  val t = this[one]
  this[one] = this[another]
  this[another] = t
}