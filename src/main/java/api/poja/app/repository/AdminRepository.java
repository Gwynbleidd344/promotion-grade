package api.poja.app.repository;

import api.poja.app.entity.Admin;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<Admin, UUID> {

  Optional<Admin> findByUserAccountId(UUID userAccountId);

  Optional<Admin> findByAdminNumber(String adminNumber);
}
