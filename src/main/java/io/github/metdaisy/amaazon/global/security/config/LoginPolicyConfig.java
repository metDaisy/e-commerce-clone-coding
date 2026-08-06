package io.github.metdaisy.amaazon.global.security.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LoginPolicyProperties.class)
public class LoginPolicyConfig {

}
