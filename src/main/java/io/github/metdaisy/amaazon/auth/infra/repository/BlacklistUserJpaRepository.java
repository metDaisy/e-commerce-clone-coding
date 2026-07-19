package io.github.metdaisy.amaazon.auth.infra.repository;

import io.github.metdaisy.amaazon.auth.domain.entity.BlacklistUser;
import io.github.metdaisy.amaazon.auth.domain.repository.BlacklistUserRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlacklistUserJpaRepository extends JpaRepository<BlacklistUser, UUID>,
        BlacklistUserRepository {

  @Modifying
  @Query("DELETE FROM BlacklistUser u WHERE u.compromisedAt <= :compromisedAt")
  void deleteByCompromisedAtLessThanEqual(@Param("compromisedAt") java.time.Instant compromisedAt);
}
