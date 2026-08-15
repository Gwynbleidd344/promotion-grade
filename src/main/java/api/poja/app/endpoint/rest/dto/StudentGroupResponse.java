package api.poja.app.endpoint.rest.dto;

import api.poja.app.entity.StudentGroup;
import java.util.UUID;

public record StudentGroupResponse(UUID id, String reference, String name) {

  public static StudentGroupResponse from(StudentGroup group) {
    return new StudentGroupResponse(group.getId(), group.getReference(), group.getName());
  }
}
