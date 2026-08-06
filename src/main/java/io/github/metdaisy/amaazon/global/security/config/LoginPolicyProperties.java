package io.github.metdaisy.amaazon.global.security.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.modulith.NamedInterface;

@NamedInterface("login-policy")
@ConfigurationProperties(prefix = "amaazon.login")
public record LoginPolicyProperties(int maxAttempt, Duration lockedDuration) {

}
