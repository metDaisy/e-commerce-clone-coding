package io.github.metdaisy.amaazon.catalog.infra.repository;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.repository.CatalogProductRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogProductJpaRepository extends JpaRepository<CatalogProduct, UUID>,
    CatalogProductRepository {

}
