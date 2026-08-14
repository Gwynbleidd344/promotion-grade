package api.poja.app.repository;

import api.poja.app.entity.Promotion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

  Optional<Promotion> findByGraduationYear(Integer graduationYear);

  List<Promotion> findAllByOrderByGraduationYearDesc();
}
