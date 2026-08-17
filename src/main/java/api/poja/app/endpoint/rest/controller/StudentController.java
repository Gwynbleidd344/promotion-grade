package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.dto.StudentGroupChangeRequest;
import api.poja.app.endpoint.rest.dto.StudentPromotionAndGroupChangeRequest;
import api.poja.app.entity.enums.ProgramCode;
import api.poja.app.model.Student;
import api.poja.app.service.StudentService;
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
@RequestMapping("/api/v1/students")
@AllArgsConstructor
public class StudentController {

  private final StudentService studentService;

  @GetMapping
  public ResponseEntity<List<Student>> listStudents(
      @RequestParam(required = false) UUID promotionId,
      @RequestParam(required = false) ProgramCode programCode,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    return ResponseEntity.ok(studentService.list(promotionId, programCode, page, size));
  }

  @GetMapping("/{id}")
  public ResponseEntity<Student> getStudent(@PathVariable UUID id) {
    return ResponseEntity.ok(studentService.getById(id));
  }

  @PatchMapping("/{id}/promotion-and-group")
  public ResponseEntity<Student> changePromotionAndGroup(
      @PathVariable UUID id, @Valid @RequestBody StudentPromotionAndGroupChangeRequest request) {
    return ResponseEntity.ok(studentService.changePromotionAndGroup(id, request));
  }

  @PatchMapping("/{id}/group")
  public ResponseEntity<Student> changeGroup(
      @PathVariable UUID id, @Valid @RequestBody StudentGroupChangeRequest request) {
    return ResponseEntity.ok(studentService.changeGroup(id, request));
  }

  @GetMapping("/{id}/group-history")
  public ResponseEntity<List<api.poja.app.model.StudentGroupHistory>> getGroupHistory(
      @PathVariable UUID id, @RequestParam(required = false) UUID academicYearId) {
    return ResponseEntity.ok(studentService.getGroupHistory(id, academicYearId));
  }
}
