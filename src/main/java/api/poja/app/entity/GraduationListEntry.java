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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "graduation_list_entry",
    uniqueConstraints = @UniqueConstraint(columnNames = {"graduation_list_id", "student_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GraduationListEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "graduation_list_id", nullable = false)
  private GraduationList graduationList;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", nullable = false)
  private Student student;

  @Column(nullable = false)
  private Integer rank;

  @Column(name = "general_average", nullable = false, precision = 5, scale = 2)
  private BigDecimal generalAverage;
}
