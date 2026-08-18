package io.github.metdaisy.amaazon.address.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.metdaisy.amaazon.support.RestControllerTest;
import io.github.metdaisy.amaazon.address.application.dto.request.AddressCreateRequest;
import io.github.metdaisy.amaazon.address.application.dto.request.AddressUpdateRequest;
import io.github.metdaisy.amaazon.address.application.dto.response.AddressResponse;
import io.github.metdaisy.amaazon.address.application.service.AddressService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(AddressController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("주소 컨트롤러 슬라이스 테스트")
class AddressControllerTest extends RestControllerTest {

  private static final String ADDRESS_URL = API_PREFIX + "/me/addresses";

  @MockitoBean
  private AddressService addressService;

  @Test
  @DisplayName("주소 목록 조회 성공: 인증 주체의 주소 목록을 반환한다")
  void findAll_success() throws Exception {
    // given
    AddressResponse response = response();
    given(addressService.findAll(USER_ID)).willReturn(List.of(response));

    // when
    MockHttpServletRequestBuilder request = get(ADDRESS_URL);

    // then
    mockMvc.perform(request)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(response.id().toString()))
        .andExpect(jsonPath("$[0].userId").value(USER_ID.toString()))
        .andExpect(jsonPath("$[0].recipientName").value("tester"))
        .andExpect(jsonPath("$[0].isPrimary").value(true));
    then(addressService).should().findAll(USER_ID);
  }

  @Test
  @DisplayName("주소 등록 성공: 인증 주체 ID와 요청을 서비스에 전달하고 201을 반환한다")
  void create_success() throws Exception {
    // given
    AddressCreateRequest request = new AddressCreateRequest(
        "tester", "01012345678", "06236", "서울특별시 강남구", true);
    AddressResponse response = response();
    given(addressService.create(USER_ID, request)).willReturn(response);

    // when & then
    mockMvc.perform(postJson(ADDRESS_URL, request))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(response.id().toString()))
        .andExpect(jsonPath("$.isPrimary").value(true));
    then(addressService).should().create(USER_ID, request);
  }

  @Test
  @DisplayName("주소 수정 성공: 인증 주체 ID와 Address ID를 서비스에 전달한다")
  void update_success() throws Exception {
    // given
    UUID addressId = response().id();
    AddressUpdateRequest request = new AddressUpdateRequest(
        "updated tester", null, null, "부산광역시 해운대구");
    AddressResponse response = response();
    given(addressService.update(USER_ID, addressId, request)).willReturn(response);

    // when & then
    mockMvc.perform(patch(ADDRESS_URL + "/" + addressId)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(addressId.toString()));
    then(addressService).should().update(USER_ID, addressId, request);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("invalidUpdateRequests")
  @DisplayName("주소 수정 실패: 공백 또는 최대 길이를 초과하면 Web 계층에서 400을 반환한다")
  void update_failure_whenRequestIsInvalid(String caseName, AddressUpdateRequest request)
      throws Exception {
    // given
    UUID addressId = response().id();

    // when & then
    mockMvc.perform(patch(ADDRESS_URL + "/" + addressId)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionCode").value("INVALID_INPUT"));
    then(addressService).should(never()).update(any(UUID.class), any(UUID.class),
        any(AddressUpdateRequest.class));
  }

  @Test
  @DisplayName("주소 삭제 성공: 인증 주체 ID와 Address ID를 서비스에 전달하고 204를 반환한다")
  void delete_success() throws Exception {
    // given
    UUID addressId = response().id();

    // when & then
    mockMvc.perform(delete(ADDRESS_URL + "/" + addressId))
        .andExpect(status().isNoContent());
    then(addressService).should().delete(USER_ID, addressId);
  }

  @Test
  @DisplayName("기본 배송지 지정 성공: 인증 주체 ID와 Address ID를 서비스에 전달한다")
  void makePrimary_success() throws Exception {
    // given
    UUID addressId = response().id();
    AddressResponse response = response();
    given(addressService.makePrimary(USER_ID, addressId)).willReturn(response);

    // when & then
    mockMvc.perform(post(ADDRESS_URL + "/" + addressId + "/default"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(addressId.toString()));
    then(addressService).should().makePrimary(USER_ID, addressId);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("invalidRequests")
  @DisplayName("주소 등록 실패: 필수 값 또는 길이가 유효하지 않으면 Web 계층에서 400을 반환한다")
  void create_failure_whenRequestIsInvalid(String caseName, AddressCreateRequest request)
      throws Exception {
    // when & then
    mockMvc.perform(postJson(ADDRESS_URL, request))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionCode").value("INVALID_INPUT"));
    then(addressService).should(never()).create(any(UUID.class), any(AddressCreateRequest.class));
  }

  private static Stream<Arguments> invalidRequests() {
    return Stream.of(
        Arguments.of("수령인 이름 누락",
            new AddressCreateRequest(null, "01012345678", "06236", "서울", false)),
        Arguments.of("수령인 연락처 공백",
            new AddressCreateRequest("tester", " ", "06236", "서울", false)),
        Arguments.of("주소 본문 길이 초과",
            new AddressCreateRequest("tester", "01012345678", "06236", "a".repeat(256), false)));
  }

  private static Stream<Arguments> invalidUpdateRequests() {
    return Stream.of(
        Arguments.of("수령인 이름 공백", new AddressUpdateRequest(" ", null, null, null)),
        Arguments.of("수령인 이름 길이 초과",
            new AddressUpdateRequest("a".repeat(101), null, null, null)),
        Arguments.of("수령인 연락처 공백", new AddressUpdateRequest(null, " ", null, null)),
        Arguments.of("수령인 연락처 길이 초과",
            new AddressUpdateRequest(null, "1".repeat(21), null, null)),
        Arguments.of("우편번호 공백", new AddressUpdateRequest(null, null, " ", null)),
        Arguments.of("우편번호 길이 초과",
            new AddressUpdateRequest(null, null, "1".repeat(21), null)),
        Arguments.of("주소 본문 공백", new AddressUpdateRequest(null, null, null, " ")),
        Arguments.of("주소 본문 길이 초과",
            new AddressUpdateRequest(null, null, null, "a".repeat(256))));
  }

  private static AddressResponse response() {
    UUID addressId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    return new AddressResponse(addressId, USER_ID, "tester", "01012345678", "06236",
        "서울특별시 강남구", true, null, null);
  }
}
