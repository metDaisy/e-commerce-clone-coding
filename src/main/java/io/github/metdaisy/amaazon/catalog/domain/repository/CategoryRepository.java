package io.github.metdaisy.amaazon.catalog.domain.repository;

import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends DomainRepository<Category> {

  List<Category> findAll();

  boolean existsByName(String name);

  boolean existsByNameAndIdNot(String name, UUID id);
}
