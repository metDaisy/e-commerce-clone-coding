package io.github.metdaisy.amaazon.catalog.domain.repository;

import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import io.github.metdaisy.amaazon.catalog.domain.entity.Category;
import java.util.List;

public interface CategoryRepository extends DomainRepository<Category> {

  List<Category> findAll();
}
