package api.poja.app.endpoint.rest.controller;

import api.poja.app.model.Teacher;
import api.poja.app.service.TeacherService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teachers")
@AllArgsConstructor
public class TeacherController {

  private final TeacherService teacherService;

  @GetMapping
  public ResponseEntity<List<Teacher>> listTeachers(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    return ResponseEntity.ok(teacherService.list(page, size));
  }
}
