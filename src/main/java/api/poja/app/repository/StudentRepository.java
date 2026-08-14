package api.poja.app.repository;

import api.poja.app.entity.Student;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

  Optional<Student> findByUserAccountId(UUID userAccountId);

  Optional<Student> findByStudentNumber(String studentNumber);

  List<Student> findByPromotionId(UUID promotionId);

  List<Student> findByPromotionIdAndProgramId(UUID promotionId, UUID programId);
}
