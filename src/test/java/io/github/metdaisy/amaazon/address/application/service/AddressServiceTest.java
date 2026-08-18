package io.github.metdaisy.amaazon.address.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

import io.github.metdaisy.amaazon.address.application.dto.request.AddressCreateRequest;
import io.github.metdaisy.amaazon.address.application.dto.response.AddressResponse;
import io.github.metdaisy.amaazon.address.application.mapper.AddressMapper;
import io.github.metdaisy.amaazon.address.domain.entity.Address;
import io.github.metdaisy.amaazon.address.domain.exception.AddressErrorCode;
import io.github.metdaisy.amaazon.address.domain.exception.AddressException;
import io.github.metdaisy.amaazon.address.domain.repository.AddressRepository;
import io.github.metdaisy.amaazon.user.application.port.in.UserQueryApi;
import io.github.metdaisy.amaazon.user.domain.exception.UserErrorCode;
import io.github.metdaisy.amaazon.user.domain.exception.UserException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("주소 서비스 테스트")
class AddressServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();

  @Mock
  private AddressRepository repository;

  @Mock
  private AddressMapper addressMapper;

  @Mock
  private UserQueryApi userQueryApi;

  @InjectMocks
  private AddressService addressService;

  @Test
  @DisplayName("주소 등록 성공: 기본 배송지 요청이면 기존 기본 배송지를 해제하고 저장한다")
  void create_success_whenPrimaryIsRequested() {
    // given
    AddressCreateRequest request = validRequest(true);
    Address address = Address.create(USER_ID, request.recipientName(), request.recipientPhone(),
        request.postalCode(), request.addressLine(), true);
    AddressResponse expected = response(address);
    given(repository.countByUserId(USER_ID)).willReturn(1L);
    given(repository.save(any(Address.class))).willReturn(address);
    given(addressMapper.toDto(address)).willReturn(expected);

    // when
    AddressResponse result = addressService.create(USER_ID, request);

    // then
    assertThat(result).isSameAs(expected);
    then(repository).should().countByUserId(USER_ID);
    then(repository).should().clearPrimaryByUserId(USER_ID);
    ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
    then(repository).should().save(captor.capture());
    assertThat(captor.getValue())
        .extracting(Address::getUserId, Address::getRecipientName, Address::isPrimary)
        .containsExactly(USER_ID, request.recipientName(), true);
  }

  @Test
  @DisplayName("주소 등록 성공: 기본 배송지를 생략하면 false로 저장한다")
  void create_success_whenPrimaryIsOmitted() {
    // given
    AddressCreateRequest request = validRequest(false);
    given(repository.countByUserId(USER_ID)).willReturn(0L);
    given(repository.save(any(Address.class))).willAnswer(invocation -> invocation.getArgument(0));
    given(addressMapper.toDto(any(Address.class))).willReturn(null);

    // when
    addressService.create(USER_ID, request);

    // then
    then(repository).should(never()).clearPrimaryByUserId(USER_ID);
    ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
    then(repository).should().save(captor.capture());
    assertThat(captor.getValue().isPrimary()).isFalse();
  }

  @Test
  @DisplayName("주소 등록 실패: User의 주소가 5개면 저장하지 않고 ADDRESS-006을 던진다")
  void create_failure_whenAddressLimitIsExceeded() {
    // given
    given(repository.countByUserId(USER_ID)).willReturn(5L);

    // when
    Throwable thrown = catchThrowable(() -> addressService.create(USER_ID, validRequest(false)));

    // then
    assertThat(thrown)
        .isInstanceOf(AddressException.class)
        .hasFieldOrPropertyWithValue("code", AddressErrorCode.ADDRESS_LIMIT_EXCEEDED.getCode());
    then(repository).should(never()).save(any(Address.class));
  }

  @Test
  @DisplayName("주소 접근 실패: 비활성 User면 USER-004를 던지고 조회하지 않는다")
  void findAll_failure_whenUserIsDisabled() {
    // given
    doThrow(new UserException(UserErrorCode.USER_DISABLED))
        .when(userQueryApi).requireEnabled(USER_ID);

    // when
    Throwable thrown = catchThrowable(() -> addressService.findAll(USER_ID));

    // then
    assertThat(thrown)
        .isInstanceOf(UserException.class)
        .hasFieldOrPropertyWithValue("code", "USER-004");
    then(repository).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("주소 등록 실패: 비활성 User면 USER-004를 던지고 저장하지 않는다")
  void create_failure_whenUserIsDisabled() {
    // given
    doThrow(new UserException(UserErrorCode.USER_DISABLED))
        .when(userQueryApi).requireEnabled(USER_ID);

    // when
    Throwable thrown = catchThrowable(
        () -> addressService.create(USER_ID, validRequest(false)));

    // then
    assertThat(thrown)
        .isInstanceOf(UserException.class)
        .hasFieldOrPropertyWithValue("code", "USER-004");
    then(repository).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("주소 목록 조회 성공: User의 주소 목록을 반환한다")
  void findAll_success() {
    // given
    Address address = Address.create(USER_ID, "tester", "01012345678", "06236", "서울", true);
    AddressResponse response = response(address);
    given(repository.findByUserId(any(UUID.class))).willReturn(List.of(address));
    given(addressMapper.toDto(address)).willReturn(response);

    // when
    List<AddressResponse> result = addressService.findAll(USER_ID);

    // then
    assertThat(result).containsExactly(response);
    then(repository).should().findByUserId(any(UUID.class));
  }

  private static AddressCreateRequest validRequest(boolean isPrimary) {
    return new AddressCreateRequest("tester", "01012345678", "06236", "서울특별시 강남구", isPrimary);
  }

  private static AddressResponse response(Address address) {
    return new AddressResponse(address.getId(), USER_ID, address.getRecipientName(),
        address.getRecipientPhone(), address.getPostalCode(), address.getAddressLine(),
        address.isPrimary(), address.getCreatedAt(), address.getUpdatedAt());
  }
}
