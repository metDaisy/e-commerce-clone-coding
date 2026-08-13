package io.github.metdaisy.amaazon.support;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import io.github.metdaisy.amaazon.TestcontainersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestcontainersConfiguration.class)
public abstract class BaseIntegrationTest {

  @Autowired
  protected EntityManager em;

  protected void flushAndClear() {
    em.flush();
    em.clear();
  }

  protected void clear() {
    em.clear();
  }

  protected <T> T persistAndFlush(T entity) {
    em.persist(entity);
    em.flush();
    return entity;
  }

  protected boolean compareInstant(Instant actual, Instant expected) {
    if (actual == null && expected == null) {
      return true;
    }
    if (actual == null || expected == null) {
      return false;
    }
    return truncateToMillis(actual).equals(truncateToMillis(expected));
  }

  private Instant truncateToMillis(Instant instant) {
    return instant.truncatedTo(ChronoUnit.MILLIS);
  }
}
