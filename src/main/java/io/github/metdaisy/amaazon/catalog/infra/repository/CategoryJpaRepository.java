package io.github.metdaisy.amaazon.catalog.infra.repository;

import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.catalog.domain.repository.CategoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryJpaRepository extends JpaRepository<Category, UUID>, CategoryRepository {

  @Override
  Optional<Category> findById(UUID id);

  @Override
  Category getReferenceById(UUID id);

  @Override
  @EntityGraph(attributePaths = {"parent", "children"})
  List<Category> findAll();

}
