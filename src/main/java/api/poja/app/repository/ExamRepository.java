package api.poja.app.repository;

import api.poja.app.entity.Exam;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamRepository extends JpaRepository<Exam, UUID> {

  List<Exam> findByGroupCourseId(UUID groupCourseId);
}
