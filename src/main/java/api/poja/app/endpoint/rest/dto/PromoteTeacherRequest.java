package api.poja.app.endpoint.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record PromoteTeacherRequest(@NotBlank String firstName, @NotBlank String lastName) {}
