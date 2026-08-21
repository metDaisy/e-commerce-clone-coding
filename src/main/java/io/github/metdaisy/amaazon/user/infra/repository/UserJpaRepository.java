package io.github.metdaisy.amaazon.user.infra.repository;

import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.user.domain.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<User, UUID>, UserRepository {

  @Override
  Optional<User> findById(UUID id);

  @Override
  @EntityGraph(attributePaths = "roles")
  @Query("select u from User u where u.id = :userId")
  Optional<User> findWithRolesById(@Param("userId") UUID userId);
}
