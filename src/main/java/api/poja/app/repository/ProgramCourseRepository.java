package api.poja.app.repository;

import api.poja.app.entity.ProgramCourse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgramCourseRepository extends JpaRepository<ProgramCourse, UUID> {

  List<ProgramCourse> findByProgramId(UUID programId);

  List<ProgramCourse> findByCourseId(UUID courseId);

  Optional<ProgramCourse> findByProgramIdAndCourseId(UUID programId, UUID courseId);

  boolean existsByProgramIdAndCourseId(UUID programId, UUID courseId);
}
