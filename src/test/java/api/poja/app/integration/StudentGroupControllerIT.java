package api.poja.app.integration;

import static org.assertj.core.api.Assertions.assertThat;

import api.poja.app.endpoint.rest.dto.ErrorResponse;
import api.poja.app.endpoint.rest.dto.StudentGroupCreateRequest;
import api.poja.app.model.StudentGroup;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

class StudentGroupControllerIT extends IntegrationTestSupport {

  @Test
  void admin_creates_group() {
    var admin = bootstrapAdminToken();

    var group = createGroup(admin);

    assertThat(group.getId()).isNotNull();
  }

  @Test
  void create_rejects_duplicate_reference() {
    var admin = bootstrapAdminToken();
    var group = createGroup(admin);

    var response =
        post(
            API + "/groups",
            admin,
            new StudentGroupCreateRequest(group.getReference(), "Other name"),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("already exists");
  }

  @Test
  void create_rejects_blank_reference() {
    var admin = bootstrapAdminToken();

    var response =
        post(
            API + "/groups",
            admin,
            new StudentGroupCreateRequest(" ", "name"),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void list_groups() {
    var admin = bootstrapAdminToken();
    var group = createGroup(admin);

    var response =
        restTemplate.exchange(
            API + "/groups?page=0&size=500",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(admin)),
            new ParameterizedTypeReference<List<StudentGroup>>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).extracting(StudentGroup::getId).contains(group.getId());
  }
}
