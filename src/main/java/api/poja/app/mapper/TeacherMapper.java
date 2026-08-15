package api.poja.app.mapper;

import api.poja.app.model.Teacher;

public final class TeacherMapper {

  private TeacherMapper() {}

  public static Teacher toModel(api.poja.app.entity.Teacher entity) {
    if (entity == null) {
      return null;
    }
    return Teacher.builder()
        .id(entity.getId())
        .employeeNumber(entity.getEmployeeNumber())
        .firstName(entity.getFirstName())
        .lastName(entity.getLastName())
        .userAccountId(entity.getUserAccount().getId())
        .build();
  }

  public static api.poja.app.entity.Teacher toNewEntity(
      Teacher model, api.poja.app.entity.UserAccount userAccount) {
    var entity = new api.poja.app.entity.Teacher();
    entity.setUserAccount(userAccount);
    entity.setEmployeeNumber(model.getEmployeeNumber());
    entity.setFirstName(model.getFirstName());
    entity.setLastName(model.getLastName());
    return entity;
  }
}
