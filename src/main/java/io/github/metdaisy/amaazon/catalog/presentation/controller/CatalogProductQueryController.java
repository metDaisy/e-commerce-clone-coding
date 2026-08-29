package io.github.metdaisy.amaazon.catalog.presentation.controller;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CatalogProductPageRequest;
import io.github.metdaisy.amaazon.catalog.application.service.CatalogProductQueryService;
import io.github.metdaisy.amaazon.catalog.application.validator.ActiveSeller;
import io.github.metdaisy.amaazon.catalog.presentation.dto.CatalogProductQueryResponse;
import io.github.metdaisy.amaazon.catalog.presentation.mapper.CatalogProductPresentationMapper;
import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import io.github.metdaisy.amaazon.common.auth.RequireEnabledUser;
import io.github.metdaisy.amaazon.common.dto.PageResult;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/catalog-products")
@RequireEnabledUser
@RequiredArgsConstructor
public class CatalogProductQueryController {

  private final CatalogProductQueryService service;
  private final CatalogProductPresentationMapper presentationMapper;

  @GetMapping
  @ActiveSeller
  public ResponseEntity<PageResult<CatalogProductQueryResponse>> findAll(
      @Valid @ModelAttribute CatalogProductPageRequest request,
      @AuthenticationPrincipal AmaazonPrincipal principal) {
    boolean admin = principal.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    PageResult<CatalogProductQueryResponse> result = presentationMapper.toQueryResponse(
        service.findPage(request, admin));
    return ResponseEntity.ok(result);
  }

  @GetMapping("/{id}")
  @ActiveSeller
  public ResponseEntity<CatalogProductQueryResponse> find(
      @PathVariable UUID id,
      @AuthenticationPrincipal AmaazonPrincipal principal) {
    boolean admin = principal.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    return ResponseEntity.ok(presentationMapper.toQueryResponse(service.find(id, admin)));
  }
}
