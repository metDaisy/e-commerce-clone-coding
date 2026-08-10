package io.github.metdaisy.amaazon.catalog.infra.repository;

import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.repository.CategoryRepository;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

public interface CategoryJpaRepository extends JpaRepository<Category, UUID>, CategoryRepository {

  @Override
  @EntityGraph(attributePaths = {"parent", "childrenCategory"})
  @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
  List<Category> findAll();

}
