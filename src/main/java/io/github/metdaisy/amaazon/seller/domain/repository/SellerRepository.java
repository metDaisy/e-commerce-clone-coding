package io.github.metdaisy.amaazon.seller.domain.repository;

import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import io.github.metdaisy.amaazon.seller.domain.entity.Seller;
import io.github.metdaisy.amaazon.seller.domain.entity.constant.SellerStatus;
import java.util.UUID;

public interface SellerRepository extends DomainRepository<Seller> {

  boolean existsByUserIdAndStatus(UUID userId, SellerStatus status);
}
