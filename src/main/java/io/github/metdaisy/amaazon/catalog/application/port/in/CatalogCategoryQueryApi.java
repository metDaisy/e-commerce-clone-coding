package io.github.metdaisy.amaazon.catalog.application.port.in;

import io.github.metdaisy.amaazon.catalog.application.service.category.CategoryQueryService;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@NamedInterface("api")
@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogCategoryQueryApi {

  private final CategoryQueryService categoryQueryService;

  public Set<UUID> findSelfAndDescendantIds(UUID categoryId) {
    return categoryQueryService.findSelfAndDescendantIds(categoryId);
  }
}
