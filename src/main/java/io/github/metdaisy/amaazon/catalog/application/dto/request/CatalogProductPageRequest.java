package io.github.metdaisy.amaazon.catalog.application.dto.request;

import io.github.metdaisy.amaazon.catalog.domain.entity.constant.CatalogProductSort;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ArchiveStatus;
import io.github.metdaisy.amaazon.common.dto.PageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record CatalogProductPageRequest(
    @Min(value = 0, message = "page는 0 이상이어야 합니다.") Integer page,
    @Min(value = 1, message = "size는 1 이상이어야 합니다.")
    @Max(value = 100, message = "size는 100 이하여야 합니다.") Integer size,
    String keyword,
    UUID categoryId,
    String tag,
    ArchiveStatus catalogPublicationStatus,
    ArchiveStatus variantPublicationStatus,
    @Pattern(regexp = "LATEST|NAME_ASC|NAME_DESC", message = "sort 값이 유효하지 않습니다.")
    String sort) {

  public CatalogProductPageRequest {
    page = page == null ? 0 : page;
    size = size == null ? 20 : size;
    sort = sort == null ? CatalogProductSort.LATEST.name() : sort;
  }

  public PageQuery toPageQuery() {
    return new PageQuery(page, size);
  }

  public CatalogProductSort toSort() {
    return CatalogProductSort.valueOf(sort);
  }
}
