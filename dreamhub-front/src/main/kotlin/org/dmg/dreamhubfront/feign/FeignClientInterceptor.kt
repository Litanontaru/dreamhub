package org.dmg.dreamhubfront.feign

import feign.RequestInterceptor
import feign.RequestTemplate
import org.dmg.dreamhubfront.page.UserSession
import org.springframework.stereotype.Component

@Component
class FeignClientInterceptor(private val userSession: UserSession) : RequestInterceptor {
  private val USER_HEADER = "User"

  override fun apply(requestTemplate: RequestTemplate) {
    if (userSession.isLoggedIn) {
      requestTemplate.header(USER_HEADER, userSession.user.email)
    }
  }
}