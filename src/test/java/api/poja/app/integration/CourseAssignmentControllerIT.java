package api.poja.app.integration;

import static org.assertj.core.api.Assertions.assertThat;

import api.poja.app.endpoint.rest.dto.CourseAssignmentCreateRequest;
import api.poja.app.endpoint.rest.dto.ErrorResponse;
import api.poja.app.model.CourseAssignment;
import api.poja.app.model.CourseTeacher;
import api.poja.app.repository.TeacherRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

class CourseAssignmentControllerIT extends IntegrationTestSupport {

  @Autowired private TeacherRepository teacherRepository;

  @Test
  void admin_creates_course_assignment_and_lists_it_with_filters() {
    var admin = bootstrapAdminToken();
    var fixture = createLinkedPromotionAndGroup(admin);
    var course = createCourse(admin);

    var created =
        createCourseAssignment(
            admin, fixture.group().getId(), course.getId(), fixture.academicYear().getId(), 1);

    assertThat(created.getId()).isNotNull();
    assertThat(created.getTeachers()).isEmpty();

    var response =
        restTemplate.exchange(
            API
                + "/course-assignments?groupId="
                + fixture.group().getId()
                + "&academicYearId="
                + fixture.academicYear().getId()
                + "&semester=1",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(admin)),
            new ParameterizedTypeReference<List<CourseAssignment>>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).extracting(CourseAssignment::getId).contains(created.getId());
  }

  @Test
  void create_conflicts_on_duplicate_combination() {
    var admin = bootstrapAdminToken();
    var fixture = createLinkedPromotionAndGroup(admin);
    var course = createCourse(admin);
    createCourseAssignment(
        admin, fixture.group().getId(), course.getId(), fixture.academicYear().getId(), 1);

    var response =
        post(
            API + "/course-assignments",
            admin,
            new CourseAssignmentCreateRequest(
                fixture.group().getId(), course.getId(), fixture.academicYear().getId(), 1),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void create_returns_not_found_for_unknown_group() {
    var admin = bootstrapAdminToken();
    var course = createCourse(admin);
    var academicYear = createAcademicYear(admin);

    var response =
        post(
            API + "/course-assignments",
            admin,
            new CourseAssignmentCreateRequest(
                UUID.randomUUID(), course.getId(), academicYear.getId(), 1),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void create_returns_not_found_for_unknown_course() {
    var admin = bootstrapAdminToken();
    var fixture = createLinkedPromotionAndGroup(admin);

    var response =
        post(
            API + "/course-assignments",
            admin,
            new CourseAssignmentCreateRequest(
                fixture.group().getId(), UUID.randomUUID(), fixture.academicYear().getId(), 1),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void create_returns_not_found_for_unknown_academic_year() {
    var admin = bootstrapAdminToken();
    var fixture = createLinkedPromotionAndGroup(admin);
    var course = createCourse(admin);

    var response =
        post(
            API + "/course-assignments",
            admin,
            new CourseAssignmentCreateRequest(
                fixture.group().getId(), course.getId(), UUID.randomUUID(), 1),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void assign_teacher_to_course_assignment_succeeds_and_appears_in_listing() {
    var admin = bootstrapAdminToken();
    var fixture = createLinkedPromotionAndGroup(admin);
    var course = createCourse(admin);
    var assignment =
        createCourseAssignment(
            admin, fixture.group().getId(), course.getId(), fixture.academicYear().getId(), 1);
    var teacherUser = register("teachforcourse");
    var teacherAccount = promoteToTeacher(admin, teacherUser.getId());
    var teacher = teacherRepository.findByUserAccountId(teacherAccount.getId()).orElseThrow();

    var response =
        post(
            API + "/course-assignments/" + assignment.getId() + "/teachers/" + teacher.getId(),
            admin,
            null,
            CourseTeacher.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().getTeacherId()).isEqualTo(teacher.getId());
  }

  @Test
  void assign_same_teacher_twice_conflicts() {
    var admin = bootstrapAdminToken();
    var fixture = createLinkedPromotionAndGroup(admin);
    var course = createCourse(admin);
    var assignment =
        createCourseAssignment(
            admin, fixture.group().getId(), course.getId(), fixture.academicYear().getId(), 1);
    var teacherUser = register("teachtwice");
    var teacherAccount = promoteToTeacher(admin, teacherUser.getId());
    var teacher = teacherRepository.findByUserAccountId(teacherAccount.getId()).orElseThrow();
    post(
        API + "/course-assignments/" + assignment.getId() + "/teachers/" + teacher.getId(),
        admin,
        null,
        CourseTeacher.class);

    var response =
        post(
            API + "/course-assignments/" + assignment.getId() + "/teachers/" + teacher.getId(),
            admin,
            null,
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void assign_teacher_to_unknown_course_assignment_returns_not_found() {
    var admin = bootstrapAdminToken();
    var teacherUser = register("teachghostassignment");
    var teacherAccount = promoteToTeacher(admin, teacherUser.getId());
    var teacher = teacherRepository.findByUserAccountId(teacherAccount.getId()).orElseThrow();

    var response =
        post(
            API + "/course-assignments/" + UUID.randomUUID() + "/teachers/" + teacher.getId(),
            admin,
            null,
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void assign_unknown_teacher_returns_not_found() {
    var admin = bootstrapAdminToken();
    var fixture = createLinkedPromotionAndGroup(admin);
    var course = createCourse(admin);
    var assignment =
        createCourseAssignment(
            admin, fixture.group().getId(), course.getId(), fixture.academicYear().getId(), 1);

    var response =
        post(
            API + "/course-assignments/" + assignment.getId() + "/teachers/" + UUID.randomUUID(),
            admin,
            null,
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }
}
