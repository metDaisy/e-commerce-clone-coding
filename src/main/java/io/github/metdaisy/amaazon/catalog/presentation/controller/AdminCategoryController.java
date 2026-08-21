package io.github.metdaisy.amaazon.catalog.presentation.controller;

import io.github.metdaisy.amaazon.catalog.application.dto.request.CategoryCreateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.request.CategoryUpdateRequest;
import io.github.metdaisy.amaazon.catalog.application.dto.response.CategoryResponse;
import io.github.metdaisy.amaazon.catalog.application.service.category.CategoryCommandService;
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
@RequestMapping("/admin/categories")
@RequireEnabledUser
@RequiredArgsConstructor
public class AdminCategoryController {

  private final CategoryCommandService service;

  @PostMapping
  public ResponseEntity<CategoryResponse> create(
      @RequestBody @Valid CategoryCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
  }

  @PatchMapping("/{categoryId}")
  public ResponseEntity<CategoryResponse> update(
      @PathVariable UUID categoryId,
      @RequestBody @Valid CategoryUpdateRequest request) {
    return ResponseEntity.ok(service.update(categoryId, request));
  }
}
