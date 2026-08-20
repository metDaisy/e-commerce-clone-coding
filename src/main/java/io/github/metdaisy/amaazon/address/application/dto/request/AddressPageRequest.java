package io.github.metdaisy.amaazon.address.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.github.metdaisy.amaazon.common.dto.PageQuery;

public record AddressPageRequest(
  @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
  Integer page,
  @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
  @Max(value = 100, message = "페이지 크기는 100 이하이어야 합니다.")
  Integer size) {

  public AddressPageRequest {
    page = page == null ? 0 : page;
    size = size == null ? 20 : size;
  }

  public PageQuery toPageQuery() {
    return new PageQuery(page, size);
  }
}
