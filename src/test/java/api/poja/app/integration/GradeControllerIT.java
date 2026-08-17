package api.poja.app.integration;

import static org.assertj.core.api.Assertions.assertThat;

import api.poja.app.endpoint.rest.dto.ErrorResponse;
import api.poja.app.endpoint.rest.dto.ExamCreateRequest;
import api.poja.app.endpoint.rest.dto.GradeCreateRequest;
import api.poja.app.model.CourseTeacher;
import api.poja.app.model.Exam;
import api.poja.app.model.Grade;
import api.poja.app.repository.TeacherRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

class GradeControllerIT extends IntegrationTestSupport {

  @Autowired private TeacherRepository teacherRepository;

  private record GradeFixture(
      UUID courseAssignmentId,
      UUID examId,
      UUID courseId,
      UUID academicYearId,
      String teacherToken,
      String studentToken,
      UUID studentId) {}

  private GradeFixture setUpGradeFixture(String admin) {
    var promoGroup = createLinkedPromotionAndGroup(admin);
    var course = createCourse(admin);
    var assignment =
        createCourseAssignment(
            admin,
            promoGroup.group().getId(),
            course.getId(),
            promoGroup.academicYear().getId(),
            1);

    var exam =
        post(
                API + "/course-assignments/" + assignment.getId() + "/exams",
                admin,
                new ExamCreateRequest(
                    "CC", LocalDate.now(), LocalTime.of(8, 0), new BigDecimal("1.0")),
                Exam.class)
            .getBody();

    var teacherUser = register("gradeteacher");
    var teacherAccount = promoteToTeacher(admin, teacherUser.getId());
    var teacher = teacherRepository.findByUserAccountId(teacherAccount.getId()).orElseThrow();
    post(
        API + "/course-assignments/" + assignment.getId() + "/teachers/" + teacher.getId(),
        admin,
        null,
        CourseTeacher.class);
    var teacherToken = login(teacherUser.getUsername(), DEFAULT_PASSWORD);

    var studentUser = register("gradestudent");
    var student =
        promoteToStudent(
            admin,
            studentUser.getId(),
            promoGroup.promotion(),
            promoGroup.group(),
            promoGroup.academicYear());
    var studentToken = login(studentUser.getUsername(), DEFAULT_PASSWORD);

    return new GradeFixture(
        assignment.getId(),
        exam.getId(),
        course.getId(),
        promoGroup.academicYear().getId(),
        teacherToken,
        studentToken,
        student.getId());
  }

  @Test
  void teacher_enters_grade_for_own_course_succeeds() {
    var admin = bootstrapAdminToken();
    var fixture = setUpGradeFixture(admin);

    var response =
        post(
            API + "/exams/" + fixture.examId() + "/grades",
            fixture.teacherToken(),
            new GradeCreateRequest(fixture.studentId(), new BigDecimal("15.5")),
            Grade.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().getStudentId()).isEqualTo(fixture.studentId());
    assertThat(response.getBody().getExamId()).isEqualTo(fixture.examId());
    assertThat(response.getBody().getValue()).isEqualByComparingTo("15.5");
  }

  @Test
  void entering_duplicate_grade_conflicts() {
    var admin = bootstrapAdminToken();
    var fixture = setUpGradeFixture(admin);
    post(
        API + "/exams/" + fixture.examId() + "/grades",
        fixture.teacherToken(),
        new GradeCreateRequest(fixture.studentId(), new BigDecimal("10")),
        Grade.class);

    var response =
        post(
            API + "/exams/" + fixture.examId() + "/grades",
            fixture.teacherToken(),
            new GradeCreateRequest(fixture.studentId(), new BigDecimal("12")),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void teacher_not_assigned_to_course_cannot_enter_grade() {
    var admin = bootstrapAdminToken();
    var fixture = setUpGradeFixture(admin);
    var outsiderTeacherUser = register("outsiderteacher");
    promoteToTeacher(admin, outsiderTeacherUser.getId());
    var outsiderToken = login(outsiderTeacherUser.getUsername(), DEFAULT_PASSWORD);

    var response =
        post(
            API + "/exams/" + fixture.examId() + "/grades",
            outsiderToken,
            new GradeCreateRequest(fixture.studentId(), new BigDecimal("10")),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void entering_grade_for_unknown_exam_returns_not_found() {
    var admin = bootstrapAdminToken();
    var fixture = setUpGradeFixture(admin);

    var response =
        post(
            API + "/exams/" + UUID.randomUUID() + "/grades",
            fixture.teacherToken(),
            new GradeCreateRequest(fixture.studentId(), new BigDecimal("10")),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void entering_grade_for_unknown_student_returns_not_found() {
    var admin = bootstrapAdminToken();
    var fixture = setUpGradeFixture(admin);

    var response =
        post(
            API + "/exams/" + fixture.examId() + "/grades",
            fixture.teacherToken(),
            new GradeCreateRequest(UUID.randomUUID(), new BigDecimal("10")),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }
}
