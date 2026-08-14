package api.poja.app.repository;

import api.poja.app.entity.StudentGroupHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentGroupHistoryRepository extends JpaRepository<StudentGroupHistory, UUID> {

  List<StudentGroupHistory> findByStudentIdOrderByStartDateAsc(UUID studentId);

  List<StudentGroupHistory> findByStudentIdAndAcademicYearId(UUID studentId, UUID academicYearId);

  List<StudentGroupHistory> findByGroupIdAndAcademicYearId(UUID groupId, UUID academicYearId);
}
