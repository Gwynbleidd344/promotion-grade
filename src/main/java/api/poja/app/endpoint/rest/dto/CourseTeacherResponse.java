package api.poja.app.endpoint.rest.dto;

import api.poja.app.entity.CourseTeacher;
import java.util.UUID;

public record CourseTeacherResponse(UUID id, UUID teacherId, UUID courseAssignmentId) {

  public static CourseTeacherResponse from(CourseTeacher courseTeacher) {
    return new CourseTeacherResponse(
        courseTeacher.getId(),
        courseTeacher.getTeacher().getId(),
        courseTeacher.getGroupCourse().getId());
  }
}
