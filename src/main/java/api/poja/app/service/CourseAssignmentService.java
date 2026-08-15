package api.poja.app.service;

import api.poja.app.endpoint.rest.dto.CourseAssignmentCreateRequest;
import api.poja.app.endpoint.rest.dto.CourseAssignmentResponse;
import api.poja.app.entity.GroupCourse;
import api.poja.app.repository.AcademicYearRepository;
import api.poja.app.repository.CourseRepository;
import api.poja.app.repository.GroupCourseRepository;
import api.poja.app.repository.model.GroupCourseSpecifications;
import api.poja.app.repository.StudentGroupRepository;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class CourseAssignmentService {

  private final GroupCourseRepository groupCourseRepository;
  private final StudentGroupRepository studentGroupRepository;
  private final CourseRepository courseRepository;
  private final AcademicYearRepository academicYearRepository;

  @Transactional(readOnly = true)
  public List<CourseAssignmentResponse> list(
      UUID groupId, UUID courseId, UUID academicYearId, Integer semester) {
    var spec = GroupCourseSpecifications.matching(groupId, courseId, academicYearId, semester);
    return groupCourseRepository.findAll(spec).stream().map(CourseAssignmentResponse::from).toList();
  }

  @Transactional
  public CourseAssignmentResponse create(CourseAssignmentCreateRequest request) {
    var group =
        studentGroupRepository
            .findById(request.groupId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    var course =
        courseRepository
            .findById(request.courseId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    var academicYear =
        academicYearRepository
            .findById(request.academicYearId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Academic year not found"));

    var alreadyExists =
        groupCourseRepository
            .findByGroupIdAndCourseIdAndAcademicYearIdAndSemester(
                request.groupId(), request.courseId(), request.academicYearId(), request.semester())
            .isPresent();
    if (alreadyExists) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "This group/course/year/semester combination already exists");
    }

    var groupCourse = new GroupCourse();
    groupCourse.setGroup(group);
    groupCourse.setCourse(course);
    groupCourse.setAcademicYear(academicYear);
    groupCourse.setSemester(request.semester());

    return CourseAssignmentResponse.from(groupCourseRepository.save(groupCourse));
  }
}
