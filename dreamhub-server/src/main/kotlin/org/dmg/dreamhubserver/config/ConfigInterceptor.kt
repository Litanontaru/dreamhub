package org.dmg.dreamhubserver.config

import org.dmg.dreamhubserver.controller.HeaderInterceptor
import org.springframework.stereotype.Component
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Component
class ConfigInterceptor(val headerInterceptor: HeaderInterceptor): WebMvcConfigurer {
  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(headerInterceptor);
  }
}