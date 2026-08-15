package api.poja.app.endpoint.rest.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CourseAssignmentCreateRequest(
    @NotNull UUID groupId,
    @NotNull UUID courseId,
    @NotNull UUID academicYearId,
    @NotNull Integer semester) {}
