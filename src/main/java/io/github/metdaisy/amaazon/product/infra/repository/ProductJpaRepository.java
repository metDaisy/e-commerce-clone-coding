package io.github.metdaisy.amaazon.product.infra.repository;

import io.github.metdaisy.amaazon.product.domain.entity.Product;
import io.github.metdaisy.amaazon.product.domain.repository.ProductRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<Product, UUID>, ProductRepository {

}
