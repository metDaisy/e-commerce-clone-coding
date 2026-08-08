package io.github.metdaisy.amaazon.product.domain.repository;

import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import io.github.metdaisy.amaazon.product.domain.entity.Tag;
import java.util.List;

public interface TagRepository extends DomainRepository<Tag> {

  List<Tag> findByNameIn(List<String> names);
}
