package api.poja.app.integration;

import static org.assertj.core.api.Assertions.assertThat;

import api.poja.app.endpoint.rest.dto.ErrorResponse;
import api.poja.app.endpoint.rest.dto.PromoteAdminRequest;
import api.poja.app.endpoint.rest.dto.PromoteStudentRequest;
import api.poja.app.endpoint.rest.dto.PromoteTeacherRequest;
import api.poja.app.entity.enums.ProgramCode;
import api.poja.app.entity.enums.UserRole;
import api.poja.app.model.UserAccount;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

class UserControllerIT extends IntegrationTestSupport {

  @Test
  void admin_lists_and_reads_users_filtered_by_role() {
    var admin = bootstrapAdminToken();
    var teacherUser = register("filterteacher");
    promoteToTeacher(admin, teacherUser.getId());

    var response =
        restTemplate.exchange(
            API + "/users?role=TEC&page=0&size=1000",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(admin)),
            new ParameterizedTypeReference<List<UserAccount>>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).extracting(UserAccount::getId).contains(teacherUser.getId());
    assertThat(response.getBody()).allMatch(u -> u.getRole() == UserRole.TEC);
  }

  @Test
  void admin_gets_single_user_by_id() {
    var admin = bootstrapAdminToken();
    var user = register("gettableuser");

    var response = get(API + "/users/" + user.getId(), admin, UserAccount.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getUsername()).isEqualTo(user.getUsername());
  }

  @Test
  void get_unknown_user_returns_not_found() {
    var admin = bootstrapAdminToken();

    var response = get(API + "/users/" + UUID.randomUUID(), admin, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void non_admin_cannot_list_users() {
    var user = registerAndLogin("cannotlist");
    var response = get(API + "/users", user.token(), ErrorResponse.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void promote_to_student_succeeds_and_creates_student_number() {
    var admin = bootstrapAdminToken();
    var fixture = createLinkedPromotionAndGroup(admin);
    var user = register("futurestudent");

    var student =
        promoteToStudent(
            admin, user.getId(), fixture.promotion(), fixture.group(), fixture.academicYear());

    assertThat(student.getStudentNumber()).startsWith("STD");
    assertThat(student.getProgram()).isEqualTo(ProgramCode.EL);
    assertThat(student.getPromotionId()).isEqualTo(fixture.promotion().getId());
  }

  @Test
  void promote_to_student_leaves_program_unset_by_default() {
    var admin = bootstrapAdminToken();
    var fixture = createLinkedPromotionAndGroup(admin);
    var user = register("noprogrambydefault");

    var student =
        promoteToStudent(
            admin,
            user.getId(),
            fixture.promotion(),
            fixture.group(),
            fixture.academicYear(),
            null);

    assertThat(student.getProgram()).isNull();
  }

  @Test
  void promote_to_student_twice_fails() {
    var admin = bootstrapAdminToken();
    var fixture = createLinkedPromotionAndGroup(admin);
    var user = register("doublestudent");
    promoteToStudent(
        admin, user.getId(), fixture.promotion(), fixture.group(), fixture.academicYear());

    var request =
        new PromoteStudentRequest(
            "Jean",
            "Rakoto",
            fixture.promotion().getId(),
            fixture.group().getId(),
            fixture.academicYear().getId(),
            1,
            fixture.academicYear().getStartDate());
    var response =
        patch(
            API + "/users/" + user.getId() + "/role/student", admin, request, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("already a student");
  }

  @Test
  void promote_to_student_with_unknown_promotion_returns_bad_request() {
    var admin = bootstrapAdminToken();
    var fixture = createLinkedPromotionAndGroup(admin);
    var user = register("unknownpromostudent");

    var request =
        new PromoteStudentRequest(
            "Jean",
            "Rakoto",
            UUID.randomUUID(),
            fixture.group().getId(),
            fixture.academicYear().getId(),
            1,
            fixture.academicYear().getStartDate());
    var response =
        patch(
            API + "/users/" + user.getId() + "/role/student", admin, request, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("Unknown promotion");
  }

  @Test
  void promote_to_student_with_group_not_linked_to_promotion_returns_bad_request() {
    var admin = bootstrapAdminToken();
    var promotion = createPromotion(admin);
    var unlinkedGroup = createGroup(admin);
    var academicYear = createAcademicYear(admin);
    var user = register("unlinkedgroupstudent");

    var request =
        new PromoteStudentRequest(
            "Jean",
            "Rakoto",
            promotion.getId(),
            unlinkedGroup.getId(),
            academicYear.getId(),
            1,
            academicYear.getStartDate());
    var response =
        patch(
            API + "/users/" + user.getId() + "/role/student", admin, request, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("does not belong to promotion");
  }

  @Test
  void promote_to_teacher_succeeds_and_twice_fails() {
    var admin = bootstrapAdminToken();
    var user = register("teachertwice2");

    var account = promoteToTeacher(admin, user.getId());
    assertThat(account.getRole()).isEqualTo(UserRole.TEC);

    var response =
        patch(
            API + "/users/" + user.getId() + "/role/teacher",
            admin,
            new PromoteTeacherRequest("Marie", "Rasoa"),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("already a teacher");
  }

  @Test
  void promote_to_admin_succeeds_and_twice_fails() {
    var admin = bootstrapAdminToken();
    var user = register("futureadmin");

    var response =
        patch(
            API + "/users/" + user.getId() + "/role/admin",
            admin,
            new PromoteAdminRequest("Admin", "Secondaire"),
            UserAccount.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getRole()).isEqualTo(UserRole.ADM);

    var secondAttempt =
        patch(
            API + "/users/" + user.getId() + "/role/admin",
            admin,
            new PromoteAdminRequest("Admin", "Secondaire"),
            ErrorResponse.class);
    assertThat(secondAttempt.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(secondAttempt.getBody().message()).contains("already an admin");
  }

  @Test
  void promote_to_admin_on_unknown_user_returns_not_found() {
    var admin = bootstrapAdminToken();

    var response =
        patch(
            API + "/users/" + UUID.randomUUID() + "/role/admin",
            admin,
            new PromoteAdminRequest("Ghost", "User"),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }
}
