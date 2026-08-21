package io.github.metdaisy.amaazon.catalog.infra.repository;

import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.repository.CategoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryJpaRepository extends JpaRepository<Category, UUID>, CategoryRepository {

  @Override
  Optional<Category> findById(UUID id);

  @Override
  Category getReferenceById(UUID id);

  @Override
  @EntityGraph(attributePaths = {"parent", "children"})
  @Query("""
      select category
      from Category category
      left join category.parent parent
      order by category.depth asc, parent.id asc, category.name asc, category.id asc
      """)
  List<Category> findAll();

}
