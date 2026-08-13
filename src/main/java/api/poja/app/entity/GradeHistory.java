package api.poja.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "grade_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GradeHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "grade_id", nullable = false)
  private Grade grade;

  @Column(name = "old_value", precision = 5, scale = 2)
  private BigDecimal oldValue;

  @Column(name = "new_value", nullable = false, precision = 5, scale = 2)
  private BigDecimal newValue;

  @Column(name = "changed_at", nullable = false)
  private LocalDateTime changedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "changed_by_teacher_id")
  private Teacher changedBy;

  private String reason;
}
