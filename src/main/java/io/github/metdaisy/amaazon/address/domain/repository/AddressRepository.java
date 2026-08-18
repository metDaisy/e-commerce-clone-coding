package io.github.metdaisy.amaazon.address.domain.repository;

import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import io.github.metdaisy.amaazon.address.domain.entity.Address;
import java.util.List;
import java.util.UUID;

public interface AddressRepository extends DomainRepository<Address> {

  List<Address> findByUserId(UUID userId);

  long countByUserId(UUID userId);

  int clearPrimaryByUserId(UUID userId);
}
