package api.poja.app.model;

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
public class CourseAssignment {
  private UUID id;
  private UUID groupId;
  private UUID courseId;
  private UUID academicYearId;
  private Integer semester;
  private List<Teacher> teachers;
}
