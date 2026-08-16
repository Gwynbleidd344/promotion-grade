package api.poja.app.integration;

import static org.assertj.core.api.Assertions.assertThat;

import api.poja.app.model.Teacher;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

class TeacherControllerIT extends IntegrationTestSupport {

  @Test
  void list_teachers_includes_newly_promoted_teacher() {
    var admin = bootstrapAdminToken();
    var user = register("listedteacher");
    promoteToTeacher(admin, user.getId());

    var response =
        restTemplate.exchange(
            API + "/teachers?page=0&size=1000",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(admin)),
            new ParameterizedTypeReference<List<Teacher>>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).extracting(Teacher::getUserAccountId).contains(user.getId());
  }

  @Test
  void list_teachers_requires_authentication() {
    var response = get(API + "/teachers", null, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
