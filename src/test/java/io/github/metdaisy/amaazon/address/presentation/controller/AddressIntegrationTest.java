package io.github.metdaisy.amaazon.address.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.metdaisy.amaazon.global.security.jwt.model.JwtPrincipal;
import io.github.metdaisy.amaazon.support.BaseIntegrationTest;
import io.github.metdaisy.amaazon.address.application.dto.request.AddressCreateRequest;
import io.github.metdaisy.amaazon.address.domain.entity.Address;
import io.github.metdaisy.amaazon.user.domain.entity.User;
import io.github.metdaisy.amaazon.address.domain.repository.AddressRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.ResultActions;

@DisplayName("Address HTTP 통합 테스트")
class AddressIntegrationTest extends BaseIntegrationTest {

  private static final String ADDRESS_URL = "/api/v1/me/addresses";
  private static final UUID USER_ID =
      UUID.fromString("2bb8df7f-9478-4d51-b055-496016dd421f");

  @Autowired
  private AddressRepository addressRepository;

  @Test
  @DisplayName("주소 목록 조회 성공: HTTP 요청으로 DB의 주소 목록을 정렬해 반환한다")
  void findAll_readsAddressesFromDatabase() throws Exception {
    // given
    persistAndFlush(User.createUser(USER_ID, "tester", "01012345678"));
    persistAndFlush(address(false));
    persistAndFlush(address(true));
    clear();

    // when & then
    mockMvc.perform(get(ADDRESS_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAs(USER_ID))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].isPrimary").value(true));
  }

  @Test
  @DisplayName("주소 목록 조회 실패: 비활성 User면 403 USER-004를 반환한다")
  void findAll_rejectsDisabledUser() throws Exception {
    // given
    User user = User.createUser(USER_ID, "tester", "01012345678");
    user.deactivate();
    persistAndFlush(user);
    clear();

    // when & then
    mockMvc.perform(get(ADDRESS_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAs(USER_ID))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.exceptionCode").value("USER-004"));
  }

  @Test
  @DisplayName("주소 등록 성공: 기본 배송지를 등록하면 기존 기본 배송지를 해제하고 DB에 저장한다")
  void create_primaryAddress_updatesDatabase() throws Exception {
    // given
    persistAndFlush(User.createUser(USER_ID, "tester", "01012345678"));
    Address oldPrimary = persistAndFlush(address(true));
    clear();
    AddressCreateRequest request = new AddressCreateRequest(
        "new tester", "01098765432", "06237", "서울특별시 강남구 테헤란로", true);

    // when
    mockMvc.perform(post(ADDRESS_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAs(USER_ID)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
        .andExpect(jsonPath("$.recipientName").value(request.recipientName()))
        .andExpect(jsonPath("$.isPrimary").value(true));
    flushAndClear();

    // then
    List<Address> addresses = addressRepository.findByUserId(USER_ID);
    assertThat(addresses).hasSize(2);
    assertThat(addresses).filteredOn(Address::isPrimary).hasSize(1);
    assertThat(addresses).filteredOn(address -> address.getId().equals(oldPrimary.getId()))
        .singleElement()
        .extracting(Address::isPrimary)
        .isEqualTo(false);
  }

  @Test
  @DisplayName("주소 등록 실패: 주소가 5개면 400 ADDRESS-006을 반환하고 저장하지 않는다")
  void create_rejectsMoreThanFiveAddresses() throws Exception {
    // given
    persistAndFlush(User.createUser(USER_ID, "tester", "01012345678"));
    for (int index = 0; index < 5; index++) {
      persistAndFlush(Address.create(USER_ID, "tester" + index, "01012345678", "06236",
          "서울특별시 강남구 " + index, false));
    }
    clear();
    AddressCreateRequest request = new AddressCreateRequest(
        "new tester", "01098765432", "06237", "서울특별시 강남구 테헤란로", false);

    // when
    ResultActions result = mockMvc.perform(post(ADDRESS_URL)
        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAs(USER_ID)))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)));

    // then
    result
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionCode").value("ADDRESS-006"));
    flushAndClear();
    assertThat(addressRepository.countByUserId(USER_ID)).isEqualTo(5);
  }

  @Test
  @DisplayName("주소 등록 실패: 필수 값이 없으면 400 INVALID_INPUT을 반환한다")
  void create_rejectsInvalidRequest() throws Exception {
    // given
    persistAndFlush(User.createUser(USER_ID, "tester", "01012345678"));
    clear();
    AddressCreateRequest request = new AddressCreateRequest(
        null, "01098765432", "06237", "서울특별시 강남구 테헤란로", false);

    // when & then
    mockMvc.perform(post(ADDRESS_URL)
            .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationAs(USER_ID)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionCode").value("INVALID_INPUT"));
  }

  private Address address(boolean isPrimary) {
    return Address.create(USER_ID, "tester", "01012345678", "06236", "서울특별시 강남구", isPrimary);
  }

  private Authentication authenticationAs(UUID userId) {
    JwtPrincipal principal = new JwtPrincipal(userId, "USER");
    return new UsernamePasswordAuthenticationToken(
        principal, null, principal.getAuthorities());
  }
}
