package io.github.metdaisy.amaazon.catalog.presentation.controller;

import io.github.metdaisy.amaazon.catalog.application.dto.request.ProductVariantCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.ProductVariantUpdateRequest;
import io.github.metdaisy.amaazon.catalog.presentation.dto.ProductVariantAdminResponse;
import io.github.metdaisy.amaazon.catalog.presentation.dto.ProductVariantArchivedResponse;
import io.github.metdaisy.amaazon.catalog.presentation.mapper.ProductVariantPresentationMapper;
import io.github.metdaisy.amaazon.catalog.application.service.ProductVariantService;
import io.github.metdaisy.amaazon.common.auth.RequireEnabledUser;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequireEnabledUser
@RequiredArgsConstructor
public class AdminProductVariantController {

  private final ProductVariantService service;
  private final ProductVariantPresentationMapper presentationMapper;

  @PostMapping("/catalog-products/{catalogProductId}/variants")
  public ResponseEntity<ProductVariantAdminResponse> create(
      @PathVariable UUID catalogProductId,
      @RequestBody @Valid ProductVariantCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(presentationMapper.toAdminResponse(service.create(catalogProductId, request)));
  }

  @GetMapping("/product-variants/{id}")
  public ResponseEntity<ProductVariantAdminResponse> find(@PathVariable UUID id) {
    return ResponseEntity.ok(presentationMapper.toAdminResponse(service.findAdmin(id)));
  }

  @PatchMapping("/product-variants/{id}")
  public ResponseEntity<ProductVariantAdminResponse> update(
      @PathVariable UUID id,
      @RequestBody @Valid ProductVariantUpdateRequest request) {
    return ResponseEntity.ok(presentationMapper.toAdminResponse(service.update(id, request)));
  }

  @PostMapping("/product-variants/{id}/archive")
  public ResponseEntity<ProductVariantArchivedResponse> archive(@PathVariable UUID id) {
    return ResponseEntity.ok(presentationMapper.toArchivedResponse(service.archive(id)));
  }
}
