package io.github.metdaisy.amaazon.catalog.infra.repository;

import io.github.metdaisy.amaazon.catalog.domain.entity.Tag;
import io.github.metdaisy.amaazon.catalog.domain.repository.TagRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagJpaRepository extends JpaRepository<Tag, UUID>,
    TagRepository {

  @Override
  Optional<Tag> findById(UUID uuid);
}
