package org.dmg.dreamhubfront

import com.vaadin.flow.spring.security.VaadinWebSecurity
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.util.matcher.AntPathRequestMatcher


@EnableWebSecurity
@Configuration
class SecurityConfiguration : VaadinWebSecurity() {
  @Throws(Exception::class)
  override fun configure(http: HttpSecurity) {
    http.authorizeHttpRequests {
      it
        .requestMatchers(AntPathRequestMatcher("/public/**"))
        .permitAll()
    }

    super.configure(http)

    http.oauth2Login().loginPage(LOGIN_URL).permitAll()
  }

  companion object {
    private const val LOGIN_URL = "/login"
  }
}