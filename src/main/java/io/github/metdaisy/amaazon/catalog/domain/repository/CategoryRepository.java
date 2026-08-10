package io.github.metdaisy.amaazon.catalog.domain.repository;

import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends DomainRepository<Category> {

  Optional<Category> findByName(String name);

  List<Category> findAll();
}
