package api.poja.app.repository;

import api.poja.app.entity.CourseTeacher;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseTeacherRepository extends JpaRepository<CourseTeacher, UUID> {

  List<CourseTeacher> findByTeacherId(UUID teacherId);

  List<CourseTeacher> findByGroupCourseId(UUID groupCourseId);

  boolean existsByTeacherIdAndGroupCourseId(UUID teacherId, UUID groupCourseId);
}
