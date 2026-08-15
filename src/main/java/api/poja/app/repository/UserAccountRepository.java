package api.poja.app.repository;

import api.poja.app.entity.UserAccount;
import api.poja.app.entity.enums.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

  Optional<UserAccount> findByUsername(String username);

  Optional<UserAccount> findByEmail(String email);

  Page<UserAccount> findByRole(UserRole role, Pageable pageable);
}
