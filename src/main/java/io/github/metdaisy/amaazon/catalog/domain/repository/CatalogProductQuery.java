package io.github.metdaisy.amaazon.catalog.domain.repository;

import io.github.metdaisy.amaazon.catalog.domain.entity.CatalogProduct;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ArchiveStatus;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogProductSort;
import io.github.metdaisy.amaazon.common.dto.PageQuery;
import io.github.metdaisy.amaazon.common.dto.PageResult;
import java.util.Set;
import java.util.UUID;

public interface CatalogProductQuery {

  PageResult<CatalogProduct> findPage(Set<UUID> categoryIds, String keyword, String tag,
      CatalogProductSort sort, ArchiveStatus catalogPublicationStatus,
      ArchiveStatus variantPublicationStatus, PageQuery pageQuery);

  boolean existsIdentifier(UUID id, String type, String value);
}
