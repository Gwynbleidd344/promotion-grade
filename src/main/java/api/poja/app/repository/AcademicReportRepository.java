package api.poja.app.repository;

import api.poja.app.entity.AcademicReport;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AcademicReportRepository extends JpaRepository<AcademicReport, UUID> {

  List<AcademicReport> findByStudentId(UUID studentId);

  Optional<AcademicReport> findByStudentIdAndAcademicYearId(
      UUID studentId, UUID academicYearId);
}
