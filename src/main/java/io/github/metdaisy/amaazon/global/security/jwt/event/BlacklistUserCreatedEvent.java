package io.github.metdaisy.amaazon.global.security.jwt.event;

import java.time.Instant;
import java.util.UUID;

public record BlacklistUserCreatedEvent(UUID userId, Instant compromisedAt) {

}
