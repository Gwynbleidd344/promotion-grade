package api.poja.app.integration;

import static org.assertj.core.api.Assertions.assertThat;

import api.poja.app.endpoint.rest.dto.ErrorResponse;
import api.poja.app.endpoint.rest.dto.StudentGroupChangeRequest;
import api.poja.app.endpoint.rest.dto.StudentPromotionAndGroupChangeRequest;
import api.poja.app.model.Student;
import api.poja.app.repository.StudentGroupHistoryRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

class StudentControllerIT extends IntegrationTestSupport {

  @Autowired private StudentGroupHistoryRepository studentGroupHistoryRepository;

  @Test
  void get_student_by_id() {
    var admin = bootstrapAdminToken();
    var fixture = createLinkedPromotionAndGroup(admin);
    var user = register("gettablestudent");
    var student =
        promoteToStudent(
            admin, user.getId(), fixture.promotion(), fixture.group(), fixture.academicYear());

    var response = get(API + "/students/" + student.getId(), admin, Student.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getId()).isEqualTo(student.getId());
  }

  @Test
  void get_unknown_student_returns_not_found() {
    var admin = bootstrapAdminToken();

    var response = get(API + "/students/" + UUID.randomUUID(), admin, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void list_students_filtered_by_promotion_and_program() {
    var admin = bootstrapAdminToken();
    var fixture = createLinkedPromotionAndGroup(admin);
    var user = register("filterablestudent");
    var student =
        promoteToStudent(
            admin, user.getId(), fixture.promotion(), fixture.group(), fixture.academicYear());

    var response =
        restTemplate.exchange(
            API
                + "/students?promotionId="
                + fixture.promotion().getId()
                + "&programCode=EL&page=0&size=1000",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(admin)),
            new ParameterizedTypeReference<List<Student>>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).extracting(Student::getId).contains(student.getId());
  }

  @Test
  void list_students_filtered_by_other_program_excludes_result() {
    var admin = bootstrapAdminToken();
    var fixture = createLinkedPromotionAndGroup(admin);
    var user = register("otherprogramstudent");
    var student =
        promoteToStudent(
            admin, user.getId(), fixture.promotion(), fixture.group(), fixture.academicYear());

    var response =
        restTemplate.exchange(
            API
                + "/students?promotionId="
                + fixture.promotion().getId()
                + "&programCode=TN&page=0&size=1000",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(admin)),
            new ParameterizedTypeReference<List<Student>>() {});

    assertThat(response.getBody()).extracting(Student::getId).doesNotContain(student.getId());
  }

  @Test
  void change_promotion_and_group_with_unknown_promotion_returns_bad_request() {
    var admin = bootstrapAdminToken();
    var fixture = createLinkedPromotionAndGroup(admin);
    var user = register("badpromostudent");
    var student =
        promoteToStudent(
            admin, user.getId(), fixture.promotion(), fixture.group(), fixture.academicYear());

    var request =
        new StudentPromotionAndGroupChangeRequest(
            UUID.randomUUID(),
            fixture.group().getId(),
            fixture.academicYear().getId(),
            1,
            LocalDate.now());

    var response =
        patch(
            API + "/students/" + student.getId() + "/promotion-and-group",
            admin,
            request,
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void change_group_to_one_outside_current_promotion_is_rejected() {
    var admin = bootstrapAdminToken();
    var fixture = createLinkedPromotionAndGroup(admin);
    var user = register("outsidegroupstudent");
    var student =
        promoteToStudent(
            admin, user.getId(), fixture.promotion(), fixture.group(), fixture.academicYear());
    var otherPromotionGroup = createGroup(admin);
    var otherPromotion = createPromotion(admin);
    linkGroupToPromotion(admin, otherPromotion.getId(), otherPromotionGroup.getId());

    var request =
        new StudentGroupChangeRequest(
            otherPromotionGroup.getId(), fixture.academicYear().getId(), 2, LocalDate.now());

    var response =
        patch(API + "/students/" + student.getId() + "/group", admin, request, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("does not belong to promotion");
  }
}
