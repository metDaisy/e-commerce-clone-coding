package io.github.metdaisy.amaazon.address.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

import io.github.metdaisy.amaazon.address.application.dto.request.AddressCreateRequest;
import io.github.metdaisy.amaazon.address.application.dto.request.AddressUpdateRequest;
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
import java.util.Optional;
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

  @Test
  @DisplayName("주소 수정 성공: 전달된 필드만 수정하고 나머지는 유지한다")
  void update_success_whenPartialFieldsAreSent() {
    // given
    Address address = Address.create(USER_ID, "tester", "01012345678", "06236", "서울", false);
    AddressUpdateRequest request = new AddressUpdateRequest(
        "updated tester", null, null, "부산광역시 해운대구");
    AddressResponse expected = response(address);
    given(repository.findById(address.getId())).willReturn(Optional.of(address));
    given(addressMapper.toDto(address)).willReturn(expected);

    // when
    AddressResponse result = addressService.update(USER_ID, address.getId(), request);

    // then
    assertThat(result).isSameAs(expected);
    assertThat(address)
        .extracting(Address::getRecipientName, Address::getRecipientPhone,
            Address::getPostalCode, Address::getAddressLine)
        .containsExactly("updated tester", "01012345678", "06236", "부산광역시 해운대구");
    then(repository).should(never()).save(any(Address.class));
  }

  @Test
  @DisplayName("주소 수정 성공: 모든 필드가 null이어도 기존 값을 유지한다")
  void update_success_whenNoFieldsAreSent() {
    // given
    Address address = Address.create(USER_ID, "tester", "01012345678", "06236", "서울", false);
    AddressResponse expected = response(address);
    given(repository.findById(address.getId())).willReturn(Optional.of(address));
    given(addressMapper.toDto(address)).willReturn(expected);

    // when
    AddressResponse result = addressService.update(USER_ID, address.getId(),
        new AddressUpdateRequest(null, null, null, null));

    // then
    assertThat(result).isSameAs(expected);
    assertThat(address)
        .extracting(Address::getRecipientName, Address::getRecipientPhone,
            Address::getPostalCode, Address::getAddressLine)
        .containsExactly("tester", "01012345678", "06236", "서울");
    then(repository).should(never()).save(any(Address.class));
  }

  @Test
  @DisplayName("주소 수정 실패: 다른 User의 Address면 ADDRESS-008을 던진다")
  void update_failure_whenAddressBelongsToAnotherUser() {
    // given
    UUID ownerId = UUID.randomUUID();
    Address address = Address.create(ownerId, "tester", "01012345678", "06236", "서울", false);
    given(repository.findById(address.getId())).willReturn(Optional.of(address));

    // when
    Throwable thrown = catchThrowable(() -> addressService.update(USER_ID, address.getId(),
        new AddressUpdateRequest("updated", null, null, null)));

    // then
    assertThat(thrown)
        .isInstanceOf(AddressException.class)
        .hasFieldOrPropertyWithValue("code", AddressErrorCode.ADDRESS_ACCESS_DENIED.getCode());
    then(repository).should(never()).save(any(Address.class));
  }

  @Test
  @DisplayName("주소 삭제 성공: 기본 배송지를 삭제하면 최신 주소를 기본 배송지로 승격한다")
  void delete_success_whenPrimaryAddressIsDeleted() {
    // given
    Address primary = Address.create(USER_ID, "primary", "01012345678", "06236", "서울", true);
    Address nextPrimary = Address.create(USER_ID, "next", "01012345678", "06237", "부산", false);
    given(repository.findById(primary.getId())).willReturn(Optional.of(primary));
    given(repository.findByUserId(USER_ID)).willReturn(List.of(nextPrimary));

    // when
    addressService.delete(USER_ID, primary.getId());

    // then
    then(repository).should().delete(primary);
    assertThat(nextPrimary.isPrimary()).isTrue();
    then(repository).should(never()).save(any(Address.class));
  }

  @Test
  @DisplayName("기본 배송지 지정 성공: 기존 기본 배송지를 해제하고 Address를 지정한다")
  void makePrimary_success() {
    // given
    Address address = Address.create(USER_ID, "tester", "01012345678", "06236", "서울", false);
    AddressResponse expected = response(address);
    given(repository.existsById(address.getId())).willReturn(true);
    given(repository.existsByIdAndUserId(address.getId(), USER_ID)).willReturn(true);
    given(repository.makePrimaryByIdAndUserId(address.getId(), USER_ID)).willReturn(1);
    given(repository.findById(address.getId())).willReturn(Optional.of(address));
    given(addressMapper.toDto(address)).willReturn(expected);

    // when
    AddressResponse result = addressService.makePrimary(USER_ID, address.getId());

    // then
    assertThat(result).isSameAs(expected);
    then(repository).should().clearPrimaryByUserId(USER_ID);
    then(repository).should().makePrimaryByIdAndUserId(address.getId(), USER_ID);
    then(repository).should(never()).save(any(Address.class));
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
