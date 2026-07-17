package io.github.metdaisy.amaazon.user.domain.repository;

import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import io.github.metdaisy.amaazon.user.domain.entity.User;

public interface UserRepository extends DomainRepository<User> {

  boolean existsByName(String name);

  boolean existsByPhoneNumber(String phoneNumber);
}
