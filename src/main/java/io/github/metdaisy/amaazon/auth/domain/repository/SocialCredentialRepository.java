package io.github.metdaisy.amaazon.auth.domain.repository;

import io.github.metdaisy.amaazon.auth.domain.entity.SocialCredential;
import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import java.util.Optional;

public interface SocialCredentialRepository extends DomainRepository<SocialCredential> {

  Optional<SocialCredential> findByProviderIdAndProvider(String providerId, String provider);
}
