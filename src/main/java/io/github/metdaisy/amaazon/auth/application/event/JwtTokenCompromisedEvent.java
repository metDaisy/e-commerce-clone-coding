package io.github.metdaisy.amaazon.auth.application.event;

import java.time.Instant;
import java.util.UUID;

public record JwtTokenCompromisedEvent(UUID userId, Instant compromisedAt) {

}
