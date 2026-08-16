package api.poja.app.endpoint.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record PromoteAdminRequest(@NotBlank String firstName, @NotBlank String lastName) {}
