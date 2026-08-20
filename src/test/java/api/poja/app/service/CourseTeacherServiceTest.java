package api.poja.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import api.poja.app.entity.CourseTeacher;
import api.poja.app.entity.GroupCourse;
import api.poja.app.entity.Teacher;
import api.poja.app.repository.CourseTeacherRepository;
import api.poja.app.repository.GroupCourseRepository;
import api.poja.app.repository.TeacherRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CourseTeacherServiceTest {

  @Mock private CourseTeacherRepository courseTeacherRepository;
  @Mock private GroupCourseRepository groupCourseRepository;
  @Mock private TeacherRepository teacherRepository;

  private CourseTeacherService service;

  private UUID groupCourseId;
  private UUID teacherId;

  @BeforeEach
  void setUp() {
    service =
        new CourseTeacherService(courseTeacherRepository, groupCourseRepository, teacherRepository);
    groupCourseId = UUID.randomUUID();
    teacherId = UUID.randomUUID();
  }

  private GroupCourse groupCourse() {
    var gc = new GroupCourse();
    gc.setId(groupCourseId);
    return gc;
  }

  private Teacher teacher() {
    var t = new Teacher();
    t.setId(teacherId);
    t.setEmployeeNumber("TEC001");
    return t;
  }

  @Test
  void assigns_teacher_when_not_already_assigned() {
    when(groupCourseRepository.findById(groupCourseId)).thenReturn(Optional.of(groupCourse()));
    when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher()));
    when(courseTeacherRepository.existsByTeacherIdAndGroupCourseId(teacherId, groupCourseId))
        .thenReturn(false);

    var saved = new CourseTeacher();
    saved.setId(UUID.randomUUID());
    saved.setTeacher(teacher());
    saved.setGroupCourse(groupCourse());
    when(courseTeacherRepository.save(any())).thenReturn(saved);

    var result = service.assignTeacher(groupCourseId, teacherId);

    assertThat(result.getTeacherId()).isEqualTo(teacherId);
    assertThat(result.getCourseAssignmentId()).isEqualTo(groupCourseId);
  }

  @Test
  void rejects_assignment_when_course_assignment_not_found() {
    when(groupCourseRepository.findById(groupCourseId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.assignTeacher(groupCourseId, teacherId))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Course assignment not found");
  }

  @Test
  void rejects_assignment_when_teacher_not_found() {
    when(groupCourseRepository.findById(groupCourseId)).thenReturn(Optional.of(groupCourse()));
    when(teacherRepository.findById(teacherId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.assignTeacher(groupCourseId, teacherId))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Teacher not found");
  }

  @Test
  void rejects_assignment_when_teacher_already_assigned_to_this_course() {
    when(groupCourseRepository.findById(groupCourseId)).thenReturn(Optional.of(groupCourse()));
    when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher()));
    when(courseTeacherRepository.existsByTeacherIdAndGroupCourseId(teacherId, groupCourseId))
        .thenReturn(true);

    assertThatThrownBy(() -> service.assignTeacher(groupCourseId, teacherId))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("already assigned");
  }
}
