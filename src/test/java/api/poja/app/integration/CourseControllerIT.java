package api.poja.app.integration;

import static org.assertj.core.api.Assertions.assertThat;

import api.poja.app.endpoint.rest.dto.CourseCreateRequest;
import api.poja.app.endpoint.rest.dto.ErrorResponse;
import api.poja.app.model.Course;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

class CourseControllerIT extends IntegrationTestSupport {

  @Test
  void admin_creates_course() {
    var admin = bootstrapAdminToken();

    var course = createCourse(admin);

    assertThat(course.getId()).isNotNull();
    assertThat(course.getCredits()).isEqualTo(6);
  }

  @Test
  void create_rejects_duplicate_reference() {
    var admin = bootstrapAdminToken();
    var course = createCourse(admin);

    var response =
        post(
            API + "/courses",
            admin,
            new CourseCreateRequest(course.getReference(), "Another title", 3),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("already exists");
  }

  @Test
  void create_rejects_non_positive_credits() {
    var admin = bootstrapAdminToken();

    var response =
        post(
            API + "/courses",
            admin,
            new CourseCreateRequest("REF-" + uniqueUsername("c"), "Title", 0),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void list_courses_is_public_to_authenticated_users() {
    var admin = bootstrapAdminToken();
    var course = createCourse(admin);

    var response =
        restTemplate.exchange(
            API + "/courses?page=0&size=500",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(admin)),
            new ParameterizedTypeReference<List<Course>>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).extracting(Course::getId).contains(course.getId());
  }
}
