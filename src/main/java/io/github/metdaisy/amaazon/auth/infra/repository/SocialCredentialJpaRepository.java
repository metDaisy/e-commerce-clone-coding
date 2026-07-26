package io.github.metdaisy.amaazon.auth.infra.repository;

import io.github.metdaisy.amaazon.auth.domain.entity.SocialCredential;
import io.github.metdaisy.amaazon.auth.domain.repository.SocialCredentialRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialCredentialJpaRepository extends JpaRepository<SocialCredential, UUID>,
        SocialCredentialRepository {

}
