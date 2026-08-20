package api.poja.app.integration;

import static org.assertj.core.api.Assertions.assertThat;

import api.poja.app.endpoint.rest.dto.ErrorResponse;
import api.poja.app.endpoint.rest.dto.ExamCreateRequest;
import api.poja.app.model.Exam;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

class ExamControllerIT extends IntegrationTestSupport {

  private UUID setUpCourseAssignment(String adminToken) {
    var fixture = createLinkedPromotionAndGroup(adminToken);
    var course = createCourse(adminToken);
    var assignment =
        createCourseAssignment(
            adminToken, fixture.group().getId(), course.getId(), fixture.academicYear().getId(), 1);
    return assignment.getId();
  }

  @Test
  void create_exam_succeeds_when_coefficient_within_bounds() {
    var admin = bootstrapAdminToken();
    var courseAssignmentId = setUpCourseAssignment(admin);

    var request =
        new ExamCreateRequest(
            "Contrôle continu", LocalDate.now(), LocalTime.of(8, 0), new BigDecimal("0.4"));

    var response =
        post(
            API + "/course-assignments/" + courseAssignmentId + "/exams",
            admin,
            request,
            Exam.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().getCourseAssignmentId()).isEqualTo(courseAssignmentId);
    assertThat(response.getBody().getCoefficient()).isEqualByComparingTo("0.4");
  }

  @Test
  void create_exam_exceeding_total_coefficient_is_rejected() {
    var admin = bootstrapAdminToken();
    var courseAssignmentId = setUpCourseAssignment(admin);

    post(
        API + "/course-assignments/" + courseAssignmentId + "/exams",
        admin,
        new ExamCreateRequest("CC", LocalDate.now(), LocalTime.of(8, 0), new BigDecimal("0.6")),
        Exam.class);

    var response =
        post(
            API + "/course-assignments/" + courseAssignmentId + "/exams",
            admin,
            new ExamCreateRequest(
                "Final", LocalDate.now(), LocalTime.of(8, 0), new BigDecimal("0.5")),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void create_exam_on_unknown_course_assignment_returns_not_found() {
    var admin = bootstrapAdminToken();

    var response =
        post(
            API + "/course-assignments/" + UUID.randomUUID() + "/exams",
            admin,
            new ExamCreateRequest("CC", LocalDate.now(), LocalTime.of(8, 0), new BigDecimal("0.4")),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void non_admin_cannot_create_exam() {
    var admin = bootstrapAdminToken();
    var courseAssignmentId = setUpCourseAssignment(admin);
    var teacherUser = register("nonadminexam");
    promoteToTeacher(admin, teacherUser.getId());
    var teacherToken = login(teacherUser.getUsername(), DEFAULT_PASSWORD);

    var response =
        post(
            API + "/course-assignments/" + courseAssignmentId + "/exams",
            teacherToken,
            new ExamCreateRequest("CC", LocalDate.now(), LocalTime.of(8, 0), new BigDecimal("0.4")),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void list_exams_returns_created_exams() {
    var admin = bootstrapAdminToken();
    var courseAssignmentId = setUpCourseAssignment(admin);

    post(
        API + "/course-assignments/" + courseAssignmentId + "/exams",
        admin,
        new ExamCreateRequest("CC", LocalDate.now(), LocalTime.of(8, 0), new BigDecimal("0.4")),
        Exam.class);
    post(
        API + "/course-assignments/" + courseAssignmentId + "/exams",
        admin,
        new ExamCreateRequest("Final", LocalDate.now(), LocalTime.of(9, 0), new BigDecimal("0.6")),
        Exam.class);

    var response =
        restTemplate.exchange(
            API + "/course-assignments/" + courseAssignmentId + "/exams",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(admin)),
            new ParameterizedTypeReference<List<Exam>>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(2);
  }
}
