package io.github.metdaisy.amaazon.support;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class QueryInspectorConfig {

  @Bean
  QueryInspector queryInspector() {
    return new QueryInspector();
  }

  @Bean
  HibernatePropertiesCustomizer queryInspectorCustomizer(QueryInspector queryInspector) {
    return properties -> properties.put(AvailableSettings.STATEMENT_INSPECTOR, queryInspector);
  }
}
