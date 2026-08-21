package io.github.metdaisy.amaazon.address.presentation.controller;

import io.github.metdaisy.amaazon.address.application.dto.request.AddressCreateRequest;
import io.github.metdaisy.amaazon.address.application.dto.request.AddressPageRequest;
import io.github.metdaisy.amaazon.address.application.dto.request.AddressUpdateRequest;
import io.github.metdaisy.amaazon.address.application.dto.response.AddressResponse;
import io.github.metdaisy.amaazon.address.application.service.AddressService;
import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import io.github.metdaisy.amaazon.common.auth.RequireEnabledUser;
import io.github.metdaisy.amaazon.common.dto.PageResponse;
import io.github.metdaisy.amaazon.common.dto.PageResult;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/addresses")
@RequireEnabledUser
@RequiredArgsConstructor
public class AddressController {

  private final AddressService service;

  @GetMapping
  public ResponseEntity<PageResponse<AddressResponse>> findAll(
      @AuthenticationPrincipal AmaazonPrincipal principal,
      @Valid @ModelAttribute AddressPageRequest request) {
    PageResult<AddressResponse> result = service.findAll(principal.getId(),
        request.toPageQuery());
    return ResponseEntity.ok(new PageResponse<>(result.content(), result.page(), result.size(),
        result.totalElements(), result.totalPages()));
  }

  @PostMapping
  public ResponseEntity<AddressResponse> create(
      @AuthenticationPrincipal AmaazonPrincipal principal,
      @RequestBody @Valid AddressCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.create(principal.getId(), request));
  }

  @PatchMapping("/{addressId}")
  public ResponseEntity<AddressResponse> update(
      @AuthenticationPrincipal AmaazonPrincipal principal,
      @PathVariable UUID addressId,
      @RequestBody @Valid AddressUpdateRequest request) {
    return ResponseEntity.ok(service.update(principal.getId(), addressId, request));
  }

  @DeleteMapping("/{addressId}")
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal AmaazonPrincipal principal,
      @PathVariable UUID addressId) {
    service.delete(principal.getId(), addressId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{addressId}/default")
  public ResponseEntity<AddressResponse> makePrimary(
      @AuthenticationPrincipal AmaazonPrincipal principal,
      @PathVariable UUID addressId) {
    return ResponseEntity.ok(service.makePrimary(principal.getId(), addressId));
  }

}
