package org.dmg.dreamhubfront

import com.vaadin.flow.spring.security.VaadinWebSecurityConfigurerAdapter
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity


@EnableWebSecurity
@Configuration
class SecurityConfiguration : VaadinWebSecurityConfigurerAdapter() {
  @Throws(Exception::class)
  override fun configure(http: HttpSecurity) {
    super.configure(http)
    http.oauth2Login().loginPage(LOGIN_URL).permitAll()
  }

  companion object {
    private const val LOGIN_URL = "/login"
  }
}