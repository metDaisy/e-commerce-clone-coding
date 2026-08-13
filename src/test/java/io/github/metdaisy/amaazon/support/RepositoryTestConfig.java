package io.github.metdaisy.amaazon.support;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@TestConfiguration(proxyBeanMethods = false)
@EnableJpaAuditing
public class RepositoryTestConfig {

  @Bean
  QueryInspector queryInspector() {
    return new QueryInspector();
  }

  @Bean
  HibernatePropertiesCustomizer queryInspectorCustomizer(QueryInspector queryInspector) {
    return properties -> properties.put(AvailableSettings.STATEMENT_INSPECTOR, queryInspector);
  }
}
