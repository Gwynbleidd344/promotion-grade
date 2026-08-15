package api.poja.app.endpoint.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record StudentGroupCreateRequest(@NotBlank String reference, String name) {}
