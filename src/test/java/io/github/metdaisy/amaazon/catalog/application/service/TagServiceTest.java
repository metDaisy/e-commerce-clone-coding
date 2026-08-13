package io.github.metdaisy.amaazon.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import io.github.metdaisy.amaazon.catalog.domain.entity.Tag;
import io.github.metdaisy.amaazon.catalog.domain.repository.TagRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("태그 서비스")
class TagServiceTest {

  @Mock
  private TagRepository repository;

  @InjectMocks
  private TagService service;

  @Test
  @DisplayName("태그 조회: 모든 태그가 이미 존재하면 저장하지 않고 반환한다")
  void findAndCreate_shouldReturnExistingTags_withoutSaving() {
    Tag existing = new Tag("office");
    given(repository.findByNameIn(Set.of("office"))).willReturn(List.of(existing));

    assertThat(service.findAndCreate(Set.of("office"))).containsExactly(existing);

    then(repository).should(never()).saveAll(any());
  }

  @Test
  @DisplayName("태그 생성: 누락된 태그만 새로 저장하고 기존 태그와 함께 반환한다")
  void findAndCreate_shouldSaveMissingTags() {
    Tag existing = new Tag("office");
    given(repository.findByNameIn(Set.of("office", "sale"))).willReturn(List.of(existing));

    List<Tag> result = service.findAndCreate(Set.of("office", "sale"));

    assertThat(result).extracting(Tag::getName).containsExactlyInAnyOrder("office", "sale");
    then(repository).should().saveAll(any());
  }

  @Test
  @DisplayName("태그 조회: tags가 null이면 빈 목록을 반환하고 저장소를 조회하지 않는다")
  void findAndCreate_shouldReturnEmptyList_whenNamesAreNull() {
    assertThat(service.findAndCreate(null)).isEmpty();

    then(repository).shouldHaveNoInteractions();
  }
}
