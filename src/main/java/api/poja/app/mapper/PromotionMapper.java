package api.poja.app.mapper;

import api.poja.app.model.Promotion;

public final class PromotionMapper {

  private PromotionMapper() {}

  public static Promotion toModel(api.poja.app.entity.Promotion entity) {
    if (entity == null) {
      return null;
    }
    return Promotion.builder()
        .id(entity.getId())
        .name(entity.getName())
        .graduationYear(entity.getGraduationYear())
        .build();
  }

  public static api.poja.app.entity.Promotion toNewEntity(Promotion model) {
    var entity = new api.poja.app.entity.Promotion();
    entity.setName(model.getName());
    entity.setGraduationYear(model.getGraduationYear());
    return entity;
  }
}
