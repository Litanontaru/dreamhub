package org.dmg.dreamhubserver.repository

import org.dmg.dreamhubserver.model.User
import org.springframework.data.repository.CrudRepository

interface UserRepository: CrudRepository<User, Long> {
  fun findByEmail(email: String): User?
}