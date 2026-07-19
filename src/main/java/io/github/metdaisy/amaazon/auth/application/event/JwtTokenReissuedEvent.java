package io.github.metdaisy.amaazon.auth.application.event;

import java.util.UUID;

public record JwtTokenReissuedEvent(UUID userId, String refreshToken) {

}
