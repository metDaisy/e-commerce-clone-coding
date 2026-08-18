package io.github.metdaisy.amaazon.address.application.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AddressResponse(
    UUID id,
    UUID userId,
    String recipientName,
    String recipientPhone,
    String postalCode,
    String addressLine,
    boolean isPrimary,
    Instant createdAt,
    Instant updatedAt) {
}
