package api.poja.app.repository;

import api.poja.app.entity.GraduationListEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GraduationListEntryRepository extends JpaRepository<GraduationListEntry, UUID> {

  List<GraduationListEntry> findByGraduationListIdOrderByRankAsc(UUID graduationListId);

  List<GraduationListEntry> findByStudentId(UUID studentId);
}
