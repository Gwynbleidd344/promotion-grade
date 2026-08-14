package api.poja.app.repository;

import api.poja.app.entity.Teacher;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, UUID> {

  Optional<Teacher> findByUserAccountId(UUID userAccountId);

  Optional<Teacher> findByEmployeeNumber(String employeeNumber);
}
