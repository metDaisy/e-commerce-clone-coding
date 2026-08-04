package io.github.metdaisy.amaazon.global.security.jwt.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.modulith.NamedInterface;

@NamedInterface("jwt")
@ConfigurationProperties(prefix = "amaazon.jwt")
public record JwtTokenExpiration(
    Duration accessExpiration,
    Duration refreshExpiration,
    Duration guestExpiration) {

}
