package io.github.metdaisy.amaazon.catalog.presentation.controller;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductPageRequest;
import io.github.metdaisy.amaazon.catalog.application.service.catalog.CatalogQueryService;
import io.github.metdaisy.amaazon.catalog.application.validator.ActiveSeller;
import io.github.metdaisy.amaazon.catalog.domain.entity.constant.ArchiveStatus;
import io.github.metdaisy.amaazon.catalog.presentation.dto.CatalogProductQueryResponse;
import io.github.metdaisy.amaazon.catalog.presentation.mapper.CatalogProductPresentationMapper;
import io.github.metdaisy.amaazon.common.auth.RequireEnabledUser;
import io.github.metdaisy.amaazon.common.dto.PageResult;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequireEnabledUser
@RequiredArgsConstructor
public class CatalogQueryController {

  private final CatalogQueryService service;
  private final CatalogProductPresentationMapper presentationMapper;

  @GetMapping("/catalog-products")
  @ActiveSeller
  public ResponseEntity<PageResult<CatalogProductQueryResponse>> findAllForProductManager(
      @Valid @ModelAttribute CatalogProductPageRequest request) {
    return ResponseEntity.ok(presentationMapper.toQueryResponse(
        service.findPageForProductManager(request)));
  }

  @GetMapping("/admin/catalog-products")
  public ResponseEntity<PageResult<CatalogProductQueryResponse>> findAllForAdmin(
      @Valid @ModelAttribute CatalogProductPageRequest request) {
    return ResponseEntity.ok(presentationMapper.toQueryResponse(
        service.findPageForAdmin(request)));
  }

  @GetMapping("/catalog-products/{id}")
  @ActiveSeller
  public ResponseEntity<CatalogProductQueryResponse> findForProductManager(
    @PathVariable UUID id) {
    return ResponseEntity.ok(
        presentationMapper.toQueryResponse(
            service.findForProductManager(id)));
  }

  @GetMapping("/admin/catalog-products/{id}")
  public ResponseEntity<CatalogProductQueryResponse> findForAdmin(@PathVariable UUID id) {
    return ResponseEntity.ok(presentationMapper.toQueryResponse(service.findForAdmin(id)));
  }
}
