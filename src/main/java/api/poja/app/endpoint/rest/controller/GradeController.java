package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.dto.GradeCreateRequest;
import api.poja.app.model.Grade;
import api.poja.app.service.GradeService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
public class GradeController {

  private final GradeService gradeService;

  @PostMapping("/api/v1/exams/{id}/grades")
  public ResponseEntity<Grade> createGrade(
      @PathVariable UUID id, @Valid @RequestBody GradeCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(gradeService.create(id, request));
  }
}
