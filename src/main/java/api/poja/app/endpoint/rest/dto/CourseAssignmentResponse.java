package api.poja.app.endpoint.rest.dto;

import api.poja.app.entity.GroupCourse;
import java.util.List;
import java.util.UUID;

public record CourseAssignmentResponse(
    UUID id,
    UUID groupId,
    UUID courseId,
    UUID academicYearId,
    Integer semester,
    List<TeacherResponse> teachers) {

  public static CourseAssignmentResponse from(GroupCourse groupCourse) {
    var teachers =
        groupCourse.getCourseTeachers().stream()
            .map(courseTeacher -> TeacherResponse.from(courseTeacher.getTeacher()))
            .toList();

    return new CourseAssignmentResponse(
        groupCourse.getId(),
        groupCourse.getGroup().getId(),
        groupCourse.getCourse().getId(),
        groupCourse.getAcademicYear().getId(),
        groupCourse.getSemester(),
        teachers);
  }
}
