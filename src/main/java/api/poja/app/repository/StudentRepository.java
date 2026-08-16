package api.poja.app.repository;

import api.poja.app.entity.Student;
import api.poja.app.entity.enums.ProgramCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

  Optional<Student> findByUserAccountId(UUID userAccountId);

  Optional<Student> findByStudentNumber(String studentNumber);

  List<Student> findByPromotionId(UUID promotionId);

  List<Student> findByPromotionIdAndProgramId(UUID promotionId, UUID programId);

  @Query(
      "select s from Student s where "
          + "(:promotionId is null or s.promotion.id = :promotionId) "
          + "and (:programCode is null or s.program.code = :programCode)")
  Page<Student> findAllFiltered(
      @Param("promotionId") UUID promotionId,
      @Param("programCode") ProgramCode programCode,
      Pageable pageable);
}
