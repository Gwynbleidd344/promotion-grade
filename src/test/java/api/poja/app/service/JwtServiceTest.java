package api.poja.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import api.poja.app.entity.UserAccount;
import api.poja.app.entity.enums.UserRole;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  // >=32 bytes once decoded, base64-encoded: satisfies the HS256 minimum key length.
  private static final String SECRET =
      Base64.getEncoder()
          .encodeToString("this-is-a-test-secret-with-more-than-32-bytes!!".getBytes());

  private JwtService jwtService;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService(SECRET, 3600000L);
  }

  private UserAccount account(UserRole role) {
    var account = new UserAccount();
    account.setId(UUID.randomUUID());
    account.setUsername("jean.rakoto");
    account.setEmail("jean.rakoto@hei.mg");
    account.setPasswordHash("hash");
    account.setRole(role);
    account.setEnabled(true);
    return account;
  }

  @Test
  void generates_token_that_round_trips_username_and_user_id() {
    var account = account(UserRole.STD);

    String token = jwtService.generateToken(account);

    assertThat(jwtService.extractUsername(token)).isEqualTo("jean.rakoto");
    assertThat(jwtService.extractUserId(token)).isEqualTo(account.getId());
  }

  @Test
  void token_is_valid_for_the_expected_username() {
    var account = account(UserRole.TEC);
    String token = jwtService.generateToken(account);

    assertThat(jwtService.isTokenValid(token, "jean.rakoto")).isTrue();
  }

  @Test
  void token_is_invalid_for_a_different_username() {
    var account = account(UserRole.TEC);
    String token = jwtService.generateToken(account);

    assertThat(jwtService.isTokenValid(token, "someone.else")).isFalse();
  }

  @Test
  void malformed_token_is_reported_invalid_rather_than_throwing() {
    assertThat(jwtService.isTokenValid("not-a-real-token", "jean.rakoto")).isFalse();
  }

  @Test
  void generates_token_even_when_role_is_absent() {
    var account = account(null);

    String token = jwtService.generateToken(account);

    assertThat(jwtService.isTokenValid(token, "jean.rakoto")).isTrue();
  }

  @Test
  void expiration_seconds_reflects_configured_duration() {
    assertThat(jwtService.getExpirationSeconds()).isEqualTo(3600L);
  }

  @Test
  void already_expired_token_is_invalid() {
    var shortLivedService = new JwtService(SECRET, -1000L);
    var account = account(UserRole.ADM);

    String token = shortLivedService.generateToken(account);

    assertThat(shortLivedService.isTokenValid(token, "jean.rakoto")).isFalse();
  }
}
