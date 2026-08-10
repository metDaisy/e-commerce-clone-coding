package io.github.metdaisy.amaazon.catalog.presentation.controller;

import io.github.metdaisy.amaazon.catalog.application.dto.response.CategoryResponse;
import io.github.metdaisy.amaazon.catalog.application.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryService service;

  @GetMapping
  public ResponseEntity<List<CategoryResponse>> findAll() {
    return ResponseEntity.status(HttpStatus.OK).body(service.findAll());
  }
}
