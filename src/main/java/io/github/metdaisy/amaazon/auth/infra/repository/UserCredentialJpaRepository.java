package io.github.metdaisy.amaazon.auth.infra.repository;

import io.github.metdaisy.amaazon.auth.domain.entity.UserCredential;
import io.github.metdaisy.amaazon.auth.domain.repository.UserCredentialRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCredentialJpaRepository extends JpaRepository<UserCredential, UUID>,
        UserCredentialRepository {

}
