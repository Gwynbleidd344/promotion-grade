package api.poja.app.mapper;

import api.poja.app.model.AcademicYear;

public final class AcademicYearMapper {

  private AcademicYearMapper() {}

  public static AcademicYear toModel(api.poja.app.entity.AcademicYear entity) {
    if (entity == null) {
      return null;
    }
    return AcademicYear.builder()
        .id(entity.getId())
        .label(entity.getLabel())
        .startDate(entity.getStartDate())
        .endDate(entity.getEndDate())
        .build();
  }

  public static api.poja.app.entity.AcademicYear toNewEntity(AcademicYear model) {
    var entity = new api.poja.app.entity.AcademicYear();
    entity.setLabel(model.getLabel());
    entity.setStartDate(model.getStartDate());
    entity.setEndDate(model.getEndDate());
    return entity;
  }
}
