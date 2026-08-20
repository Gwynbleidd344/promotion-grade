package api.poja.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.app.endpoint.rest.dto.GradeCreateRequest;
import api.poja.app.endpoint.rest.dto.GradeUpdateRequest;
import api.poja.app.entity.CourseTeacher;
import api.poja.app.entity.Exam;
import api.poja.app.entity.Grade;
import api.poja.app.entity.GroupCourse;
import api.poja.app.entity.Student;
import api.poja.app.entity.Teacher;
import api.poja.app.entity.UserAccount;
import api.poja.app.entity.enums.UserRole;
import api.poja.app.repository.ExamRepository;
import api.poja.app.repository.GradeHistoryRepository;
import api.poja.app.repository.GradeRepository;
import api.poja.app.repository.StudentRepository;
import api.poja.app.repository.TeacherRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

  @Mock private GradeRepository gradeRepository;
  @Mock private GradeHistoryRepository gradeHistoryRepository;
  @Mock private ExamRepository examRepository;
  @Mock private StudentRepository studentRepository;
  @Mock private TeacherRepository teacherRepository;

  private GradeService service;

  private UUID examId;
  private UUID studentId;
  private UUID groupCourseId;
  private UUID teacherId;

  @BeforeEach
  void setUp() {
    service =
        new GradeService(
            gradeRepository,
            gradeHistoryRepository,
            examRepository,
            studentRepository,
            teacherRepository);
    examId = UUID.randomUUID();
    studentId = UUID.randomUUID();
    groupCourseId = UUID.randomUUID();
    teacherId = UUID.randomUUID();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void loginAs(UserAccount account) {
    var authentication = new UsernamePasswordAuthenticationToken(account, null, List.of());
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
  }

  private UserAccount account(UserRole role) {
    var a = new UserAccount();
    a.setId(UUID.randomUUID());
    a.setUsername("user");
    a.setRole(role);
    return a;
  }

  private GroupCourse groupCourse() {
    var gc = new GroupCourse();
    gc.setId(groupCourseId);
    return gc;
  }

  private Exam exam() {
    var e = new Exam();
    e.setId(examId);
    e.setGroupCourse(groupCourse());
    e.setCoefficient(new BigDecimal("1"));
    return e;
  }

  private Student student(UserAccount owner) {
    var s = new Student();
    s.setId(studentId);
    s.setUserAccount(owner);
    return s;
  }

  @Test
  void admin_can_create_a_grade_for_any_course() {
    var admin = account(UserRole.ADM);
    loginAs(admin);
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam()));
    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student(admin)));
    when(gradeRepository.findByStudentIdAndExamId(studentId, examId)).thenReturn(Optional.empty());
    var saved = new Grade();
    saved.setId(UUID.randomUUID());
    saved.setStudent(student(admin));
    saved.setExam(exam());
    saved.setValue(new BigDecimal("15"));
    when(gradeRepository.save(any())).thenReturn(saved);

    var request = new GradeCreateRequest(studentId, new BigDecimal("15"));
    var result = service.create(examId, request);

    assertThat(result.getValue()).isEqualByComparingTo("15");
  }

  @Test
  void teacher_can_create_a_grade_for_a_course_they_teach() {
    var teacherAccount = account(UserRole.TEC);
    loginAs(teacherAccount);

    var teacher = new Teacher();
    teacher.setId(teacherId);
    teacher.setUserAccount(teacherAccount);
    var assignment = new CourseTeacher();
    assignment.setGroupCourse(groupCourse());
    teacher.setCourseAssignments(List.of(assignment));

    when(examRepository.findById(examId)).thenReturn(Optional.of(exam()));
    when(teacherRepository.findByUserAccountId(teacherAccount.getId()))
        .thenReturn(Optional.of(teacher));
    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student(teacherAccount)));
    when(gradeRepository.findByStudentIdAndExamId(studentId, examId)).thenReturn(Optional.empty());
    var saved = new Grade();
    saved.setId(UUID.randomUUID());
    saved.setStudent(student(teacherAccount));
    saved.setExam(exam());
    saved.setValue(new BigDecimal("12"));
    when(gradeRepository.save(any())).thenReturn(saved);

    var request = new GradeCreateRequest(studentId, new BigDecimal("12"));
    var result = service.create(examId, request);

    assertThat(result.getValue()).isEqualByComparingTo("12");
  }

  @Test
  void teacher_cannot_grade_a_course_they_do_not_teach() {
    var teacherAccount = account(UserRole.TEC);
    loginAs(teacherAccount);

    var teacher = new Teacher();
    teacher.setId(teacherId);
    teacher.setUserAccount(teacherAccount);
    teacher.setCourseAssignments(List.of());

    when(examRepository.findById(examId)).thenReturn(Optional.of(exam()));
    when(teacherRepository.findByUserAccountId(teacherAccount.getId()))
        .thenReturn(Optional.of(teacher));

    var request = new GradeCreateRequest(studentId, new BigDecimal("12"));

    assertThatThrownBy(() -> service.create(examId, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("does not teach this course");
  }

  @Test
  void rejects_creation_when_exam_not_found() {
    loginAs(account(UserRole.ADM));
    when(examRepository.findById(examId)).thenReturn(Optional.empty());

    var request = new GradeCreateRequest(studentId, new BigDecimal("12"));

    assertThatThrownBy(() -> service.create(examId, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Exam not found");
  }

  @Test
  void rejects_creation_when_grade_already_exists_for_student_and_exam() {
    var admin = account(UserRole.ADM);
    loginAs(admin);
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam()));
    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student(admin)));
    when(gradeRepository.findByStudentIdAndExamId(studentId, examId))
        .thenReturn(Optional.of(new Grade()));

    var request = new GradeCreateRequest(studentId, new BigDecimal("12"));

    assertThatThrownBy(() -> service.create(examId, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void student_can_view_their_own_grades() {
    var studentAccount = account(UserRole.STD);
    loginAs(studentAccount);
    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student(studentAccount)));
    when(gradeRepository.findByStudentIdFiltered(studentId, null, null)).thenReturn(List.of());

    var result = service.listForStudent(studentId, null, null);

    assertThat(result).isEmpty();
  }

  @Test
  void student_cannot_view_another_students_grades() {
    var studentAccount = account(UserRole.STD);
    loginAs(studentAccount);
    var otherOwner = account(UserRole.STD);
    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student(otherOwner)));

    assertThatThrownBy(() -> service.listForStudent(studentId, null, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Not allowed");
  }

  @Test
  void staff_can_view_any_students_grades() {
    var adminAccount = account(UserRole.ADM);
    loginAs(adminAccount);
    var owner = account(UserRole.STD);
    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student(owner)));
    when(gradeRepository.findByStudentIdFiltered(studentId, null, null)).thenReturn(List.of());

    assertThat(service.listForStudent(studentId, null, null)).isEmpty();
  }

  @Test
  void updating_a_grade_records_history_with_old_and_new_values() {
    var admin = account(UserRole.ADM);
    loginAs(admin);

    var gradeId = UUID.randomUUID();
    var entity = new Grade();
    entity.setId(gradeId);
    entity.setExam(exam());
    entity.setStudent(student(admin));
    entity.setValue(new BigDecimal("8"));

    when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(entity));
    when(gradeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var request = new GradeUpdateRequest(new BigDecimal("11"), "Erreur de saisie corrigée");
    var result = service.update(gradeId, request);

    assertThat(result.getValue()).isEqualByComparingTo("11");

    var captor = ArgumentCaptor.forClass(api.poja.app.entity.GradeHistory.class);
    verify(gradeHistoryRepository).save(captor.capture());
    var history = captor.getValue();
    assertThat(history.getOldValue()).isEqualByComparingTo("8");
    assertThat(history.getNewValue()).isEqualByComparingTo("11");
    assertThat(history.getChangedBy()).isEqualTo(admin);
    assertThat(history.getReason()).isEqualTo("Erreur de saisie corrigée");
  }

  @Test
  void teacher_cannot_update_a_grade_for_a_course_they_do_not_teach() {
    var teacherAccount = account(UserRole.TEC);
    loginAs(teacherAccount);

    var teacher = new Teacher();
    teacher.setId(teacherId);
    teacher.setUserAccount(teacherAccount);
    teacher.setCourseAssignments(List.of());

    var gradeId = UUID.randomUUID();
    var entity = new Grade();
    entity.setId(gradeId);
    entity.setExam(exam());
    entity.setStudent(student(teacherAccount));
    entity.setValue(new BigDecimal("8"));

    when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(entity));
    when(teacherRepository.findByUserAccountId(teacherAccount.getId()))
        .thenReturn(Optional.of(teacher));

    var request = new GradeUpdateRequest(new BigDecimal("11"), "reason");

    assertThatThrownBy(() -> service.update(gradeId, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("does not teach this course");
  }

  @Test
  void owner_can_view_grade_history() {
    var studentAccount = account(UserRole.STD);
    loginAs(studentAccount);

    var gradeId = UUID.randomUUID();
    var entity = new Grade();
    entity.setId(gradeId);
    entity.setStudent(student(studentAccount));

    when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(entity));
    when(gradeHistoryRepository.findByGradeIdOrderByChangedAtDesc(gradeId)).thenReturn(List.of());

    assertThat(service.history(gradeId)).isEmpty();
  }
}
