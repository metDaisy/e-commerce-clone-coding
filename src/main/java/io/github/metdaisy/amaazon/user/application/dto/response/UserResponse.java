package io.github.metdaisy.amaazon.user.application.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.github.metdaisy.amaazon.user.domain.entity.constant.UserRole;

public record UserResponse(
    UUID id,
    String name,
    String phoneNumber,
    List<UserRole> roles,
    boolean isEnabled,
    Instant createdAt,
    Instant updatedAt) {

}
