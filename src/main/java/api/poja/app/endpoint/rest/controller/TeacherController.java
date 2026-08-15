package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.dto.TeacherCreateRequest;
import api.poja.app.endpoint.rest.dto.TeacherResponse;
import api.poja.app.service.TeacherService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teachers")
@AllArgsConstructor
public class TeacherController {

  private final TeacherService teacherService;

  @GetMapping
  public ResponseEntity<List<TeacherResponse>> listTeachers(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    return ResponseEntity.ok(teacherService.list(page, size));
  }

  @PostMapping
  public ResponseEntity<TeacherResponse> createTeacher(
      @Valid @RequestBody TeacherCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(teacherService.create(request));
  }
}
