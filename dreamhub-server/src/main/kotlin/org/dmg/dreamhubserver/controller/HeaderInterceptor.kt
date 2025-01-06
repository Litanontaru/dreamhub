package org.dmg.dreamhubserver.controller

import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlin.concurrent.getOrSet

@Component
class HeaderInterceptor : HandlerInterceptor {
  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    request
      .getHeader("User")
      ?.takeIf { it.isNotEmpty() }
      ?.let { UserSession.userEmail = it }
    return true
  }

  override fun postHandle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
    modelAndView: ModelAndView?
  ) {
    UserSession.clear()
  }
}

object UserSession {
  private val userEmails = ThreadLocal<String>()

  var userEmail: String
    get() = userEmails.getOrSet { "" }
    set(value) {
      userEmails.set(value)
    }

  fun clear() {
    userEmails.remove()
  }
}