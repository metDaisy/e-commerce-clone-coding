package io.github.metdaisy.amaazon.catalog.presentation.controller;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductIdentifierUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductIdentifierUpdateResponse;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CatalogProductResponse;
import io.github.metdaisy.amaazon.catalog.application.service.CatalogProductService;
import io.github.metdaisy.amaazon.catalog.application.validator.ActiveSeller;
import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/catalog-products")
@RequiredArgsConstructor
public class CatalogProductController {

  private final CatalogProductService service;

  @PreAuthorize("hasAnyRole('PRODUCT_MANAGER', 'ADMIN')")
  @ActiveSeller
  @PostMapping
  public ResponseEntity<CatalogProductResponse> create(
      @AuthenticationPrincipal AmaazonPrincipal principal,
      @RequestBody @Valid CatalogProductCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.create(principal.getId(), request));
  }

  @PreAuthorize("hasAnyRole('PRODUCT_MANAGER', 'ADMIN')")
  @ActiveSeller(checkOwner = true)
  @PatchMapping("/{id}")
  public ResponseEntity<CatalogProductResponse> update(
      @PathVariable UUID id,
      @RequestBody @Valid CatalogProductUpdateRequest request) {
    return ResponseEntity.status(HttpStatus.OK).body(service.update(id, request));
  }

  @PreAuthorize("hasAnyRole('PRODUCT_MANAGER', 'ADMIN')")
  @ActiveSeller(checkOwner = true)
  @PatchMapping("/{id}/identifiers")
  public ResponseEntity<CatalogProductIdentifierUpdateResponse> verifyIdentifierCode(
      @PathVariable UUID id, @RequestBody CatalogProductIdentifierUpdateRequest request) {
    return ResponseEntity.status(HttpStatus.OK).body(service.updateIdentifier(id, request));
  }
}
