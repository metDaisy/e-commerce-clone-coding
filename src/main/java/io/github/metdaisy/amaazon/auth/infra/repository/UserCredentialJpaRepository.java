package io.github.metdaisy.amaazon.auth.infra.repository;

import io.github.metdaisy.amaazon.auth.domain.entity.UserCredential;
import io.github.metdaisy.amaazon.auth.domain.repository.UserCredentialRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface UserCredentialJpaRepository extends JpaRepository<UserCredential, UUID>,
    UserCredentialRepository {

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<UserCredential> findByEmail(String email);

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select uc from UserCredential uc where uc.id = :id")
  Optional<UserCredential> findByIdForUpdate(UUID id);
}
