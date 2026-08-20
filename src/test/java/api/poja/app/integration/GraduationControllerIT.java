package api.poja.app.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.app.endpoint.rest.dto.ErrorResponse;
import api.poja.app.entity.Exam;
import api.poja.app.entity.Grade;
import api.poja.app.entity.ProgramCourse;
import api.poja.app.entity.enums.ProgramCode;
import api.poja.app.file.bucket.BucketComponent;
import api.poja.app.model.Course;
import api.poja.app.model.GraduationListEntry;
import api.poja.app.model.GraduationListExport;
import api.poja.app.model.GraduationStatus;
import api.poja.app.repository.CourseRepository;
import api.poja.app.repository.ExamRepository;
import api.poja.app.repository.GradeRepository;
import api.poja.app.repository.GroupCourseRepository;
import api.poja.app.repository.ProgramCourseRepository;
import api.poja.app.repository.ProgramRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

class GraduationControllerIT extends IntegrationTestSupport {

  private static final BigDecimal PASSING_GRADE = new BigDecimal("15.00");

  @Autowired private CourseRepository courseRepository;
  @Autowired private GroupCourseRepository groupCourseRepository;
  @Autowired private ExamRepository examRepository;
  @Autowired private GradeRepository gradeRepository;
  @Autowired private ProgramRepository programRepository;
  @Autowired private ProgramCourseRepository programCourseRepository;

  @MockBean private BucketComponent bucketComponent;

  @BeforeEach
  void stubBucket() throws Exception {
    when(bucketComponent.presign(any(), any()))
        .thenReturn(new URI("https://dummy-bucket.s3.amazonaws.com/signed").toURL());
  }

  private record StudentFixture(UUID studentId, UUID promotionId) {}

  private Course addCourseToProgram(String adminToken) {
    var course = createCourse(adminToken);
    var program = programRepository.findByCode(ProgramCode.EL).orElseThrow();
    var courseEntity = courseRepository.findById(course.getId()).orElseThrow();
    var programCourse = new ProgramCourse();
    programCourse.setProgram(program);
    programCourse.setCourse(courseEntity);
    programCourseRepository.save(programCourse);
    return course;
  }

  private Exam ensureExamForCourse(
      String adminToken, BasePromotionFixture base, UUID courseId, Map<UUID, Exam> examCache) {
    var cached = examCache.get(courseId);
    if (cached != null) {
      return cached;
    }
    var assignment =
        createCourseAssignment(
            adminToken, base.group().getId(), courseId, base.academicYear().getId(), 1);
    var groupCourse = groupCourseRepository.findById(assignment.getId()).orElseThrow();
    var exam = new Exam();
    exam.setGroupCourse(groupCourse);
    exam.setName("Examen final");
    exam.setExamDate(LocalDate.now());
    exam.setExamTime(LocalTime.of(8, 0));
    exam.setCoefficient(new BigDecimal("1.0000"));
    exam = examRepository.save(exam);
    examCache.put(courseId, exam);
    return exam;
  }

  private void gradeStudentOnCourse(UUID studentId, Exam exam, BigDecimal value) {
    var studentEntity = studentRepository.findById(studentId).orElseThrow();
    var grade = new Grade();
    grade.setStudent(studentEntity);
    grade.setExam(exam);
    grade.setValue(value);
    grade.setCreatedAt(LocalDateTime.now());
    grade.setUpdatedAt(LocalDateTime.now());
    gradeRepository.save(grade);
  }

  private void gradeStudentOnAllProgramCourses(
      String adminToken,
      BasePromotionFixture base,
      UUID studentId,
      Map<UUID, Exam> examCache,
      BigDecimal value) {
    var program = programRepository.findByCode(ProgramCode.EL).orElseThrow();
    var programCourses = programCourseRepository.findByProgramId(program.getId());
    for (var pc : programCourses) {
      var exam = ensureExamForCourse(adminToken, base, pc.getCourse().getId(), examCache);
      gradeStudentOnCourse(studentId, exam, value);
    }
  }

  private StudentFixture setUpGraduatedStudent(String adminToken, BigDecimal average) {
    var base = createLinkedPromotionAndGroup(adminToken);
    var user = register("student-" + UUID.randomUUID());
    var student =
        promoteToStudent(
            adminToken, user.getId(), base.promotion(), base.group(), base.academicYear());

    addCourseToProgram(adminToken);

    var examCache = new HashMap<UUID, Exam>();
    gradeStudentOnAllProgramCourses(adminToken, base, student.getId(), examCache, average);

    return new StudentFixture(student.getId(), base.promotion().getId());
  }

  private StudentFixture setUpNonGraduatedStudent(String adminToken) {
    var base = createLinkedPromotionAndGroup(adminToken);
    var user = register("student-" + UUID.randomUUID());
    var student =
        promoteToStudent(
            adminToken, user.getId(), base.promotion(), base.group(), base.academicYear());
    addCourseToProgram(adminToken);
    return new StudentFixture(student.getId(), base.promotion().getId());
  }

