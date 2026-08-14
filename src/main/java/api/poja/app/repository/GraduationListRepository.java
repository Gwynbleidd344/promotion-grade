package api.poja.app.repository;

import api.poja.app.entity.GraduationList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GraduationListRepository extends JpaRepository<GraduationList, UUID> {

  List<GraduationList> findByPromotionId(UUID promotionId);
}
