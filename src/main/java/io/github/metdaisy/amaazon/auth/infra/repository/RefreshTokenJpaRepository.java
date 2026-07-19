package io.github.metdaisy.amaazon.auth.infra.repository;

import io.github.metdaisy.amaazon.auth.domain.entity.RefreshToken;
import io.github.metdaisy.amaazon.auth.domain.repository.RefreshTokenRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, UUID>,
        RefreshTokenRepository {

  @Override
  @Modifying
  @Query("delete from RefreshToken r where r.token = :jti")
  void deleteByTokenDirectly(String jti);
}
