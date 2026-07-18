package io.github.metdaisy.amaazon.global.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "amaazon.jwt")
public record JwtProperties(String secretKey,
                            long accessTokenExpiration,
                            long refreshTokenExpiration) {

}
