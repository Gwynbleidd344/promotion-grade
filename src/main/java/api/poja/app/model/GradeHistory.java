package api.poja.app.model;

import java.math.BigDecimal;
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
public class GradeHistory {
  private UUID id;
  private UUID gradeId;
  private BigDecimal oldValue;
  private BigDecimal newValue;
  private LocalDateTime changedAt;
  private UUID changedByUserAccountId;
  private String reason;
}
