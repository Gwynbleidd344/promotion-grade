package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.dto.RoleChangeRequest;
import api.poja.app.endpoint.rest.dto.UserAccountResponse;
import api.poja.app.entity.enums.UserRole;
import api.poja.app.service.UserAccountService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class UserController {

  private final UserAccountService userAccountService;

  @GetMapping
  public ResponseEntity<List<UserAccountResponse>> listUsers(
      @RequestParam(required = false) UserRole role,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    return ResponseEntity.ok(userAccountService.list(role, page, size));
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserAccountResponse> getUser(@PathVariable UUID id) {
    return ResponseEntity.ok(userAccountService.getById(id));
  }

  @PatchMapping("/{id}/role")
  public ResponseEntity<UserAccountResponse> changeRole(
      @PathVariable UUID id, @Valid @RequestBody RoleChangeRequest request) {
    return ResponseEntity.ok(userAccountService.changeRole(id, request));
  }
}
