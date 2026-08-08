package io.github.metdaisy.amaazon.product.infra.repository;

import io.github.metdaisy.amaazon.product.domain.entity.Tag;
import io.github.metdaisy.amaazon.product.domain.repository.TagRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagJpaRepository extends JpaRepository<Tag, UUID>,
    TagRepository {

}
