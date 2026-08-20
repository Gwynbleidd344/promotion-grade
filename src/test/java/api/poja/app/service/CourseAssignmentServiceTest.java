package api.poja.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import api.poja.app.endpoint.rest.dto.CourseAssignmentCreateRequest;
import api.poja.app.entity.AcademicYear;
import api.poja.app.entity.Course;
import api.poja.app.entity.GroupCourse;
import api.poja.app.entity.StudentGroup;
import api.poja.app.repository.AcademicYearRepository;
import api.poja.app.repository.CourseRepository;
import api.poja.app.repository.GroupCourseRepository;
import api.poja.app.repository.StudentGroupRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CourseAssignmentServiceTest {

  @Mock private GroupCourseRepository groupCourseRepository;
  @Mock private StudentGroupRepository studentGroupRepository;
  @Mock private CourseRepository courseRepository;
  @Mock private AcademicYearRepository academicYearRepository;

  private CourseAssignmentService service;

  private UUID groupId;
  private UUID courseId;
  private UUID academicYearId;

  @BeforeEach
  void setUp() {
    service =
        new CourseAssignmentService(
            groupCourseRepository,
            studentGroupRepository,
            courseRepository,
            academicYearRepository);
    groupId = UUID.randomUUID();
    courseId = UUID.randomUUID();
    academicYearId = UUID.randomUUID();
  }

  private StudentGroup group() {
    var g = new StudentGroup();
    g.setId(groupId);
    g.setReference("K1");
    return g;
  }

  private Course course() {
    var c = new Course();
    c.setId(courseId);
    c.setReference("PROG1");
    c.setCredits(5);
    return c;
  }

  private AcademicYear academicYear() {
    var y = new AcademicYear();
    y.setId(academicYearId);
    y.setLabel("2025-2026");
    return y;
  }

  @Test
  void lists_assignments_using_specification() {
    var groupCourse = new GroupCourse();
    groupCourse.setId(UUID.randomUUID());
    groupCourse.setGroup(group());
    groupCourse.setCourse(course());
    groupCourse.setAcademicYear(academicYear());
    groupCourse.setSemester(1);
    groupCourse.setCourseTeachers(List.of());
    when(groupCourseRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class)))
        .thenReturn(List.of(groupCourse));

    var result = service.list(groupId, courseId, academicYearId, 1);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getSemester()).isEqualTo(1);
  }

  @Test
  void creates_assignment_when_combination_is_new() {
    var request = new CourseAssignmentCreateRequest(groupId, courseId, academicYearId, 1);
    when(studentGroupRepository.findById(groupId)).thenReturn(Optional.of(group()));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course()));
    when(academicYearRepository.findById(academicYearId)).thenReturn(Optional.of(academicYear()));
    when(groupCourseRepository.findByGroupIdAndCourseIdAndAcademicYearIdAndSemester(
            groupId, courseId, academicYearId, 1))
        .thenReturn(Optional.empty());

    var savedEntity = new GroupCourse();
    savedEntity.setId(UUID.randomUUID());
    savedEntity.setGroup(group());
    savedEntity.setCourse(course());
    savedEntity.setAcademicYear(academicYear());
    savedEntity.setSemester(1);
    savedEntity.setCourseTeachers(List.of());
    when(groupCourseRepository.save(any())).thenReturn(savedEntity);

    var result = service.create(request);

    assertThat(result.getGroupId()).isEqualTo(groupId);
    assertThat(result.getCourseId()).isEqualTo(courseId);
  }

  @Test
  void rejects_creation_when_group_not_found() {
    var request = new CourseAssignmentCreateRequest(groupId, courseId, academicYearId, 1);
    when(studentGroupRepository.findById(groupId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Group not found");
  }

  @Test
  void rejects_creation_when_course_not_found() {
    var request = new CourseAssignmentCreateRequest(groupId, courseId, academicYearId, 1);
    when(studentGroupRepository.findById(groupId)).thenReturn(Optional.of(group()));
    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Course not found");
  }

  @Test
  void rejects_creation_when_academic_year_not_found() {
    var request = new CourseAssignmentCreateRequest(groupId, courseId, academicYearId, 1);
    when(studentGroupRepository.findById(groupId)).thenReturn(Optional.of(group()));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course()));
    when(academicYearRepository.findById(academicYearId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Academic year not found");
  }

  @Test
  void rejects_creation_when_combination_already_exists() {
    var request = new CourseAssignmentCreateRequest(groupId, courseId, academicYearId, 1);
    when(studentGroupRepository.findById(groupId)).thenReturn(Optional.of(group()));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course()));
    when(academicYearRepository.findById(academicYearId)).thenReturn(Optional.of(academicYear()));

    var existing = new GroupCourse();
    existing.setId(UUID.randomUUID());
    when(groupCourseRepository.findByGroupIdAndCourseIdAndAcademicYearIdAndSemester(
            groupId, courseId, academicYearId, 1))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("already exists");
  }
}
