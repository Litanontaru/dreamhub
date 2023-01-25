package org.dmg.dreamhubfront

enum class UserRoleType {
  OWNER,
  MAINTAINER,
  MEMBER,
  GUEST;

  fun toItemListDto(): ItemListDto {
    return ItemListDto().also { it.id = ordinal.toLong(); it.name = name; it.rank = ordinal }
  }
}