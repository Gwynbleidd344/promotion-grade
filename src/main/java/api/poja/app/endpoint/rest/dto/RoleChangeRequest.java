package api.poja.app.endpoint.rest.dto;

import api.poja.app.entity.enums.ProgramCode;
import api.poja.app.entity.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RoleChangeRequest(
        @NotNull UserRole role,
        StudentProfile studentProfile,
        TeacherProfile teacherProfile,
        AdminProfile adminProfile) {

  public record StudentProfile(
          String firstName, String lastName, ProgramCode program, UUID promotionId) {}

  public record TeacherProfile(String firstName, String lastName) {}

  public record AdminProfile(String firstName, String lastName) {}
}