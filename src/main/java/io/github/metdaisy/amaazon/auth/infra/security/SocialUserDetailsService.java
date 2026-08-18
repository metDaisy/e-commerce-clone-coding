package io.github.metdaisy.amaazon.auth.infra.security;

import io.github.metdaisy.amaazon.auth.domain.repository.SocialCredentialRepository;
import io.github.metdaisy.amaazon.user.application.port.in.UserQueryApi;
import java.util.Map;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class SocialUserDetailsService extends DefaultOAuth2UserService {

  private final SocialCredentialRepository repository;
  private final UserQueryApi userQueryApi;

  public SocialUserDetailsService(SocialCredentialRepository repository, UserQueryApi userQueryApi) {
    this.repository = repository;
    this.userQueryApi = userQueryApi;
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
        .flatMap(credential -> userQueryApi.findById(credential.getUserId()))
        .map(userDto -> SocialUserDetails.create(userDto.id(), userDto.rolesCsv(), attributes,
            userDto.isEnabled()))
        .orElse(SocialUserDetails.createGuest(attributes));
  }

}
