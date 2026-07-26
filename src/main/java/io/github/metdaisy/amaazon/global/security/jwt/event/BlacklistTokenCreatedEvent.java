package io.github.metdaisy.amaazon.global.security.jwt.event;

import java.time.Instant;

public record BlacklistTokenCreatedEvent(String jti, Instant expiredAt) {

}
