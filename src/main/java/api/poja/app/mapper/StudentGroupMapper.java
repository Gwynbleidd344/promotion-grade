package api.poja.app.mapper;

import api.poja.app.model.StudentGroup;

public final class StudentGroupMapper {

  private StudentGroupMapper() {}

  public static StudentGroup toModel(api.poja.app.entity.StudentGroup entity) {
    if (entity == null) {
      return null;
    }
    return StudentGroup.builder()
        .id(entity.getId())
        .reference(entity.getReference())
        .name(entity.getName())
        .build();
  }

  public static api.poja.app.entity.StudentGroup toNewEntity(StudentGroup model) {
    var entity = new api.poja.app.entity.StudentGroup();
    entity.setReference(model.getReference());
    entity.setName(model.getName());
    return entity;
  }
}
