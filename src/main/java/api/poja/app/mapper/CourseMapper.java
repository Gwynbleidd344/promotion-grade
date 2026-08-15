package api.poja.app.mapper;

import api.poja.app.model.Course;

public final class CourseMapper {

  private CourseMapper() {}

  public static Course toModel(api.poja.app.entity.Course entity) {
    if (entity == null) {
      return null;
    }
    return Course.builder()
        .id(entity.getId())
        .reference(entity.getReference())
        .title(entity.getTitle())
        .credits(entity.getCredits())
        .build();
  }

  public static api.poja.app.entity.Course toNewEntity(Course model) {
    var entity = new api.poja.app.entity.Course();
    entity.setReference(model.getReference());
    entity.setTitle(model.getTitle());
    entity.setCredits(model.getCredits());
    return entity;
  }
}
