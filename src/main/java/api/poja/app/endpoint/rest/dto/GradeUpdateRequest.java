package api.poja.app.endpoint.rest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record GradeUpdateRequest(
    @NotNull @DecimalMin("0") @DecimalMax("20") BigDecimal value, String reason) {}
