package api.poja.app.mapper;

import api.poja.app.entity.GroupCourse;
import api.poja.app.model.Exam;

public final class ExamMapper {

  private ExamMapper() {}

  public static Exam toModel(api.poja.app.entity.Exam entity) {
    if (entity == null) {
      return null;
    }
    return Exam.builder()
        .id(entity.getId())
        .courseAssignmentId(entity.getGroupCourse().getId())
        .name(entity.getName())
        .examDate(entity.getExamDate())
        .examTime(entity.getExamTime())
        .coefficient(entity.getCoefficient())
        .build();
  }

  public static api.poja.app.entity.Exam toNewEntity(Exam model, GroupCourse groupCourse) {
    var entity = new api.poja.app.entity.Exam();
    entity.setGroupCourse(groupCourse);
    entity.setName(model.getName());
    entity.setExamDate(model.getExamDate());
    entity.setExamTime(model.getExamTime());
    entity.setCoefficient(model.getCoefficient());
    return entity;
  }
}
