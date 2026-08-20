package api.poja.app.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraduationStatus {
  private UUID studentId;
  private boolean graduated;
  private BigDecimal generalAverage;
  private Integer totalCreditsObtained;
  private List<FailedCourse> failedCourses;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FailedCourse {
    private UUID courseId;
    private String courseReference;
    private BigDecimal average;
  }
}
