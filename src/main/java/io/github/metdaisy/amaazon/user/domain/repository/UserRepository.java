package io.github.metdaisy.amaazon.user.domain.repository;

import java.util.UUID;
import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import io.github.metdaisy.amaazon.user.domain.entity.User;

public interface UserRepository extends DomainRepository<User> {

  boolean existsByPhoneNumber(String phoneNumber);

  boolean existsById(UUID userId);
}
