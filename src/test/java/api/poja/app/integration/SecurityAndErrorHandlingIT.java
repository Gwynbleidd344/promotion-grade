package api.poja.app.integration;

import static org.assertj.core.api.Assertions.assertThat;

import api.poja.app.endpoint.rest.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class SecurityAndErrorHandlingIT extends IntegrationTestSupport {

  @Test
  void anonymous_request_to_protected_endpoint_is_unauthorized() {
    var response = get(API + "/students", null, ErrorResponse.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void authenticated_non_admin_can_read_public_get_endpoints() {
    var user = registerAndLogin("readeruser");

    var response = get(API + "/teachers", user.token(), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void malformed_json_body_returns_bad_request() {
    var admin = bootstrapAdminToken();
    var headers = authHeaders(admin);
    headers.setContentType(MediaType.APPLICATION_JSON);

    var response =
        restTemplate.exchange(
            API + "/academic-years",
            HttpMethod.POST,
            new HttpEntity<>("{not-valid-json", headers),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("Malformed JSON");
  }

  @Test
  void path_variable_type_mismatch_returns_bad_request() {
    var admin = bootstrapAdminToken();

    var response = get(API + "/students/not-a-uuid", admin, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("could not be converted");
  }

  @Test
  void invalid_bearer_token_is_treated_as_anonymous_and_rejected() {
    var headers = new HttpHeaders();
    headers.setBearerAuth("this.is.not.a.valid.jwt");

    var response =
        restTemplate.exchange(
            API + "/students", HttpMethod.GET, new HttpEntity<>(headers), ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void ping_and_health_endpoints_are_public() {
    var response = restTemplate.getForEntity("/ping", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("pong");
  }
}
