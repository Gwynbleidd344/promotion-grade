package api.poja.app.mapper;

import api.poja.app.model.UserAccount;

public final class UserAccountMapper {

  private UserAccountMapper() {}

  public static UserAccount toModel(api.poja.app.entity.UserAccount entity) {
    if (entity == null) {
      return null;
    }
    return UserAccount.builder()
        .id(entity.getId())
        .username(entity.getUsername())
        .email(entity.getEmail())
        .role(entity.getRole())
        .enabled(entity.isEnabled())
        .createdAt(entity.getCreatedAt())
        .build();
  }

}
