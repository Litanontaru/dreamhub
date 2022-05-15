package org.dmg.dreamhubfront.page

import org.springframework.security.core.context.SecurityContextHolder

import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.SessionScope

import java.io.Serializable

@Component
@SessionScope
class UserSession : Serializable {
  val user: User
    get() {
      val authentication = SecurityContextHolder.getContext().authentication
      val principal = authentication.getPrincipal() as OAuth2AuthenticatedPrincipal
      return User(
        principal.getAttribute<String>("given_name"),
        principal.getAttribute<String>("family_name"),
        principal.getAttribute<String>("email"),
        principal.getAttribute<String>("picture")
      )
    }

  val isLoggedIn: Boolean
    get() {
      return SecurityContextHolder.getContext().authentication != null
    }
}