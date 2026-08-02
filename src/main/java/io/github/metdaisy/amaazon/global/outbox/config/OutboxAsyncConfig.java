package io.github.metdaisy.amaazon.global.outbox.config;

import java.util.concurrent.Executor;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.github.metdaisy.amaazon.global.config.MdcTaskDecorator;

@Configuration
public class OutboxAsyncConfig {

  @Bean("outboxWorker")
  public Executor outboxWorker(ThreadPoolTaskExecutorBuilder builder) {
    return builder.corePoolSize(10)
        .maxPoolSize(20)
        .queueCapacity(50)
        .threadNamePrefix("outbox-worker-")
        .taskDecorator(new MdcTaskDecorator())
        .build();
  }
}
