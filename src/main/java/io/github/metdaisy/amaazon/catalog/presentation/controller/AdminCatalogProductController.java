package io.github.metdaisy.amaazon.catalog.presentation.controller;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogIdentifierUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductUpdateRequest;
import io.github.metdaisy.amaazon.catalog.presentation.dto.CatalogIdentifierUpdateResponse;
import io.github.metdaisy.amaazon.catalog.presentation.dto.CatalogArchivedResponse;
import io.github.metdaisy.amaazon.catalog.presentation.dto.CatalogProductResponse;
import io.github.metdaisy.amaazon.catalog.presentation.mapper.CatalogProductPresentationMapper;
import io.github.metdaisy.amaazon.catalog.application.service.CatalogProductService;
import io.github.metdaisy.amaazon.common.auth.RequireEnabledUser;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/catalog-products")
@RequireEnabledUser
@RequiredArgsConstructor
public class AdminCatalogProductController {

  private final CatalogProductService service;
  private final CatalogProductPresentationMapper presentationMapper;

  @PostMapping
  public ResponseEntity<CatalogProductResponse> create(
      @RequestBody @Valid CatalogProductCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(presentationMapper.toResponse(service.create(request)));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<CatalogProductResponse> update(
      @PathVariable UUID id,
      @RequestBody @Valid CatalogProductUpdateRequest request) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(presentationMapper.toResponse(service.update(id, request)));
  }

  @PatchMapping("/{id}/identifiers")
  public ResponseEntity<CatalogIdentifierUpdateResponse> verifyIdentifierCode(
      @PathVariable UUID id,
      @RequestBody @Valid CatalogIdentifierUpdateRequest request) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(presentationMapper.toIdentifierResponse(
            service.updateIdentifier(id, request.identifiers())));
  }

  @PostMapping("/{id}/archive")
  public ResponseEntity<CatalogArchivedResponse> archive(@PathVariable UUID id) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(presentationMapper.toArchivedResponse(service.archive(id)));
  }
}
