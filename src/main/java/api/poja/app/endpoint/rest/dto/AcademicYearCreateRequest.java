package api.poja.app.endpoint.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AcademicYearCreateRequest(
    @NotBlank String label, @NotNull LocalDate startDate, @NotNull LocalDate endDate) {}
