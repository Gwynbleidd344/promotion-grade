package api.poja.app.endpoint.rest.dto;

import api.poja.app.entity.UserAccount;
import api.poja.app.entity.enums.UserRole;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserAccountResponse(
    UUID id,
    String username,
    String email,
    UserRole role,
    boolean enabled,
    LocalDateTime createdAt) {

  public static UserAccountResponse from(UserAccount userAccount) {
    return new UserAccountResponse(
        userAccount.getId(),
        userAccount.getUsername(),
        userAccount.getEmail(),
        userAccount.getRole(),
        userAccount.isEnabled(),
        userAccount.getCreatedAt());
  }
}
