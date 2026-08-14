package api.poja.app.repository;

import api.poja.app.entity.Program;
import api.poja.app.entity.enums.ProgramCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgramRepository extends JpaRepository<Program, UUID> {

  Optional<Program> findByCode(ProgramCode code);
}
