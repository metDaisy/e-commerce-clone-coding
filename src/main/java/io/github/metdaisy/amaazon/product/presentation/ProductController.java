package io.github.metdaisy.amaazon.product.presentation;

import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import io.github.metdaisy.amaazon.product.application.dto.ProductCreateRequest;
import io.github.metdaisy.amaazon.product.application.dto.ProductResponse;
import io.github.metdaisy.amaazon.product.application.service.ProductService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService service;

  @PreAuthorize("hasRole('PRODUCT_MANAGER')")
  @PostMapping
  public ResponseEntity<Void> create(
      @AuthenticationPrincipal AmaazonPrincipal principal,
      @RequestBody @Valid ProductCreateRequest request) {
    UUID productId = service.create(principal.getId(), request);
    URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
        .buildAndExpand(productId)
        .toUri();
    return ResponseEntity.created(location).build();
  }

  @GetMapping("/{id}")
  public ResponseEntity<ProductResponse> find(@PathVariable UUID id) {
    return ResponseEntity.status(HttpStatus.OK).body(service.find(id));
  }
}
