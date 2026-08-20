package api.poja.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.app.entity.AcademicYear;
import api.poja.app.entity.Student;
import api.poja.app.entity.StudentGroup;
import api.poja.app.entity.StudentGroupHistory;
import api.poja.app.repository.AcademicYearRepository;
import api.poja.app.repository.StudentGroupHistoryRepository;
import api.poja.app.repository.StudentGroupPromotionRepository;
import api.poja.app.repository.StudentGroupRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class StudentGroupAssignmentServiceTest {

  @Mock private StudentGroupRepository studentGroupRepository;
  @Mock private StudentGroupPromotionRepository studentGroupPromotionRepository;
  @Mock private StudentGroupHistoryRepository studentGroupHistoryRepository;
  @Mock private AcademicYearRepository academicYearRepository;

  private StudentGroupAssignmentService service;

  private UUID groupId;
  private UUID promotionId;
  private UUID academicYearId;
  private Student student;

  @BeforeEach
  void setUp() {
    service =
        new StudentGroupAssignmentService(
            studentGroupRepository,
            studentGroupPromotionRepository,
            studentGroupHistoryRepository,
            academicYearRepository);
    groupId = UUID.randomUUID();
    promotionId = UUID.randomUUID();
    academicYearId = UUID.randomUUID();
    student = new Student();
    student.setId(UUID.randomUUID());
  }

  private StudentGroup group() {
    var g = new StudentGroup();
    g.setId(groupId);
    g.setReference("K1");
    return g;
  }

  private AcademicYear academicYear() {
    var y = new AcademicYear();
    y.setId(academicYearId);
    y.setLabel("2025-2026");
    return y;
  }

  @Test
  void finds_group_when_it_belongs_to_promotion() {
    when(studentGroupRepository.findById(groupId)).thenReturn(Optional.of(group()));
    when(studentGroupPromotionRepository.existsByGroupIdAndPromotionId(groupId, promotionId))
        .thenReturn(true);

    var result = service.findGroupBelongingToPromotionOrThrow(groupId, promotionId);

    assertThat(result.getId()).isEqualTo(groupId);
  }

  @Test
  void rejects_when_group_not_found() {
    when(studentGroupRepository.findById(groupId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findGroupBelongingToPromotionOrThrow(groupId, promotionId))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Group not found");
  }

  @Test
  void rejects_when_group_does_not_belong_to_promotion() {
    when(studentGroupRepository.findById(groupId)).thenReturn(Optional.of(group()));
    when(studentGroupPromotionRepository.existsByGroupIdAndPromotionId(groupId, promotionId))
        .thenReturn(false);

    assertThatThrownBy(() -> service.findGroupBelongingToPromotionOrThrow(groupId, promotionId))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("does not belong to promotion");
  }

  @Test
  void creates_initial_history_entry_with_no_end_date() {
    when(academicYearRepository.findById(academicYearId)).thenReturn(Optional.of(academicYear()));
    when(studentGroupHistoryRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var startDate = LocalDate.of(2025, 9, 1);
    var result = service.createInitialHistory(student, group(), academicYearId, 1, startDate);

    assertThat(result.getStudent()).isEqualTo(student);
    assertThat(result.getGroup().getId()).isEqualTo(groupId);
    assertThat(result.getStartDate()).isEqualTo(startDate);
    assertThat(result.getEndDate()).isNull();
  }

  @Test
  void rejects_history_creation_when_academic_year_not_found() {
    when(academicYearRepository.findById(academicYearId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.createInitialHistory(
                    student, group(), academicYearId, 1, LocalDate.of(2025, 9, 1)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Academic year not found");
  }

  @Test
  void appending_history_closes_previous_open_entry_and_creates_new_one() {
    var previousEntry = new StudentGroupHistory();
    previousEntry.setId(UUID.randomUUID());
    previousEntry.setStudent(student);
    previousEntry.setStartDate(LocalDate.of(2024, 9, 1));
    previousEntry.setEndDate(null);

    when(studentGroupHistoryRepository.findByStudentIdOrderByStartDateAsc(student.getId()))
        .thenReturn(List.of(previousEntry));
    when(academicYearRepository.findById(academicYearId)).thenReturn(Optional.of(academicYear()));
    when(studentGroupHistoryRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var newStartDate = LocalDate.of(2025, 9, 1);
    var result = service.appendHistoryEntry(student, group(), academicYearId, 1, newStartDate);

    assertThat(previousEntry.getEndDate()).isEqualTo(newStartDate);
    assertThat(result.getStartDate()).isEqualTo(newStartDate);
    assertThat(result.getEndDate()).isNull();

    var captor = ArgumentCaptor.forClass(StudentGroupHistory.class);
    verify(studentGroupHistoryRepository, times(2)).save(captor.capture());
    assertThat(captor.getAllValues().get(0)).isEqualTo(previousEntry);
  }

  @Test
  void appending_history_does_not_fail_when_no_previous_open_entry_exists() {
    when(studentGroupHistoryRepository.findByStudentIdOrderByStartDateAsc(student.getId()))
        .thenReturn(List.of());
    when(academicYearRepository.findById(academicYearId)).thenReturn(Optional.of(academicYear()));
    when(studentGroupHistoryRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result =
        service.appendHistoryEntry(student, group(), academicYearId, 1, LocalDate.of(2025, 9, 1));

    assertThat(result).isNotNull();
    verify(studentGroupHistoryRepository, times(1)).save(any());
  }

  @Test
  void appending_history_only_closes_entries_still_open() {
    var closedEntry = new StudentGroupHistory();
    closedEntry.setId(UUID.randomUUID());
    closedEntry.setStudent(student);
    closedEntry.setStartDate(LocalDate.of(2023, 9, 1));
    closedEntry.setEndDate(LocalDate.of(2024, 6, 30));

    when(studentGroupHistoryRepository.findByStudentIdOrderByStartDateAsc(student.getId()))
        .thenReturn(List.of(closedEntry));
    when(academicYearRepository.findById(academicYearId)).thenReturn(Optional.of(academicYear()));
    when(studentGroupHistoryRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.appendHistoryEntry(student, group(), academicYearId, 1, LocalDate.of(2025, 9, 1));

    assertThat(closedEntry.getEndDate()).isEqualTo(LocalDate.of(2024, 6, 30));
    verify(studentGroupHistoryRepository, never()).save(closedEntry);
  }
}
