package api.poja.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import api.poja.app.endpoint.rest.dto.CourseCreateRequest;
import api.poja.app.entity.Course;
import api.poja.app.repository.CourseRepository;
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
class CourseServiceTest {

  @Mock private CourseRepository courseRepository;

  private CourseService service;

  @BeforeEach
  void setUp() {
    service = new CourseService(courseRepository);
  }

  private Course course(String reference) {
    var entity = new Course();
    entity.setId(UUID.randomUUID());
    entity.setReference(reference);
    entity.setTitle("Programmation " + reference);
    entity.setCredits(5);
    return entity;
  }

  @Test
  void lists_courses_from_paginated_repository() {
    var page = new PageImpl<>(List.of(course("PROG1"), course("WEB1")));
    when(courseRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);

    var result = service.list(0, 10);

    assertThat(result).extracting("reference").containsExactly("PROG1", "WEB1");
  }

  @Test
  void creates_course_when_reference_is_unique() {
    var request = new CourseCreateRequest("PROG4", "Programmation 4", 6);
    when(courseRepository.findByReference("PROG4")).thenReturn(Optional.empty());
    when(courseRepository.save(any())).thenReturn(course("PROG4"));

    var result = service.create(request);

    assertThat(result.getReference()).isEqualTo("PROG4");
  }

  @Test
  void rejects_creation_when_reference_already_exists() {
    var request = new CourseCreateRequest("PROG1", "Programmation 1", 5);
    when(courseRepository.findByReference("PROG1")).thenReturn(Optional.of(course("PROG1")));

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("already exists");
  }
}
