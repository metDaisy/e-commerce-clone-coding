package io.github.metdaisy.amaazon.auth.application.event;

import java.util.UUID;

public record SocialSignUpTask(UUID userId, String guestToken) {

}
