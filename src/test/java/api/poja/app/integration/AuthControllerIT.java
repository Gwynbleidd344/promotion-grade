package api.poja.app.integration;

import static org.assertj.core.api.Assertions.assertThat;

import api.poja.app.endpoint.rest.dto.ErrorResponse;
import api.poja.app.endpoint.rest.dto.LoginRequest;
import api.poja.app.endpoint.rest.dto.LoginResponse;
import api.poja.app.endpoint.rest.dto.RegisterRequest;
import api.poja.app.entity.enums.UserRole;
import api.poja.app.model.UserAccount;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AuthControllerIT extends IntegrationTestSupport {

  @Test
  void register_creates_account_with_no_role() {
    String username = uniqueUsername("newuser");
    var response =
        post(
            API + "/auth/register",
            null,
            new RegisterRequest(username, username + "@hei.mg", DEFAULT_PASSWORD),
            UserAccount.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().getRole()).isNull();
    assertThat(response.getBody().isEnabled()).isTrue();
  }

  @Test
  void register_rejects_duplicate_username() {
    String username = uniqueUsername("dupuser");
    registerRaw(username, username + "@hei.mg", DEFAULT_PASSWORD);

    var response =
        post(
            API + "/auth/register",
            null,
            new RegisterRequest(username, uniqueUsername("other") + "@hei.mg", DEFAULT_PASSWORD),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void register_rejects_duplicate_email() {
    String email = uniqueUsername("dupemail") + "@hei.mg";
    registerRaw(uniqueUsername("first"), email, DEFAULT_PASSWORD);

    var response =
        post(
            API + "/auth/register",
            null,
            new RegisterRequest(uniqueUsername("second"), email, DEFAULT_PASSWORD),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void register_rejects_invalid_payload() {
    var response =
        post(
            API + "/auth/register",
            null,
            new RegisterRequest("", "not-an-email", "short"),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("Invalid input data");
  }

  @Test
  void login_succeeds_with_valid_credentials_and_returns_role() {
    var user = register("loginuser");

    var response =
        post(
            API + "/auth/login",
            null,
            new LoginRequest(user.getUsername(), DEFAULT_PASSWORD),
            LoginResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().accessToken()).isNotBlank();
    assertThat(response.getBody().tokenType()).isEqualTo("Bearer");
    assertThat(response.getBody().role()).isNull();
  }

  @Test
  void login_fails_with_wrong_password() {
    var user = register("badpassuser");

    var response =
        post(
            API + "/auth/login",
            null,
            new LoginRequest(user.getUsername(), "WrongPassword!23"),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void login_fails_for_unknown_username() {
    var response =
        post(
            API + "/auth/login",
            null,
            new LoginRequest(uniqueUsername("ghost"), DEFAULT_PASSWORD),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void login_returns_role_once_promoted() {
    var user = register("teacheruser");
    var adminToken = bootstrapAdminToken();
    promoteToTeacher(adminToken, user.getId());

    var response =
        post(
            API + "/auth/login",
            null,
            new LoginRequest(user.getUsername(), DEFAULT_PASSWORD),
            LoginResponse.class);

    assertThat(response.getBody().role()).isEqualTo(UserRole.TEC);
  }
}
