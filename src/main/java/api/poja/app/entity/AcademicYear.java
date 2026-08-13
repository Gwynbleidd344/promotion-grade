package api.poja.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "academic_year")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYear {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String label;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @OneToMany(mappedBy = "academicYear", fetch = FetchType.LAZY)
  private List<StudentGroupHistory> groupHistoryEntries = new ArrayList<>();

  @OneToMany(mappedBy = "academicYear", fetch = FetchType.LAZY)
  private List<GroupCourse> groupCourses = new ArrayList<>();

  @OneToMany(mappedBy = "academicYear", fetch = FetchType.LAZY)
  private List<AcademicReport> academicReports = new ArrayList<>();
}
