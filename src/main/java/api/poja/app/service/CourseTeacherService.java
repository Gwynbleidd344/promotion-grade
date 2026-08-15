package api.poja.app.service;

import api.poja.app.endpoint.rest.dto.CourseTeacherResponse;
import api.poja.app.entity.CourseTeacher;
import api.poja.app.repository.CourseTeacherRepository;
import api.poja.app.repository.GroupCourseRepository;
import api.poja.app.repository.TeacherRepository;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class CourseTeacherService {

  private final CourseTeacherRepository courseTeacherRepository;
  private final GroupCourseRepository groupCourseRepository;
  private final TeacherRepository teacherRepository;

  @Transactional
  public CourseTeacherResponse assignTeacher(UUID courseAssignmentId, UUID teacherId) {
    var groupCourse =
        groupCourseRepository
            .findById(courseAssignmentId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Course assignment not found"));
    var teacher =
        teacherRepository
            .findById(teacherId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));

    if (courseTeacherRepository.existsByTeacherIdAndGroupCourseId(teacherId, courseAssignmentId)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "The teacher is already assigned to this course");
    }

    var courseTeacher = new CourseTeacher();
    courseTeacher.setTeacher(teacher);
    courseTeacher.setGroupCourse(groupCourse);

    return CourseTeacherResponse.from(courseTeacherRepository.save(courseTeacher));
  }
}
