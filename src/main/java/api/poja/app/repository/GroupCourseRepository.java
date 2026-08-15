package api.poja.app.repository;

import api.poja.app.entity.GroupCourse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupCourseRepository
    extends JpaRepository<GroupCourse, UUID>, JpaSpecificationExecutor<GroupCourse> {

  List<GroupCourse> findByGroupIdAndAcademicYearId(UUID groupId, UUID academicYearId);

  List<GroupCourse> findByGroupIdAndAcademicYearIdAndSemester(
      UUID groupId, UUID academicYearId, Integer semester);

  List<GroupCourse> findByCourseIdAndAcademicYearId(UUID courseId, UUID academicYearId);

  Optional<GroupCourse> findByGroupIdAndCourseIdAndAcademicYearIdAndSemester(
      UUID groupId, UUID courseId, UUID academicYearId, Integer semester);
}
