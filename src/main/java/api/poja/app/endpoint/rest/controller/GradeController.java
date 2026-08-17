package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.dto.GradeCreateRequest;
import api.poja.app.endpoint.rest.dto.GradeUpdateRequest;
import api.poja.app.model.Grade;
import api.poja.app.service.GradeService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class GradeController {

  private final GradeService gradeService;

  @PostMapping("/api/v1/exams/{id}/grades")
  public ResponseEntity<Grade> createGrade(
      @PathVariable UUID id, @Valid @RequestBody GradeCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(gradeService.create(id, request));
  }

  @PutMapping("/api/v1/grades/{id}")
  public ResponseEntity<Grade> updateGrade(
      @PathVariable UUID id, @Valid @RequestBody GradeUpdateRequest request) {
    return ResponseEntity.ok(gradeService.update(id, request));
  }
}
