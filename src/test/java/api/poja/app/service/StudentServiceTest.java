package api.poja.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.app.endpoint.rest.dto.StudentGroupChangeRequest;
import api.poja.app.endpoint.rest.dto.StudentPromotionAndGroupChangeRequest;
import api.poja.app.entity.Program;
import api.poja.app.entity.Promotion;
import api.poja.app.entity.Student;
import api.poja.app.entity.StudentGroup;
import api.poja.app.entity.StudentGroupHistory;
import api.poja.app.entity.UserAccount;
import api.poja.app.entity.enums.ProgramCode;
import api.poja.app.repository.PromotionRepository;
import api.poja.app.repository.StudentGroupHistoryRepository;
import api.poja.app.repository.StudentRepository;
import java.time.LocalDate;
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
class StudentServiceTest {

  @Mock private StudentRepository studentRepository;
  @Mock private PromotionRepository promotionRepository;
  @Mock private StudentGroupAssignmentService studentGroupAssignmentService;
  @Mock private StudentGroupHistoryRepository studentGroupHistoryRepository;

  private StudentService service;

  private UUID studentId;
  private UUID promotionId;
  private UUID groupId;
  private UUID academicYearId;

  @BeforeEach
  void setUp() {
    service =
        new StudentService(
            studentRepository,
            promotionRepository,
            studentGroupAssignmentService,
            studentGroupHistoryRepository);
    studentId = UUID.randomUUID();
    promotionId = UUID.randomUUID();
    groupId = UUID.randomUUID();
    academicYearId = UUID.randomUUID();
  }

  private Student student() {
    var s = new Student();
    s.setId(studentId);
    s.setStudentNumber("STD001");
    s.setFirstName("Jean");
    s.setLastName("Rakoto");

    var program = new Program();
    program.setId(UUID.randomUUID());
    program.setCode(ProgramCode.EL);
    s.setProgram(program);

    var promotion = new Promotion();
    promotion.setId(promotionId);
    s.setPromotion(promotion);

    var userAccount = new UserAccount();
    userAccount.setId(UUID.randomUUID());
    s.setUserAccount(userAccount);
    return s;
  }

  @Test
  void lists_students_filtered_and_paginated() {
    var page = new PageImpl<>(List.of(student()));
    when(studentRepository.findAllFiltered(
            eq(promotionId), eq(ProgramCode.EL), eq(PageRequest.of(0, 10))))
        .thenReturn(page);

    var result = service.list(promotionId, ProgramCode.EL, 0, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStudentNumber()).isEqualTo("STD001");
  }

  @Test
  void gets_student_by_id() {
    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student()));

    var result = service.getById(studentId);

    assertThat(result.getStudentNumber()).isEqualTo("STD001");
  }

  @Test
  void rejects_get_by_id_when_student_not_found() {
    when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getById(studentId))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Student not found");
  }

  @Test
  void group_history_is_sorted_by_start_date_ascending() {
    var group = new StudentGroup();
    group.setId(groupId);

    var academicYear = new api.poja.app.entity.AcademicYear();
    academicYear.setId(academicYearId);

    var later = new StudentGroupHistory();
    later.setId(UUID.randomUUID());
    later.setStudent(student());
    later.setGroup(group);
    later.setAcademicYear(academicYear);
    later.setStartDate(LocalDate.of(2025, 9, 1));

    var earlier = new StudentGroupHistory();
    earlier.setId(UUID.randomUUID());
    earlier.setStudent(student());
    earlier.setGroup(group);
    earlier.setAcademicYear(academicYear);
    earlier.setStartDate(LocalDate.of(2024, 9, 1));

    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student()));
    when(studentGroupHistoryRepository.findByStudentIdOrderByStartDateAsc(studentId))
        .thenReturn(List.of(later, earlier));

    var result = service.getGroupHistory(studentId, null);

    assertThat(result)
        .extracting("startDate")
        .containsExactly(LocalDate.of(2024, 9, 1), LocalDate.of(2025, 9, 1));
  }

  @Test
  void group_history_filters_by_academic_year_when_provided() {
    var group = new StudentGroup();
    group.setId(groupId);
    var academicYear = new api.poja.app.entity.AcademicYear();
    academicYear.setId(academicYearId);

    var entry = new StudentGroupHistory();
    entry.setId(UUID.randomUUID());
    entry.setStudent(student());
    entry.setGroup(group);
    entry.setAcademicYear(academicYear);
    entry.setStartDate(LocalDate.of(2025, 9, 1));

    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student()));
    when(studentGroupHistoryRepository.findByStudentIdAndAcademicYearId(studentId, academicYearId))
        .thenReturn(List.of(entry));

    var result = service.getGroupHistory(studentId, academicYearId);

    assertThat(result).hasSize(1);
  }

  @Test
  void changes_promotion_and_group_and_appends_history() {
    var newPromotionId = UUID.randomUUID();
    var newPromotion = new Promotion();
    newPromotion.setId(newPromotionId);

    var request =
        new StudentPromotionAndGroupChangeRequest(
            newPromotionId, groupId, academicYearId, 1, LocalDate.of(2025, 9, 1));

    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student()));
    when(promotionRepository.findById(newPromotionId)).thenReturn(Optional.of(newPromotion));
    var group = new StudentGroup();
    group.setId(groupId);
    when(studentGroupAssignmentService.findGroupBelongingToPromotionOrThrow(
            groupId, newPromotionId))
        .thenReturn(group);

    var result = service.changePromotionAndGroup(studentId, request);

    assertThat(result.getPromotionId()).isEqualTo(newPromotionId);
    verify(studentRepository).save(any());
    verify(studentGroupAssignmentService)
        .appendHistoryEntry(any(), eq(group), eq(academicYearId), eq(1), eq(request.startDate()));
  }

  @Test
  void rejects_promotion_change_when_promotion_unknown() {
    var request =
        new StudentPromotionAndGroupChangeRequest(
            promotionId, groupId, academicYearId, 1, LocalDate.of(2025, 9, 1));
    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student()));
    when(promotionRepository.findById(promotionId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.changePromotionAndGroup(studentId, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Unknown promotion");
  }

  @Test
  void changes_group_within_the_current_promotion() {
    var request =
        new StudentGroupChangeRequest(groupId, academicYearId, 1, LocalDate.of(2025, 9, 1));
    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student()));
    var group = new StudentGroup();
    group.setId(groupId);
    when(studentGroupAssignmentService.findGroupBelongingToPromotionOrThrow(groupId, promotionId))
        .thenReturn(group);

    var result = service.changeGroup(studentId, request);

    assertThat(result.getStudentNumber()).isEqualTo("STD001");
    verify(studentGroupAssignmentService)
        .appendHistoryEntry(any(), eq(group), eq(academicYearId), eq(1), eq(request.startDate()));
  }
}
