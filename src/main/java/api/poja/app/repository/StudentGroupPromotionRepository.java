package api.poja.app.repository;

import api.poja.app.entity.StudentGroupPromotion;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentGroupPromotionRepository
    extends JpaRepository<StudentGroupPromotion, UUID> {

  boolean existsByGroupIdAndPromotionId(UUID groupId, UUID promotionId);
}
