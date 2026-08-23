package io.github.metdaisy.amaazon.catalog.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.metdaisy.amaazon.catalog.domain.entity.Tag;
import io.github.metdaisy.amaazon.support.BaseRepositoryTest;
import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DisplayName("태그 JPA 저장소")
class TagJpaRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private TagJpaRepository repository;

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @Autowired
  private TransactionTemplate transactionTemplate;

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @DisplayName("태그 조회: 영속성 컨텍스트를 비워도 2차 캐시에서 조회되어 추가 쿼리가 발생하지 않는다")
  void findById_shouldUseSecondLevelCacheAfterFirstDatabaseQuery() {
    UUID tagId = transactionTemplate.execute(status -> {
      Tag tag = persistAndFlush(new Tag("office"));
      return tag.getId();
    });
    evictTagCache(tagId);
    clear();

    Tag first = transactionTemplate.execute(status -> repository.findById(tagId).orElseThrow());

    assertThat(first.getName()).isEqualTo("office");
    ensureQueryCount(1);

    clear();

    Tag second = transactionTemplate.execute(status -> repository.findById(tagId).orElseThrow());

    assertThat(second.getName()).isEqualTo("office");
    assertThat(second.getId()).isEqualTo(tagId);
    ensureQueryCount(0);
  }

  private void evictTagCache(UUID tagId) {
    SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    sessionFactory.getCache().evictEntityData(Tag.class, tagId);
  }
}
