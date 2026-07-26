package io.github.metdaisy.amaazon.auth.infra.security;

import io.github.metdaisy.amaazon.auth.application.port.out.AuthUserPort;
import io.github.metdaisy.amaazon.auth.domain.repository.SocialCredentialRepository;
import java.util.Map;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class SocialUserDetailsService extends DefaultOAuth2UserService {

  private final SocialCredentialRepository repository;
  private final AuthUserPort userPort;

  public SocialUserDetailsService(SocialCredentialRepository repository, AuthUserPort userPort) {
    this.repository = repository;
    this.userPort = userPort;
    super.setAttributesConverter(
            request -> OAuth2Provider.from(request.getClientRegistration().getRegistrationId()));
  }

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(userRequest);
    Map<String, Object> attributes = oAuth2User.getAttributes();
    String provider = (String) attributes.get("provider");
    String providerId = (String) attributes.get("providerId");
    return repository.findByProviderIdAndProvider(providerId, provider)
            .flatMap(credential -> userPort.loadUser(credential.getUserId()))
            .map(dto -> SocialUserDetails.create(dto.id(), dto.role(), attributes))
            .orElse(SocialUserDetails.createGuest(attributes));
  }
}
