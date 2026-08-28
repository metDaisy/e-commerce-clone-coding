package io.github.metdaisy.amaazon.address.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import io.github.metdaisy.amaazon.address.application.dto.request.AddressCreateRequest;
import io.github.metdaisy.amaazon.address.application.dto.request.AddressUpdateRequest;
import io.github.metdaisy.amaazon.address.application.dto.response.AddressResponse;
import io.github.metdaisy.amaazon.address.application.mapper.AddressMapper;
import io.github.metdaisy.amaazon.address.application.mapper.AddressMapperImpl;
import io.github.metdaisy.amaazon.common.mapper.UtilMapper;
import io.github.metdaisy.amaazon.address.domain.entity.Address;
import io.github.metdaisy.amaazon.address.domain.exception.AddressErrorCode;
import io.github.metdaisy.amaazon.address.domain.exception.AddressException;
import io.github.metdaisy.amaazon.address.domain.repository.AddressRepository;
import io.github.metdaisy.amaazon.common.dto.PageQuery;
import io.github.metdaisy.amaazon.common.dto.PageResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("주소 서비스 테스트")
class AddressServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();

  @Mock
  private AddressRepository repository;

  @Spy
  private AddressMapper addressMapper = new AddressMapperImpl(new UtilMapper() {});

  @InjectMocks
  private AddressService addressService;

  @Test
  @DisplayName("주소 등록 성공: 기본 배송지 요청이면 기존 기본 배송지를 해제하고 저장한다")
  void create_success_whenPrimaryIsRequested() {
    // given
    AddressCreateRequest request = validRequest(true);
    given(repository.existsByUserIdAndPostalCodeAndAddressLine(USER_ID, request.postalCode(),
        request.addressLine())).willReturn(false);
    given(repository.save(any(Address.class))).willAnswer(invocation -> invocation.getArgument(0));

    // when
    AddressResponse result = addressService.create(USER_ID, request);

    // then
    assertThat(result.recipientName()).isEqualTo(request.recipientName());
    assertThat(result.isPrimary()).isTrue();
    then(repository).should().existsByUserIdAndPostalCodeAndAddressLine(USER_ID,
        request.postalCode(), request.addressLine());
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
    given(repository.existsByUserIdAndPostalCodeAndAddressLine(USER_ID, request.postalCode(),
        request.addressLine())).willReturn(false);
    given(repository.save(any(Address.class))).willAnswer(invocation -> invocation.getArgument(0));

    // when
    addressService.create(USER_ID, request);

    // then
    then(repository).should(never()).clearPrimaryByUserId(USER_ID);
    ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
    then(repository).should().save(captor.capture());
    assertThat(captor.getValue().isPrimary()).isFalse();
  }

  @Test
  @DisplayName("주소 등록 실패: 동일한 주소면 저장하지 않고 ADDRESS-005를 던진다")
  void create_failure_whenAddressIsDuplicated() {
    // given
    AddressCreateRequest request = validRequest(false);
    given(repository.existsByUserIdAndPostalCodeAndAddressLine(USER_ID, request.postalCode(),
        request.addressLine())).willReturn(true);

    // when
    Throwable thrown = catchThrowable(() -> addressService.create(USER_ID, request));

    // then
    assertThat(thrown)
        .isInstanceOf(AddressException.class)
        .hasFieldOrPropertyWithValue("code", AddressErrorCode.ADDRESS_DUPLICATED.getCode());
    then(repository).should(never()).save(any(Address.class));
  }

  @Test
  @DisplayName("주소 목록 조회 성공: User의 주소 목록을 반환한다")
  void findAll_success() {
    // given
    Address address = Address.create(USER_ID, "집", "tester", "01012345678", "06236", "서울",
        true);
    given(repository.findPageByUserId(any(UUID.class), any())).willReturn(
        new PageResult<>(List.of(address), 0, 20, 1, 1));

    // when
    PageResult<AddressResponse> result = addressService.findAll(USER_ID, new PageQuery(0, 20));

    // then
    assertThat(result.content()).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(response(address));
    then(repository).should().findPageByUserId(any(UUID.class), any());
  }

  @Test
  @DisplayName("주소 수정 성공: 전달된 필드만 수정하고 나머지는 유지한다")
  void update_success_whenPartialFieldsAreSent() {
    // given
    Address address = Address.create(USER_ID, "집", "tester", "01012345678", "06236", "서울",
        false);
    AddressUpdateRequest request = new AddressUpdateRequest(
        null, "updated tester", null, null, "부산광역시 해운대구");
    given(repository.findById(address.getId())).willReturn(Optional.of(address));

    // when
    AddressResponse result = addressService.update(USER_ID, address.getId(), request);

    // then
    assertThat(result.recipientName()).isEqualTo("updated tester");
    assertThat(address)
        .extracting(Address::getRecipientName, Address::getRecipientPhone,
            Address::getPostalCode, Address::getAddressLine)
        .containsExactly("updated tester", "01012345678", "06236", "부산광역시 해운대구");
    then(repository).should(never()).save(any(Address.class));
  }

  @Test
  @DisplayName("주소 수정 실패: 다른 주소와 중복되면 저장하지 않고 ADDRESS-005를 던진다")
  void update_failure_whenAddressIsDuplicated() {
    // given
    Address address = Address.create(USER_ID, "집", "tester", "01012345678", "06236", "서울",
        false);
    AddressUpdateRequest request = new AddressUpdateRequest(
        null, null, null, "06237", "부산");
    given(repository.findById(address.getId())).willReturn(Optional.of(address));
    given(repository.existsByUserIdAndPostalCodeAndAddressLineAndIdNot(USER_ID, "06237", "부산",
        address.getId())).willReturn(true);

    // when
    Throwable thrown = catchThrowable(
        () -> addressService.update(USER_ID, address.getId(), request));

    // then
    assertThat(thrown)
        .isInstanceOf(AddressException.class)
        .hasFieldOrPropertyWithValue("code", AddressErrorCode.ADDRESS_DUPLICATED.getCode());
    assertThat(address.getPostalCode()).isEqualTo("06236");
    assertThat(address.getAddressLine()).isEqualTo("서울");
  }

  @Test
  @DisplayName("주소 수정 성공: 모든 필드가 null이어도 기존 값을 유지한다")
  void update_success_whenNoFieldsAreSent() {
    // given
    Address address = Address.create(USER_ID, "집", "tester", "01012345678", "06236", "서울",
        false);
    given(repository.findById(address.getId())).willReturn(Optional.of(address));

    // when
    AddressResponse result = addressService.update(USER_ID, address.getId(),
        new AddressUpdateRequest(null, null, null, null, null));

    // then
    assertThat(result.recipientName()).isEqualTo("tester");
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
    Address address = Address.create(ownerId, "집", "tester", "01012345678", "06236", "서울",
        false);
    given(repository.findById(address.getId())).willReturn(Optional.of(address));

    // when
    Throwable thrown = catchThrowable(() -> addressService.update(USER_ID, address.getId(),
        new AddressUpdateRequest(null, "updated", null, null, null)));

    // then
    assertThat(thrown)
        .isInstanceOf(AddressException.class)
        .hasFieldOrPropertyWithValue("code", AddressErrorCode.ADDRESS_ACCESS_DENIED.getCode());
    then(repository).should(never()).save(any(Address.class));
  }

  @Test
  @DisplayName("주소 삭제 성공: 기본 배송지를 삭제하면 최근 사용한 주소를 기본 배송지로 승격한다")
  void delete_success_whenPrimaryAddressIsDeleted() {
    // given
    Address primary = Address.create(USER_ID, "집", "primary", "01012345678", "06236", "서울",
        true);
    given(repository.existsById(primary.getId())).willReturn(true);
    given(repository.existsByIdAndUserId(primary.getId(), USER_ID)).willReturn(true);

    // when
    addressService.delete(USER_ID, primary.getId());

    // then
    then(repository).should().deleteAndUpdatePrimary(USER_ID, primary.getId());
    then(repository).should(never()).save(any(Address.class));
  }

  @Test
  @DisplayName("기본 배송지 지정 성공: 기존 기본 배송지를 해제하고 Address를 지정한다")
  void makePrimary_success() {
    // given
    Address address = Address.create(USER_ID, "집", "tester", "01012345678", "06236", "서울",
        false);
    given(repository.existsById(address.getId())).willReturn(true);
    given(repository.existsByIdAndUserId(address.getId(), USER_ID)).willReturn(true);
    address.setPrimary(true);
    given(repository.makePrimary(USER_ID, address.getId())).willReturn(address);

    // when
    AddressResponse result = addressService.makePrimary(USER_ID, address.getId());

    // then
    assertThat(result.isPrimary()).isTrue();
    assertThat(result.recipientName()).isEqualTo("tester");
    assertThat(address.isPrimary()).isTrue();
    then(repository).should().makePrimary(USER_ID, address.getId());
    then(repository).should(never()).save(any(Address.class));
  }

  private static AddressCreateRequest validRequest(boolean isPrimary) {
    return new AddressCreateRequest("집", "tester", "01012345678", "06236",
        "서울특별시 강남구", isPrimary);
  }

  private static AddressResponse response(Address address) {
    return new AddressResponse(address.getId(), USER_ID, address.getAlias(),
        address.getRecipientName(),
        address.getRecipientPhone(), address.getPostalCode(), address.getAddressLine(),
        address.isPrimary(), address.getLastUsedAt(), address.getCreatedAt(),
        address.getUpdatedAt());
  }
}
