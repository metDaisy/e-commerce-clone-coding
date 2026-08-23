package io.github.metdaisy.amaazon.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@TestConfiguration(proxyBeanMethods = false)
@EnableJpaAuditing
@Import(QueryInspectorConfig.class)
public class RepositoryTestConfig {
}
