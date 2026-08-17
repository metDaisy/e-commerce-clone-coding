package io.github.metdaisy.amaazon.auth.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import io.github.metdaisy.amaazon.auth.application.dto.AuthUserDto;
import io.github.metdaisy.amaazon.auth.application.port.out.AuthUserPort;
import io.github.metdaisy.amaazon.auth.domain.entity.SocialCredential;
import io.github.metdaisy.amaazon.auth.domain.repository.SocialCredentialRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.RestOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("SocialUserDetailsService 테스트")
class SocialUserDetailsServiceTest {

  @Mock
  private SocialCredentialRepository repository;

  @Mock
  private AuthUserPort userPort;

  @InjectMocks
  private SocialUserDetailsService service;

  @Test
  @DisplayName("loadUser - 성공")
  void loadUser_success() {
    // given
    RestOperations restOperations = mock(RestOperations.class);
    service.setRestOperations(restOperations);

    ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("google")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .clientId("client")
        .clientSecret("secret")
        .authorizationUri("http://uri")
        .tokenUri("http://uri")
        .userInfoUri("http://uri")
        .redirectUri("http://uri")
        .userNameAttributeName("id")
        .build();
    OAuth2AccessToken token = new OAuth2AccessToken(TokenType.BEARER, "token", null, null);
    OAuth2UserRequest request = new OAuth2UserRequest(clientRegistration, token);

    ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(Map.of("id", "provider-id", "provider", "google"));
    given(restOperations.exchange(any(RequestEntity.class), eq(new ParameterizedTypeReference<Map<String, Object>>() {})))
        .willReturn(response);

    UUID userId = UUID.randomUUID();
    SocialCredential credential = mock(SocialCredential.class);
    given(credential.getUserId()).willReturn(userId);

    given(repository.findByProviderIdAndProvider(any(), any()))
        .willReturn(Optional.of(credential));

    AuthUserDto userDto = new AuthUserDto(userId, List.of("USER"), true);
    given(userPort.loadUser(any())).willReturn(Optional.of(userDto));

    // when
    OAuth2User result = service.loadUser(request);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getAuthorities()).isNotEmpty();
  }
}
