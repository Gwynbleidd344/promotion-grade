package api.poja.app.endpoint.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PromotionCreateRequest(@NotBlank String name, @NotNull Integer graduationYear) {}
