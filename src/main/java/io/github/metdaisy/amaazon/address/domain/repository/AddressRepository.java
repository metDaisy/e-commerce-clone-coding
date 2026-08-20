package io.github.metdaisy.amaazon.address.domain.repository;

import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import io.github.metdaisy.amaazon.common.dto.PageQuery;
import io.github.metdaisy.amaazon.common.dto.PageResult;
import io.github.metdaisy.amaazon.address.domain.entity.Address;
import java.util.UUID;

public interface AddressRepository extends DomainRepository<Address> {

  PageResult<Address> findPageByUserId(UUID userId, PageQuery pageQuery);

  void deleteAndUpdatePrimary(UUID userId, UUID addressId);

  boolean existsByUserIdAndPostalCodeAndAddressLine(UUID userId, String postalCode,
      String addressLine);

  boolean existsByUserIdAndPostalCodeAndAddressLineAndIdNot(UUID userId, String postalCode,
      String addressLine, UUID id);

  boolean existsByIdAndUserId(UUID id, UUID userId);

  int clearPrimaryByUserId(UUID userId);

  Address makePrimary(UUID userId, UUID addressId);
}
