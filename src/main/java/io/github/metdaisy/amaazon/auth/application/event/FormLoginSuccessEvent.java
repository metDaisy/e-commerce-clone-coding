package io.github.metdaisy.amaazon.auth.application.event;

import java.util.UUID;
import org.springframework.context.ApplicationEvent;

public record FormLoginSuccessEvent(UUID userId) {

}
