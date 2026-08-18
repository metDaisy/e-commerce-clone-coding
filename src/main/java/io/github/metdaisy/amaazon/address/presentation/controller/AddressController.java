package io.github.metdaisy.amaazon.address.presentation.controller;

import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import io.github.metdaisy.amaazon.address.application.dto.request.AddressCreateRequest;
import io.github.metdaisy.amaazon.address.application.dto.response.AddressResponse;
import io.github.metdaisy.amaazon.address.application.service.AddressService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/addresses")
@RequiredArgsConstructor
public class AddressController {

  private final AddressService service;

  @GetMapping
  public ResponseEntity<List<AddressResponse>> findAll(
      @AuthenticationPrincipal AmaazonPrincipal principal) {
    return ResponseEntity.ok(service.findAll(principal.getId()));
  }

  @PostMapping
  public ResponseEntity<AddressResponse> create(
      @AuthenticationPrincipal AmaazonPrincipal principal,
      @RequestBody @Valid AddressCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.create(principal.getId(), request));
  }
}
