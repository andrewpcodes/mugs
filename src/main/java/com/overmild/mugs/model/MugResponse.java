package com.overmild.mugs.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "MugResponse", description = "Response payload representing a mug")
public record MugResponse(
        @Schema(example = "f95af6c7-c9c5-44c9-95a4-8520d9f77d84")
        String id,
        @Schema(example = "1d20f53f-2c6d-49d1-8f36-8eaf2d9f9939")
        String ownerUserId,
        @Schema(example = "2025-01-15")
        LocalDate purchaseDate,
        @Schema(example = "2026-01-15")
        LocalDate renewalDate,
        @Schema(example = "18.99")
        BigDecimal cost,
        @Schema(example = "12.0")
        BigDecimal sizeOunces
) {
    public static MugResponse from(Mug mug) {
        return new MugResponse(
                mug.id(),
                mug.ownerUserId(),
                mug.purchaseDate(),
                mug.renewalDate(),
                mug.cost(),
                mug.sizeOunces()
        );
    }
}
