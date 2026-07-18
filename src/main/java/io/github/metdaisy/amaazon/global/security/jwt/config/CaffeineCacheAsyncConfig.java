package io.github.metdaisy.amaazon.global.security.jwt.config;

import java.util.concurrent.Executor;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@Configuration
public class CaffeineCacheAsyncConfig {

  @Bean("caffeineWorker")
  public Executor caffeineWorker(ThreadPoolTaskExecutorBuilder builder) {
    return builder.corePoolSize(10)
            .maxPoolSize(20)
            .queueCapacity(50)
            .threadNamePrefix("caffeine-worker-")
            .build();
  }
}
