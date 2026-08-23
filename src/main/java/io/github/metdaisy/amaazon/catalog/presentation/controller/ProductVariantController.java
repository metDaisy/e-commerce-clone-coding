package io.github.metdaisy.amaazon.catalog.presentation.controller;

import io.github.metdaisy.amaazon.catalog.application.dto.response.ProductVariantResponse;
import io.github.metdaisy.amaazon.catalog.application.service.ProductVariantService;
import io.github.metdaisy.amaazon.common.auth.RequireEnabledUser;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/product-variants")
@RequireEnabledUser
@RequiredArgsConstructor
public class ProductVariantController {

  private final ProductVariantService service;

  @GetMapping("/{id}")
  public ResponseEntity<ProductVariantResponse> find(@PathVariable UUID id) {
    return ResponseEntity.ok(service.findPublic(id));
  }
}
