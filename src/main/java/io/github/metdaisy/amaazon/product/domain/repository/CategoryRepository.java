package io.github.metdaisy.amaazon.product.domain.repository;

import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import io.github.metdaisy.amaazon.product.domain.entity.Category;
import java.util.Optional;

public interface CategoryRepository extends DomainRepository<Category> {

  Optional<Category> findByName(String name);
}
