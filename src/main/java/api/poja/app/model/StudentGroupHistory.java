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
public class StudentGroupHistory {
    private UUID id;
    private UUID studentId;
    private UUID groupId;
    private UUID academicYearId;
    private Integer semester;
    private LocalDate startDate;
    private LocalDate endDate;
}