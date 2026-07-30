package io.github.metdaisy.amaazon.global.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.context.annotation.Bean;
import java.time.Clock;

@EnableJpaAuditing
@EnableAsync
@EnableCaching
@EnableWebSecurity
@Configuration
public class GlobalConfig {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

}
