package api.poja.app.endpoint.rest.controller.auth;

import api.poja.app.endpoint.rest.dto.LoginRequest;
import api.poja.app.endpoint.rest.dto.LoginResponse;
import api.poja.app.endpoint.rest.dto.RegisterRequest;
import api.poja.app.endpoint.rest.dto.UserAccountResponse;
import api.poja.app.entity.UserAccount;
import api.poja.app.repository.UserAccountRepository;
import api.poja.app.security.JwtService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final UserAccountRepository userAccountRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  @PostMapping("/register")
  public ResponseEntity<UserAccountResponse> register(@Valid @RequestBody RegisterRequest request) {
    if (userAccountRepository.findByUsername(request.username()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
    }
    if (userAccountRepository.findByEmail(request.email()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already taken");
    }

    var userAccount = new UserAccount();
    userAccount.setUsername(request.username());
    userAccount.setEmail(request.email());
    userAccount.setPasswordHash(passwordEncoder.encode(request.password()));
    userAccount.setRole(null);
    userAccount.setEnabled(true);

    var saved = userAccountRepository.save(userAccount);
    return ResponseEntity.status(HttpStatus.CREATED).body(UserAccountResponse.from(saved));
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    var authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password()));

    var userAccount = (UserAccount) authentication.getPrincipal();
    var token = jwtService.generateToken(userAccount);

    return ResponseEntity.ok(
        new LoginResponse(
            token, "Bearer", userAccount.getRole(), jwtService.getExpirationSeconds()));
  }
}
