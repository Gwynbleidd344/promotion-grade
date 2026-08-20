package api.poja.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import api.poja.app.endpoint.rest.dto.ExamCreateRequest;
import api.poja.app.entity.Exam;
import api.poja.app.entity.GroupCourse;
import api.poja.app.repository.ExamRepository;
import api.poja.app.repository.GroupCourseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

  @Mock private ExamRepository examRepository;
  @Mock private GroupCourseRepository groupCourseRepository;

  private ExamService service;

  private UUID groupCourseId;

  @BeforeEach
  void setUp() {
    service = new ExamService(examRepository, groupCourseRepository);
    groupCourseId = UUID.randomUUID();
  }

  private GroupCourse groupCourse() {
    var gc = new GroupCourse();
    gc.setId(groupCourseId);
    return gc;
  }

  private Exam exam(BigDecimal coefficient) {
    var e = new Exam();
    e.setId(UUID.randomUUID());
    e.setGroupCourse(groupCourse());
    e.setName("Examen");
    e.setCoefficient(coefficient);
    return e;
  }

  @Test
  void lists_exams_for_a_course_assignment() {
    when(groupCourseRepository.findById(groupCourseId)).thenReturn(Optional.of(groupCourse()));
    when(examRepository.findByGroupCourseId(groupCourseId))
        .thenReturn(List.of(exam(new BigDecimal("0.5"))));

    var result = service.list(groupCourseId);

    assertThat(result).hasSize(1);
  }

  @Test
  void rejects_listing_when_course_assignment_not_found() {
    when(groupCourseRepository.findById(groupCourseId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.list(groupCourseId))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Course assignment not found");
  }

  @Test
  void creates_exam_when_coefficient_sum_stays_at_or_below_one() {
    var request =
        new ExamCreateRequest(
            "Partiel", LocalDate.of(2026, 1, 15), LocalTime.of(9, 0), new BigDecimal("0.5"));
    when(groupCourseRepository.findById(groupCourseId)).thenReturn(Optional.of(groupCourse()));
    when(examRepository.findByGroupCourseId(groupCourseId))
        .thenReturn(List.of(exam(new BigDecimal("0.5"))));
    when(examRepository.save(any())).thenReturn(exam(new BigDecimal("0.5")));

    var result = service.create(groupCourseId, request);

    assertThat(result).isNotNull();
  }

  @Test
  void rejects_creation_when_coefficient_sum_would_exceed_one() {
    var request =
        new ExamCreateRequest(
            "Rattrapage", LocalDate.of(2026, 1, 15), LocalTime.of(9, 0), new BigDecimal("0.6"));
    when(groupCourseRepository.findById(groupCourseId)).thenReturn(Optional.of(groupCourse()));
    when(examRepository.findByGroupCourseId(groupCourseId))
        .thenReturn(List.of(exam(new BigDecimal("0.5"))));

    assertThatThrownBy(() -> service.create(groupCourseId, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("exceed 1");
  }

  @Test
  void rejects_creation_when_course_assignment_not_found() {
    var request =
        new ExamCreateRequest(
            "Partiel", LocalDate.of(2026, 1, 15), LocalTime.of(9, 0), new BigDecimal("0.5"));
    when(groupCourseRepository.findById(groupCourseId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create(groupCourseId, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Course assignment not found");
  }
}
