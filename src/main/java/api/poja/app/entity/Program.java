package api.poja.app.entity;

import api.poja.app.entity.enums.ProgramCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "program")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Program {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, unique = true)
  private ProgramCode code;

  @Column(nullable = false)
  private String name;

  private String description;

  @OneToMany(mappedBy = "program", fetch = FetchType.LAZY)
  private List<Student> students = new ArrayList<>();

  @OneToMany(mappedBy = "program", fetch = FetchType.LAZY)
  private List<ProgramCourse> programCourses = new ArrayList<>();
}
