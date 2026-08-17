package api.poja.app.repository;

import api.poja.app.entity.Grade;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeRepository extends JpaRepository<Grade, UUID> {

  List<Grade> findByStudentId(UUID studentId);

  List<Grade> findByExamId(UUID examId);

  Optional<Grade> findByStudentIdAndExamId(UUID studentId, UUID examId);

  List<Grade> findByStudentIdAndExam_GroupCourse_CourseId(UUID studentId, UUID courseId);

  @Query(
      "select g from Grade g where g.student.id = :studentId "
          + "and (:courseId is null or g.exam.groupCourse.course.id = :courseId) "
          + "and (:academicYearId is null or g.exam.groupCourse.academicYear.id = :academicYearId) "
          + "order by g.createdAt desc")
  List<Grade> findByStudentIdFiltered(
      @Param("studentId") UUID studentId,
      @Param("courseId") UUID courseId,
      @Param("academicYearId") UUID academicYearId);
}
