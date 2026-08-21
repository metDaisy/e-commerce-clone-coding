package io.github.metdaisy.amaazon.user.domain.repository;

import java.util.Optional;
import java.util.UUID;
import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import io.github.metdaisy.amaazon.user.domain.entity.User;

public interface UserRepository extends DomainRepository<User> {

  Optional<User> findWithRolesById(UUID userId);

  boolean existsByNameAndIsEnabledTrue(String name);

  boolean existsByPhoneNumberAndIsEnabledTrue(String phoneNumber);

  boolean existsByNameAndIsEnabledTrueAndIdNot(String name, UUID userId);

  boolean existsByPhoneNumberAndIsEnabledTrueAndIdNot(String phoneNumber, UUID userId);

  boolean existsByIdAndIsEnabledTrue(UUID userId);
}
