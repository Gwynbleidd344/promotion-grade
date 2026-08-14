package api.poja.app.repository;

import api.poja.app.entity.Grade;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeRepository extends JpaRepository<Grade, UUID> {

  List<Grade> findByStudentId(UUID studentId);

  List<Grade> findByExamId(UUID examId);

  Optional<Grade> findByStudentIdAndExamId(UUID studentId, UUID examId);

  List<Grade> findByStudentIdAndExam_GroupCourse_CourseId(UUID studentId, UUID courseId);
}
