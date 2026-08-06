package io.github.metdaisy.amaazon.auth.application.event;

import java.util.UUID;
import org.springframework.context.ApplicationEvent;

public record SocialSignUpTask(UUID userId, String guestToken) {

}
