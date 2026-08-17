package api.poja.app.endpoint.rest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record ExamCreateRequest(
    @NotBlank String name,
    LocalDate examDate,
    LocalTime examTime,
    @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal coefficient) {}
