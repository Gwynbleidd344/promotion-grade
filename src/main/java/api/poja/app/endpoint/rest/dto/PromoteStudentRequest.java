package api.poja.app.endpoint.rest.dto;

import api.poja.app.entity.enums.ProgramCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record PromoteStudentRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotNull ProgramCode program,
    @NotNull UUID promotionId,
    @NotNull UUID groupId,
    @NotNull UUID academicYearId,
    @NotNull Integer semester,
    @NotNull LocalDate startDate) {}
