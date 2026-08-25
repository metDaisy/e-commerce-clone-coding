package io.github.metdaisy.amaazon.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PageResult<T>(
    @JsonProperty("data")
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages) {

}
