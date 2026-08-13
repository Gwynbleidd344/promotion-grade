package api.poja.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Student {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_account_id", nullable = false, unique = true)
  private UserAccount userAccount;

  // Unique, just stored for now
  @Column(name = "student_number", nullable = false, unique = true)
  private String studentNumber;

  @Column(name = "first_name", nullable = false)
  private String firstName;

  @Column(name = "last_name", nullable = false)
  private String lastName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "promotion_id", nullable = false)
  private Promotion promotion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "program_id", nullable = false)
  private Program program;

  @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
  private List<StudentGroupHistory> groupHistory = new ArrayList<>();

  @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
  private List<Grade> grades = new ArrayList<>();

  @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
  private List<AcademicReport> academicReports = new ArrayList<>();

  @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
  private List<GraduationListEntry> graduationListEntries = new ArrayList<>();
}
