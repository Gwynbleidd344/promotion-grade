package api.poja.app.mapper;

import api.poja.app.entity.GraduationList;
import api.poja.app.entity.GraduationListEntry;
import api.poja.app.entity.Promotion;
import api.poja.app.entity.Student;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class GraduationListMapper {

  private GraduationListMapper() {}

  public static GraduationList toNewEntity(Promotion promotion, String s3Key) {
    var entity = new GraduationList();
    entity.setPromotion(promotion);
    entity.setS3Key(s3Key);
    entity.setGeneratedAt(LocalDateTime.now());
    return entity;
  }

  public static GraduationListEntry toEntryEntity(
      GraduationList list, Student student, Integer rank, BigDecimal average) {
    var entity = new GraduationListEntry();
    entity.setGraduationList(list);
    entity.setStudent(student);
    entity.setRank(rank);
    entity.setGeneralAverage(average);
    return entity;
  }

  public static api.poja.app.model.GraduationListEntry toModel(GraduationListEntry entity) {
    if (entity == null) {
      return null;
    }
    var student = entity.getStudent();
    return api.poja.app.model.GraduationListEntry.builder()
        .rank(entity.getRank())
        .studentNumber(student.getStudentNumber())
        .lastName(student.getLastName())
        .firstName(student.getFirstName())
        .generalAverage(entity.getGeneralAverage())
        .build();
  }

  public static List<api.poja.app.model.GraduationListEntry> toModelList(
      List<GraduationListEntry> entities) {
    return entities.stream()
        .sorted((a, b) -> Integer.compare(a.getRank(), b.getRank()))
        .map(GraduationListMapper::toModel)
        .toList();
  }
}
