package api.poja.app.model;

import api.poja.app.entity.enums.UserRole;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount {
  private UUID id;
  private String username;
  private String email;
  private UserRole role;
  private boolean enabled;
  private LocalDateTime createdAt;
}
