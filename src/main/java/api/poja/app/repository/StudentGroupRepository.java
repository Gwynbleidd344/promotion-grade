package api.poja.app.repository;

import api.poja.app.entity.StudentGroup;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentGroupRepository extends JpaRepository<StudentGroup, UUID> {

  Optional<StudentGroup> findByReference(String reference);
}
