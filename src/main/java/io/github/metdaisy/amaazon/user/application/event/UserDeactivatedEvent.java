package io.github.metdaisy.amaazon.user.application.event;

import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

@NamedInterface("deactivation")
public record UserDeactivatedEvent(UUID eventId, UUID userId, Instant deactivatedAt) {

}
