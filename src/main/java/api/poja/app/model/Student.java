package api.poja.app.model;

import api.poja.app.entity.enums.ProgramCode;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student {
  private UUID id;
  private String studentNumber;
  private String firstName;
  private String lastName;
  private ProgramCode program;
  private UUID promotionId;
  private UUID userAccountId;
}
