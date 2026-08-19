package api.poja.app.model;

import api.poja.app.entity.enums.ReportStatus;
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
public class AcademicReport {
  private UUID id;
  private UUID studentId;
  private UUID academicYearId;
  private ReportStatus status;
  private String pdfS3Key;
  private LocalDateTime generatedAt;
  private LocalDateTime sentAt;
  private BigDecimal generalAverage;
  private Integer totalCreditsObtained;

  // Not persisted: computed on read from BucketComponent so clients never need
  // to know the S3 key.
  private String downloadUrl;
}
