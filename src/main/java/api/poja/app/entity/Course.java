package api.poja.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Course {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String reference;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private Integer credits;

  @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
  private List<ProgramCourse> programCourses = new ArrayList<>();

  @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
  private List<GroupCourse> groupCourses = new ArrayList<>();
}
