package api.poja.app.security;

import api.poja.app.repository.UserAccountRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserAccountRepository userAccountRepository;

  @Override
  public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
    return userAccountRepository
        .findByUsername(usernameOrEmail)
        .or(() -> userAccountRepository.findByEmail(usernameOrEmail))
        .orElseThrow(() -> new UsernameNotFoundException("No account for " + usernameOrEmail));
  }
}
