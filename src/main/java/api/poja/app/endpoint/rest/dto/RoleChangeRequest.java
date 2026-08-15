package api.poja.app.endpoint.rest.dto;

import api.poja.app.entity.enums.ProgramCode;
import api.poja.app.entity.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RoleChangeRequest(
    @NotNull UserRole role, StudentProfile studentProfile, TeacherProfile teacherProfile) {

  public record StudentProfile(
      String studentNumber,
      String firstName,
      String lastName,
      ProgramCode program,
      UUID promotionId) {}

  public record TeacherProfile(String employeeNumber, String firstName, String lastName) {}
}
