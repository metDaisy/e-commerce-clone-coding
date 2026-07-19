package io.github.metdaisy.amaazon.global.security.jwt.event;

import java.time.Instant;
import org.springframework.context.ApplicationEvent;
import org.springframework.modulith.NamedInterface;

@NamedInterface("blacklist")
public record BlacklistTokenCreatedEvent(String jti, Instant expiredAt) {
}
