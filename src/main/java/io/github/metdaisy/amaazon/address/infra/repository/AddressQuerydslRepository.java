package io.github.metdaisy.amaazon.address.infra.repository;

import io.github.metdaisy.amaazon.address.domain.entity.Address;
import java.util.UUID;

public interface AddressQuerydslRepository {

  void deleteAndUpdatePrimary(UUID userId, UUID addressId);

  Address makePrimary(UUID userId, UUID addressId);
}
