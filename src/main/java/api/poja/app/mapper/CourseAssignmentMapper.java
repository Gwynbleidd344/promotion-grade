package api.poja.app.mapper;

import api.poja.app.entity.AcademicYear;
import api.poja.app.entity.Course;
import api.poja.app.entity.GroupCourse;
import api.poja.app.entity.StudentGroup;
import api.poja.app.model.CourseAssignment;

public final class CourseAssignmentMapper {

  private CourseAssignmentMapper() {}

  public static CourseAssignment toModel(GroupCourse entity) {
    if (entity == null) {
      return null;
    }
    var teachers =
        entity.getCourseTeachers().stream()
            .map(courseTeacher -> TeacherMapper.toModel(courseTeacher.getTeacher()))
            .toList();

    return CourseAssignment.builder()
        .id(entity.getId())
        .groupId(entity.getGroup().getId())
        .courseId(entity.getCourse().getId())
        .academicYearId(entity.getAcademicYear().getId())
        .semester(entity.getSemester())
        .teachers(teachers)
        .build();
  }

  public static GroupCourse toNewEntity(
      CourseAssignment model, StudentGroup group, Course course, AcademicYear academicYear) {
    var entity = new GroupCourse();
    entity.setGroup(group);
    entity.setCourse(course);
    entity.setAcademicYear(academicYear);
    entity.setSemester(model.getSemester());
    return entity;
  }
}
