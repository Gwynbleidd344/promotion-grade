package api.poja.app.repository;

import api.poja.app.entity.AcademicYear;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {

  Optional<AcademicYear> findByLabel(String label);

  List<AcademicYear> findAllByOrderByStartDateAsc();
}
