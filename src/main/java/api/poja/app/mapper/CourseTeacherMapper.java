package api.poja.app.mapper;

import api.poja.app.model.CourseTeacher;

public final class CourseTeacherMapper {

  private CourseTeacherMapper() {}

  public static CourseTeacher toModel(api.poja.app.entity.CourseTeacher entity) {
    if (entity == null) {
      return null;
    }
    return CourseTeacher.builder()
        .id(entity.getId())
        .teacherId(entity.getTeacher().getId())
        .courseAssignmentId(entity.getGroupCourse().getId())
        .build();
  }

  public static api.poja.app.entity.CourseTeacher toNewEntity(
      api.poja.app.entity.Teacher teacher, api.poja.app.entity.GroupCourse groupCourse) {
    var entity = new api.poja.app.entity.CourseTeacher();
    entity.setTeacher(teacher);
    entity.setGroupCourse(groupCourse);
    return entity;
  }
}