  private List<GraduationListEntry> getGraduates(String token, UUID promotionId) {
    var response =
        restTemplate.exchange(
            API + "/promotions/" + promotionId + "/graduates",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(token)),
            new ParameterizedTypeReference<List<GraduationListEntry>>() {});
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return response.getBody();
  }

  @Test
  void get_graduation_status_for_student_with_all_courses_passed_returns_graduated() {
    var admin = bootstrapAdminToken();
    var fixture = setUpGraduatedStudent(admin, PASSING_GRADE);

    var response =
        get(
            API + "/students/" + fixture.studentId() + "/graduation",
            admin,
            GraduationStatus.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    var status = response.getBody();
    assertThat(status.getStudentId()).isEqualTo(fixture.studentId());
    assertThat(status.isGraduated()).isTrue();
    assertThat(status.getGeneralAverage()).isEqualByComparingTo(PASSING_GRADE);
    assertThat(status.getTotalCreditsObtained()).isPositive();
    assertThat(status.getFailedCourses()).isEmpty();
  }

  @Test
  void get_graduation_status_for_student_with_ungraded_course_returns_not_graduated() {
    var admin = bootstrapAdminToken();
    var base = createLinkedPromotionAndGroup(admin);
    var user = register("student-" + UUID.randomUUID());
    var student =
        promoteToStudent(admin, user.getId(), base.promotion(), base.group(), base.academicYear());
    var examCache = new HashMap<UUID, Exam>();

    addCourseToProgram(admin);
    gradeStudentOnAllProgramCourses(admin, base, student.getId(), examCache, PASSING_GRADE);
    var ungradedCourse = addCourseToProgram(admin);

    var response =
        get(API + "/students/" + student.getId() + "/graduation", admin, GraduationStatus.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    var status = response.getBody();
    assertThat(status.isGraduated()).isFalse();
    assertThat(status.getFailedCourses()).hasSize(1);
    assertThat(status.getFailedCourses().getFirst().getCourseId())
        .isEqualTo(ungradedCourse.getId());
    assertThat(status.getFailedCourses().getFirst().getAverage()).isNull();
  }

  @Test
  void get_graduation_status_for_student_with_failing_grade_lists_failed_course() {
    var admin = bootstrapAdminToken();
    var base = createLinkedPromotionAndGroup(admin);
    var user = register("student-" + UUID.randomUUID());
    var student =
        promoteToStudent(admin, user.getId(), base.promotion(), base.group(), base.academicYear());
    var examCache = new HashMap<UUID, Exam>();

    addCourseToProgram(admin);
    gradeStudentOnAllProgramCourses(admin, base, student.getId(), examCache, PASSING_GRADE);

    var failingCourse = addCourseToProgram(admin);
    var failingGrade = new BigDecimal("8.00");
    var exam = ensureExamForCourse(admin, base, failingCourse.getId(), examCache);
    gradeStudentOnCourse(student.getId(), exam, failingGrade);

    var response =
        get(API + "/students/" + student.getId() + "/graduation", admin, GraduationStatus.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    var status = response.getBody();
    assertThat(status.isGraduated()).isFalse();
    assertThat(status.getFailedCourses()).hasSize(1);
    assertThat(status.getFailedCourses().getFirst().getCourseId()).isEqualTo(failingCourse.getId());
    assertThat(status.getFailedCourses().getFirst().getAverage())
        .isEqualByComparingTo(failingGrade);
  }

  @Test
  void get_graduation_status_for_unknown_student_returns_not_found() {
    var admin = bootstrapAdminToken();

    var response =
        get(API + "/students/" + UUID.randomUUID() + "/graduation", admin, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void get_graduation_status_without_token_returns_unauthorized() {
    var admin = bootstrapAdminToken();
    var fixture = setUpGraduatedStudent(admin, PASSING_GRADE);

    var response =
        get(API + "/students/" + fixture.studentId() + "/graduation", null, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void get_graduation_status_is_accessible_to_any_authenticated_user() {
    var admin = bootstrapAdminToken();
    var fixture = setUpGraduatedStudent(admin, PASSING_GRADE);
    var teacherUser = register("teacher-grad-" + UUID.randomUUID());
    promoteToTeacher(admin, teacherUser.getId());
    var teacherToken = login(teacherUser.getUsername(), DEFAULT_PASSWORD);

    var response =
        get(
            API + "/students/" + fixture.studentId() + "/graduation",
            teacherToken,
            GraduationStatus.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void get_graduates_returns_only_graduated_students_ranked_by_average_desc() {
    var admin = bootstrapAdminToken();
    var base = createLinkedPromotionAndGroup(admin);
    var examCache = new HashMap<UUID, Exam>();
    addCourseToProgram(admin);

    var topUser = register("top-" + UUID.randomUUID());
    var topStudent =
        promoteToStudent(
            admin, topUser.getId(), base.promotion(), base.group(), base.academicYear());
    gradeStudentOnAllProgramCourses(
        admin, base, topStudent.getId(), examCache, new BigDecimal("18.00"));

    var secondUser = register("second-" + UUID.randomUUID());
    var secondStudent =
        promoteToStudent(
            admin, secondUser.getId(), base.promotion(), base.group(), base.academicYear());
    gradeStudentOnAllProgramCourses(
        admin, base, secondStudent.getId(), examCache, new BigDecimal("12.00"));

    var failingUser = register("failing-" + UUID.randomUUID());
    promoteToStudent(
        admin, failingUser.getId(), base.promotion(), base.group(), base.academicYear());

    var entries = getGraduates(admin, base.promotion().getId());

    assertThat(entries).hasSize(2);
    assertThat(entries.get(0).getRank()).isEqualTo(1);
    assertThat(entries.get(0).getGeneralAverage()).isEqualByComparingTo("18.00");
    assertThat(entries.get(1).getRank()).isEqualTo(2);
    assertThat(entries.get(1).getGeneralAverage()).isEqualByComparingTo("12.00");
  }

  @Test
  void get_graduates_filters_by_program_code() {
    var admin = bootstrapAdminToken();
    var fixture = setUpGraduatedStudent(admin, PASSING_GRADE);

    var matching = getGraduates(admin, fixture.promotionId());
    assertThat(matching).hasSize(1);

    var response =
        restTemplate.exchange(
            API + "/promotions/" + fixture.promotionId() + "/graduates?programCode=TN",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(admin)),
            new ParameterizedTypeReference<List<GraduationListEntry>>() {});
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEmpty();
  }

  @Test
  void get_graduates_for_promotion_with_no_graduated_students_returns_empty_list() {
    var admin = bootstrapAdminToken();
    var fixture = setUpNonGraduatedStudent(admin);

    var entries = getGraduates(admin, fixture.promotionId());

    assertThat(entries).isEmpty();
  }

  @Test
  void get_graduates_for_unknown_promotion_returns_not_found() {
    var admin = bootstrapAdminToken();

    var response =
        get(API + "/promotions/" + UUID.randomUUID() + "/graduates", admin, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void non_admin_cannot_list_graduates() {
    var admin = bootstrapAdminToken();
    var fixture = setUpGraduatedStudent(admin, PASSING_GRADE);
    var teacherUser = register("teacher-list-" + UUID.randomUUID());
    promoteToTeacher(admin, teacherUser.getId());
    var teacherToken = login(teacherUser.getUsername(), DEFAULT_PASSWORD);

    var response =
        get(
            API + "/promotions/" + fixture.promotionId() + "/graduates",
            teacherToken,
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void list_graduates_without_token_returns_unauthorized() {
    var admin = bootstrapAdminToken();
    var fixture = setUpGraduatedStudent(admin, PASSING_GRADE);

    var response =
        get(API + "/promotions/" + fixture.promotionId() + "/graduates", null, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void export_graduates_uploads_workbook_and_returns_download_url() {
    var admin = bootstrapAdminToken();
    var fixture = setUpGraduatedStudent(admin, PASSING_GRADE);

    var response =
        get(
            API + "/promotions/" + fixture.promotionId() + "/graduates/export",
            admin,
            GraduationListExport.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    var export = response.getBody();
    assertThat(export.getPromotionId()).isEqualTo(fixture.promotionId());
    assertThat(export.getS3Key()).isNotBlank();
    assertThat(export.getDownloadUrl()).isNotBlank();
    assertThat(export.getGeneratedAt()).isNotNull();
    verify(bucketComponent, times(1)).upload(any(), any());
    verify(bucketComponent, times(1)).presign(any(), any());
  }

  @Test
  void export_graduates_for_unknown_promotion_returns_not_found() {
    var admin = bootstrapAdminToken();

    var response =
        get(
            API + "/promotions/" + UUID.randomUUID() + "/graduates/export",
            admin,
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void non_admin_cannot_export_graduates() {
    var admin = bootstrapAdminToken();
    var fixture = setUpGraduatedStudent(admin, PASSING_GRADE);
    var teacherUser = register("teacher-export-" + UUID.randomUUID());
    promoteToTeacher(admin, teacherUser.getId());
    var teacherToken = login(teacherUser.getUsername(), DEFAULT_PASSWORD);

    var response =
        get(
            API + "/promotions/" + fixture.promotionId() + "/graduates/export",
            teacherToken,
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void export_graduates_without_token_returns_unauthorized() {
    var admin = bootstrapAdminToken();
    var fixture = setUpGraduatedStudent(admin, PASSING_GRADE);

    var response =
        get(
            API + "/promotions/" + fixture.promotionId() + "/graduates/export",
            null,
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
