package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.dto.ExamCreateRequest;
import api.poja.app.model.Exam;
import api.poja.app.service.ExamService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/course-assignments/{id}/exams")
@AllArgsConstructor
public class ExamController {

  private final ExamService examService;

  @PostMapping
  public ResponseEntity<Exam> createExam(
      @PathVariable UUID id, @Valid @RequestBody ExamCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(examService.create(id, request));
  }

  @GetMapping
  public ResponseEntity<List<Exam>> listExams(@PathVariable UUID id) {
    return ResponseEntity.ok(examService.list(id));
  }
}
