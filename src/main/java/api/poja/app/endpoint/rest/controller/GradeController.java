package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.dto.GradeCreateRequest;
import api.poja.app.endpoint.rest.dto.GradeUpdateRequest;
import api.poja.app.model.Grade;
import api.poja.app.model.GradeHistory;
import api.poja.app.service.GradeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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

  @GetMapping("/api/v1/students/{id}/grades")
  public ResponseEntity<List<Grade>> listStudentGrades(
      @PathVariable UUID id,
      @RequestParam(required = false) UUID courseId,
      @RequestParam(required = false) UUID academicYearId) {
    return ResponseEntity.ok(gradeService.listForStudent(id, courseId, academicYearId));
  }

  @GetMapping("/api/v1/grades/{id}/history")
  public ResponseEntity<List<GradeHistory>> getGradeHistory(@PathVariable UUID id) {
    return ResponseEntity.ok(gradeService.history(id));
  }
}
