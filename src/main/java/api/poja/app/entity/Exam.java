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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Exam {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_course_id", nullable = false)
  private GroupCourse groupCourse;

  @Column(nullable = false)
  private String name;

  @Column(name = "exam_date")
  private LocalDate examDate;

  @Column(name = "exam_time")
  private LocalTime examTime;

  @Column(nullable = false, precision = 4, scale = 3)
  private BigDecimal coefficient;

  @OneToMany(mappedBy = "exam", fetch = FetchType.LAZY)
  private List<Grade> grades = new ArrayList<>();
}
