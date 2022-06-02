package org.dmg.dreamhubfront

import com.github.benmanes.caffeine.cache.Caffeine
import org.dmg.dreamhubfront.CacheNames.SETTING_TYPES
import org.springframework.cache.Cache
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig {
  @Bean
  fun settingTypesCache(): Cache {
    return CaffeineCache(
      SETTING_TYPES,
      Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(10L))
        .build()
    )
  }
}

object CacheNames {
  const val SETTING_TYPES: String = "SETTING_TYPES"
}
