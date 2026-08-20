package api.poja.app.model;

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
public class GraduationListExport {
  private UUID promotionId;
  private String s3Key;
  private String downloadUrl;
  private LocalDateTime generatedAt;
}
