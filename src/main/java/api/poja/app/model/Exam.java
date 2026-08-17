package api.poja.app.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Exam {
  private UUID id;
  private UUID courseAssignmentId;
  private String name;
  private LocalDate examDate;
  private LocalTime examTime;
  private BigDecimal coefficient;
}
