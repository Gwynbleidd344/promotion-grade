package api.poja.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import api.poja.app.endpoint.rest.dto.StudentGroupCreateRequest;
import api.poja.app.entity.StudentGroup;
import api.poja.app.repository.StudentGroupRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class StudentGroupServiceTest {

  @Mock private StudentGroupRepository studentGroupRepository;

  private StudentGroupService service;

  @BeforeEach
  void setUp() {
    service = new StudentGroupService(studentGroupRepository);
  }

  private StudentGroup group(String reference) {
    var entity = new StudentGroup();
    entity.setId(UUID.randomUUID());
    entity.setReference(reference);
    entity.setName("Groupe " + reference);
    return entity;
  }

  @Test
  void lists_groups_from_paginated_repository() {
    var page = new PageImpl<>(List.of(group("K1"), group("K2")));
    when(studentGroupRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);

    var result = service.list(0, 10);

    assertThat(result).extracting("reference").containsExactly("K1", "K2");
  }

  @Test
  void creates_group_when_reference_is_unique() {
    var request = new StudentGroupCreateRequest("K3", "Groupe K3");
    when(studentGroupRepository.findByReference("K3")).thenReturn(Optional.empty());
    when(studentGroupRepository.save(any())).thenReturn(group("K3"));

    var result = service.create(request);

    assertThat(result.getReference()).isEqualTo("K3");
  }

  @Test
  void rejects_creation_when_reference_already_exists() {
    var request = new StudentGroupCreateRequest("K1", "Groupe K1");
    when(studentGroupRepository.findByReference("K1")).thenReturn(Optional.of(group("K1")));

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("already exists");
  }
}
