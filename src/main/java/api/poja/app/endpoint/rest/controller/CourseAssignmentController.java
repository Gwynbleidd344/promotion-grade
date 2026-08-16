package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.dto.CourseAssignmentCreateRequest;
import api.poja.app.model.CourseAssignment;
import api.poja.app.model.CourseTeacher;
import api.poja.app.service.CourseAssignmentService;
import api.poja.app.service.CourseTeacherService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/course-assignments")
@AllArgsConstructor
public class CourseAssignmentController {

  private final CourseAssignmentService courseAssignmentService;
  private final CourseTeacherService courseTeacherService;

  @GetMapping
  public ResponseEntity<List<CourseAssignment>> listCourseAssignments(
      @RequestParam(required = false) UUID groupId,
      @RequestParam(required = false) UUID courseId,
      @RequestParam(required = false) UUID academicYearId,
      @RequestParam(required = false) Integer semester) {
    return ResponseEntity.ok(
        courseAssignmentService.list(groupId, courseId, academicYearId, semester));
  }

  @PostMapping
  public ResponseEntity<CourseAssignment> createCourseAssignment(
      @Valid @RequestBody CourseAssignmentCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(courseAssignmentService.create(request));
  }

  @PostMapping("/{id}/teachers/{teacherId}")
  public ResponseEntity<CourseTeacher> assignTeacher(
      @PathVariable UUID id, @PathVariable UUID teacherId) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(courseTeacherService.assignTeacher(id, teacherId));
  }
}
