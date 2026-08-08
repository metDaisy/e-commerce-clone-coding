package io.github.metdaisy.amaazon.product.application.service;

import io.github.metdaisy.amaazon.product.domain.entity.Tag;
import io.github.metdaisy.amaazon.product.domain.repository.TagRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagService {

  private final TagRepository repository;

  @Transactional
  public List<Tag> findAndCreate(List<String> names) {
    List<Tag> tags = new ArrayList<>(repository.findByNameIn(names));
    Set<String> tagNames = tags.stream().map(Tag::getName).collect(Collectors.toSet());
    Set<String> noNames = new HashSet<>(names);
    noNames.removeAll(tagNames);

    List<Tag> newTags = noNames.stream()
        .map(Tag::new)
        .toList();

    if (!newTags.isEmpty()) {
      repository.saveAll(newTags);
      tags.addAll(newTags);
    }

    return tags;
  }
}
