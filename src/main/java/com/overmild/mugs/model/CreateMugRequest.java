package com.overmild.mugs.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "CreateMugRequest", description = "Request payload to create a mug")
public record CreateMugRequest(
        @Schema(example = "2025-01-15")
        @NotNull(message = "purchaseDate is required")
        LocalDate purchaseDate,
        @Schema(example = "2026-01-15")
        @NotNull(message = "renewalDate is required")
        LocalDate renewalDate,
        @Schema(example = "18.99")
        @NotNull(message = "cost is required")
        @DecimalMin(value = "0.0", message = "cost must be non-negative")
        BigDecimal cost,
        @Schema(example = "12.0")
        @NotNull(message = "sizeOunces is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "sizeOunces must be greater than zero")
        BigDecimal sizeOunces
) {
}
