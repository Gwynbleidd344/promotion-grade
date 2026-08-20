package api.poja.app.model;

import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYear {
  private UUID id;
  private String label;
  private LocalDate startDate;
  private LocalDate endDate;
}
