package api.poja.app.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraduationListEntry {
  private Integer rank;
  private String studentNumber;
  private String lastName;
  private String firstName;
  private BigDecimal generalAverage;
}
