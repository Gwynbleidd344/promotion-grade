package api.poja.app.endpoint.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CourseCreateRequest(
    @NotBlank String reference, @NotBlank String title, @NotNull @Min(1) Integer credits) {}
