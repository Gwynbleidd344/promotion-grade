package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.dto.StudentGroupCreateRequest;
import api.poja.app.endpoint.rest.dto.StudentGroupResponse;
import api.poja.app.service.StudentGroupService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups")
@AllArgsConstructor
public class StudentGroupController {

  private final StudentGroupService studentGroupService;

  @GetMapping
  public ResponseEntity<List<StudentGroupResponse>> listGroups(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    return ResponseEntity.ok(studentGroupService.list(page, size));
  }

  @PreAuthorize("hasRole('ADM')")
  @PostMapping
  public ResponseEntity<StudentGroupResponse> createGroup(
      @Valid @RequestBody StudentGroupCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(studentGroupService.create(request));
  }
}
