package api.poja.app.endpoint.rest.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record StudentGroupChangeRequest(
    @NotNull UUID groupId,
    @NotNull UUID academicYearId,
    @NotNull Integer semester,
    @NotNull LocalDate startDate) {}
