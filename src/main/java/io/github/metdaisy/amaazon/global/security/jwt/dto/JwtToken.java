package io.github.metdaisy.amaazon.global.security.jwt.dto;

import java.time.Instant;
import java.util.UUID;

public record JwtToken(UUID userId, String token, String device, Instant createdAt) {

}
