package io.github.metdaisy.amaazon.support;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import io.github.metdaisy.amaazon.TestcontainersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({TestcontainersConfiguration.class, RepositoryTestConfig.class})
public abstract class BaseRepositoryTest {

  @Autowired
  protected EntityManager em;

  @Autowired
  protected QueryInspector queryInspector;

  protected void flushAndClear() {
    em.flush();
    em.clear();
    queryInspector.clear();
  }

  protected void clear() {
    em.clear();
    queryInspector.clear();
  }

  protected void ensureQueryCount(int count) {
    assertThat(queryInspector.getCount()).isEqualTo(count);
  }

  protected <T> T persistAndFlush(T entity) {
    em.persist(entity);
    em.flush();
    return entity;
  }

  protected boolean compareInstant(Instant a, Instant b) {
    if (a == null && b == null) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }
    return toTruncated(a).equals(toTruncated(b));
  }

  private Instant toTruncated(Instant time) {
    return time.truncatedTo(ChronoUnit.MILLIS);
  }
}
