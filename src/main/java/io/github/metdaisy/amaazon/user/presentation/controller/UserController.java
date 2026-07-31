package io.github.metdaisy.amaazon.user.presentation.controller;

import io.github.metdaisy.amaazon.user.application.dto.request.UserUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.github.metdaisy.amaazon.common.auth.AmaazonPrincipal;
import io.github.metdaisy.amaazon.user.application.dto.UserCreateRequest;
import io.github.metdaisy.amaazon.user.application.service.UserService;
import io.github.metdaisy.amaazon.user.presentation.dto.response.UserResponse;
import io.github.metdaisy.amaazon.user.presentation.mapper.UserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

  private final UserMapper mapper;
  private final UserService service;

  @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UserResponse> create(@RequestBody @Valid UserCreateRequest request) {
    return ResponseEntity.status(HttpStatus.OK).body(mapper.toDto(service.create(request)));
  }

  @PostMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UserResponse> update(
      @AuthenticationPrincipal AmaazonPrincipal principal,
      @RequestBody @Valid UserUpdateRequest request) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(mapper.toDto(service.update(principal.getId(), request)));
  }
}
