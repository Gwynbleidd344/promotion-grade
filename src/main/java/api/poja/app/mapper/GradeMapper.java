package api.poja.app.mapper;

import api.poja.app.entity.Exam;
import api.poja.app.entity.Student;
import api.poja.app.model.Grade;
import api.poja.app.model.GradeHistory;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class GradeMapper {

  private GradeMapper() {}

  public static Grade toModel(api.poja.app.entity.Grade entity) {
    if (entity == null) {
      return null;
    }
    return Grade.builder()
        .id(entity.getId())
        .studentId(entity.getStudent().getId())
        .examId(entity.getExam().getId())
        .value(entity.getValue())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }

  public static api.poja.app.entity.Grade toNewEntity(Student student, Exam exam, BigDecimal value) {
    var entity = new api.poja.app.entity.Grade();
    entity.setStudent(student);
    entity.setExam(exam);
    entity.setValue(value);
    var now = LocalDateTime.now();
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    return entity;
  }

  public static GradeHistory toHistoryModel(api.poja.app.entity.GradeHistory entity) {
    if (entity == null) {
      return null;
    }
    return GradeHistory.builder()
        .id(entity.getId())
        .gradeId(entity.getGrade().getId())
        .oldValue(entity.getOldValue())
        .newValue(entity.getNewValue())
        .changedAt(entity.getChangedAt())
        .changedByUserAccountId(
            entity.getChangedBy() != null ? entity.getChangedBy().getId() : null)
        .reason(entity.getReason())
        .build();
  }
}
