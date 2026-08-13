package api.poja.app.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "group_course",
    uniqueConstraints =
        @UniqueConstraint(
            columnNames = {"group_id", "course_id", "academic_year_id", "semester"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupCourse {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id", nullable = false)
  private StudentGroup group;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "academic_year_id", nullable = false)
  private AcademicYear academicYear;

  private Integer semester;

  @OneToMany(mappedBy = "groupCourse", fetch = FetchType.LAZY)
  private List<CourseTeacher> courseTeachers = new ArrayList<>();

  @OneToMany(mappedBy = "groupCourse", fetch = FetchType.LAZY)
  private List<Exam> exams = new ArrayList<>();
}
