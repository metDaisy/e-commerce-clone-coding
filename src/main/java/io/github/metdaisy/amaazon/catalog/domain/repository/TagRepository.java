package io.github.metdaisy.amaazon.catalog.domain.repository;

import io.github.metdaisy.amaazon.catalog.domain.entity.Tag;
import io.github.metdaisy.amaazon.common.jpa.repository.DomainRepository;
import java.util.Collection;
import java.util.List;

public interface TagRepository extends DomainRepository<Tag> {

  List<Tag> findByNameIn(Collection<String> names);
}
