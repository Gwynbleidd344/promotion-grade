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
public class Grade {
  private UUID id;
  private UUID studentId;
  private UUID examId;
  private BigDecimal value;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
