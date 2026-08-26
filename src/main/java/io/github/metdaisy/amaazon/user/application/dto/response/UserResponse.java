package io.github.metdaisy.amaazon.user.application.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String name,
    String phoneNumber,
    List<String> roles,
    boolean isEnabled,
    Instant createdAt,
    Instant updatedAt) {

}
