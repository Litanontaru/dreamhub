package org.dmg.dreamhubfront.config

import feign.okhttp.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FeignConfig {
  @Bean
  fun client(): OkHttpClient? {
    return OkHttpClient()
  }
}