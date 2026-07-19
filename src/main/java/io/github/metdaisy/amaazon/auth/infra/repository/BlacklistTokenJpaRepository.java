package io.github.metdaisy.amaazon.auth.infra.repository;

import io.github.metdaisy.amaazon.auth.domain.entity.BlacklistToken;
import io.github.metdaisy.amaazon.auth.domain.repository.BlacklistTokenRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlacklistTokenJpaRepository extends JpaRepository<BlacklistToken, UUID>,
        BlacklistTokenRepository {

  @Modifying
  @Query("DELETE FROM BlacklistToken t WHERE t.expiredAt <= :expiredAt")
  void deleteByExpiredAtLessThanEqual(@Param("expiredAt") java.time.Instant expiredAt);
}
