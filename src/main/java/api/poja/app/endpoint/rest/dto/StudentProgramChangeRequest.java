package api.poja.app.endpoint.rest.dto;

import api.poja.app.entity.enums.ProgramCode;
import jakarta.validation.constraints.NotNull;

public record StudentProgramChangeRequest(@NotNull ProgramCode program) {}
