package api.poja.app.endpoint.rest.dto;

import api.poja.app.entity.Teacher;
import java.util.UUID;

public record TeacherResponse(
    UUID id, String employeeNumber, String firstName, String lastName, UUID userAccountId) {

  public static TeacherResponse from(Teacher teacher) {
    return new TeacherResponse(
        teacher.getId(),
        teacher.getEmployeeNumber(),
        teacher.getFirstName(),
        teacher.getLastName(),
        teacher.getUserAccount().getId());
  }
}
