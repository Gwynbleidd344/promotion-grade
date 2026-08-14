package api.poja.app.service;

import api.poja.app.entity.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Issues and validates the JWTs used to authenticate API calls. */
@Component
@Slf4j
public class JwtService {

  private static final String ROLE_CLAIM = "role";
  private static final String USER_ID_CLAIM = "userId";

  private final SecretKey signingKey;
  private final long expirationMs;

  public JwtService(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.expiration-ms:3600000}") long expirationMs) {
    this.signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    this.expirationMs = expirationMs;
  }

  public String generateToken(UserAccount userAccount) {
    Date now = new Date();
    Date expiration = new Date(now.getTime() + expirationMs);

    var builder =
        Jwts.builder()
            .subject(userAccount.getUsername())
            .claim(USER_ID_CLAIM, userAccount.getId().toString())
            .issuedAt(now)
            .expiration(expiration)
            .signWith(signingKey);

    if (userAccount.getRole() != null) {
      builder.claim(ROLE_CLAIM, userAccount.getRole().name());
    }

    return builder.compact();
  }

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public UUID extractUserId(String token) {
    return UUID.fromString(extractClaim(token, claims -> claims.get(USER_ID_CLAIM, String.class)));
  }

  public boolean isTokenValid(String token, String expectedUsername) {
    try {
      String username = extractUsername(token);
      return username.equals(expectedUsername) && !isExpired(token);
    } catch (JwtException | IllegalArgumentException e) {
      log.warn("Invalid JWT: {}", e.getMessage());
      return false;
    }
  }

  public long getExpirationSeconds() {
    return expirationMs / 1000;
  }

  private boolean isExpired(String token) {
    return extractClaim(token, Claims::getExpiration).before(new Date());
  }

  private <T> T extractClaim(String token, Function<Claims, T> resolver) {
    Claims claims =
        Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    return resolver.apply(claims);
  }
}
