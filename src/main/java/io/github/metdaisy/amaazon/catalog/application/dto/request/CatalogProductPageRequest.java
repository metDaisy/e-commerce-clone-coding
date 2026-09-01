package io.github.metdaisy.amaazon.catalog.application.dto.request;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ArchiveStatus;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogProductSort;
import io.github.metdaisy.amaazon.common.dto.PageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record CatalogProductPageRequest(
    @Min(value = 0, message = "page must be greater than or equal to 0")
    Integer page,
    @Min(value = 1, message = "size must be greater than or equal to 1")
    @Max(value = 100, message = "size must be less than or equal to 100")
    Integer size,
    String keyword,
    UUID categoryId,
    String tag,
    ArchiveStatus catalogPublicationStatus,
    ArchiveStatus variantPublicationStatus,
    @Pattern(regexp = "LATEST|NAME_ASC|NAME_DESC", message = "sort value is invalid")
    String sort) {

  public CatalogProductPageRequest {
    page = page == null ? 0 : page;
    size = size == null ? 20 : size;
    catalogPublicationStatus = catalogPublicationStatus == null
        ? ArchiveStatus.ACTIVE : catalogPublicationStatus;
    variantPublicationStatus = variantPublicationStatus == null
        ? ArchiveStatus.ACTIVE : variantPublicationStatus;
    sort = sort == null ? CatalogProductSort.LATEST.name() : sort;
  }

  public PageQuery toPageQuery() {
    return new PageQuery(page, size);
  }

  public CatalogProductSort toSort() {
    return CatalogProductSort.valueOf(sort);
  }
}
