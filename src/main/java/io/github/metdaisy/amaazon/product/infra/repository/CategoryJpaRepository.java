package io.github.metdaisy.amaazon.product.infra.repository;

import io.github.metdaisy.amaazon.product.domain.entity.Category;
import io.github.metdaisy.amaazon.product.domain.repository.CategoryRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryJpaRepository extends JpaRepository<Category, UUID>, CategoryRepository {

}
