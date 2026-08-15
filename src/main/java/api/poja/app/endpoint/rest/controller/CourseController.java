package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.dto.CourseCreateRequest;
import api.poja.app.model.Course;
import api.poja.app.service.CourseService;
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
@RequestMapping("/api/v1/courses")
@AllArgsConstructor
public class CourseController {

  private final CourseService courseService;

  @GetMapping
  public ResponseEntity<List<Course>> listCourses(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    return ResponseEntity.ok(courseService.list(page, size));
  }

  @PreAuthorize("hasRole('ADM')")
  @PostMapping
  public ResponseEntity<Course> createCourse(@Valid @RequestBody CourseCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(request));
  }
}
