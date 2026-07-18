package io.github.metdaisy.amaazon.global.security.config;

import io.github.metdaisy.amaazon.global.security.constant.SecurityConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http,
          AuthenticationSuccessHandler loginSuccessHandler,
          AuthenticationFailureHandler loginFailureHandler) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(
                    session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(form ->
                    form.loginProcessingUrl(SecurityConstants.LOGIN_URL)
                            .successHandler(loginSuccessHandler)
                            .failureHandler(loginFailureHandler)
                            .usernameParameter(SecurityConstants.USERNAME_PARAMETER)
                            .passwordParameter(SecurityConstants.PASSWORD_PARAMETER))
            .logout(logout ->
                    logout.logoutUrl(SecurityConstants.LOGOUT_URL)
                            .permitAll())
            .httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth ->
                    auth.requestMatchers(HttpMethod.GET, SecurityConstants.PUBLIC_GET_PATHS).permitAll()
                            .requestMatchers(HttpMethod.POST, SecurityConstants.PUBLIC_POST_PATHS).permitAll()
                            .anyRequest().authenticated())
            .build();
  }
}
