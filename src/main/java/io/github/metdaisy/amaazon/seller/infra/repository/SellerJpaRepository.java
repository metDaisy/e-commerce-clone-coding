package io.github.metdaisy.amaazon.seller.infra.repository;

import io.github.metdaisy.amaazon.seller.domain.entity.Seller;
import io.github.metdaisy.amaazon.seller.domain.repository.SellerRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerJpaRepository extends JpaRepository<Seller, UUID>, SellerRepository {

}
